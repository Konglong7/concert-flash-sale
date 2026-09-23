package com.flashsale.service;

import com.flashsale.common.BusinessException;
import com.flashsale.dto.SeckillRequest;
import com.flashsale.entity.Concert;
import com.flashsale.entity.TicketTier;
import com.flashsale.mapper.ConcertMapper;
import com.flashsale.mapper.TicketTierMapper;
import com.flashsale.mq.OrderProducer;
import com.flashsale.mq.SeckillMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 抢票入口：轻量校验 + Redis 原子扣减 + 发 MQ，全部操作不碰 DB，
 * 单次请求耗时约 1-2ms，支撑高并发
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillService {

    private final ConcertMapper concertMapper;
    private final TicketTierMapper ticketTierMapper;
    private final StockService stockService;
    private final OrderProducer orderProducer;
    private final RateLimiterService rateLimiterService;

    public void seckill(Long userId, SeckillRequest request) {
        Long tierId = request.getTierId();

        // 0. 防刷限流：同账号窗口期内限频（Redis 固定窗口计数）
        if (!rateLimiterService.tryAcquire("seckill:user:" + userId)) {
            throw new BusinessException(429, "操作过于频繁，请稍后再试");
        }

        // 1. 票档存在性校验
        TicketTier tier = ticketTierMapper.selectById(tierId);
        if (tier == null || !tier.getConcertId().equals(request.getConcertId())) {
            throw new BusinessException(400, "票档不存在");
        }

        // 2. 演出必须在售
        Concert concert = concertMapper.selectById(request.getConcertId());
        if (concert == null) {
            throw new BusinessException(400, "演出不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(concert.getSaleStartTime()) || now.isAfter(concert.getSaleEndTime())) {
            throw new BusinessException(400, "当前不在售票时间内");
        }

        // 3. 一人一票档仅可抢一次（Redis Set 去重，微秒级）
        if (!stockService.markBought(tierId, userId)) {
            throw new BusinessException(400, "您已抢购过该票档，请勿重复抢票");
        }

        // 4. Redis Lua 原子扣库存（防超卖核心）
        if (!stockService.decreaseStock(tierId)) {
            stockService.removeBought(tierId, userId);
            throw new BusinessException(400, "该票档已售罄");
        }

        // 5. 标记处理中 + 发 MQ 异步下单
        try {
            stockService.setResultProcessing(userId, tierId);
            orderProducer.send(new SeckillMessage(userId, request.getConcertId(), tierId, request.getIdCard()));
        } catch (Exception e) {
            // 发送失败则回补，保证用户可重试
            log.error("send mq failed, compensate. userId={}, tierId={}", userId, tierId, e);
            stockService.restoreStock(tierId);
            stockService.removeBought(tierId, userId);
            throw new BusinessException(500, "系统繁忙，请重试");
        }
    }

    /**
     * 轮询抢票结果（结果由下单消费者写入 Redis）
     */
    public String getResult(Long userId, Long tierId) {
        return stockService.getResult(userId, tierId);
    }
}
