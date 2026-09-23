package com.flashsale.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flashsale.common.BusinessException;
import com.flashsale.entity.Order;
import com.flashsale.entity.TicketTier;
import com.flashsale.entity.UserTicketLimit;
import com.flashsale.mapper.OrderMapper;
import com.flashsale.mapper.TicketTierMapper;
import com.flashsale.mapper.UserTicketLimitMapper;
import com.flashsale.mq.SeckillMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 订单服务（消费者侧 DB 操作 + 支付 + 查询）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final TicketTierMapper ticketTierMapper;
    private final UserTicketLimitMapper userTicketLimitMapper;
    private final StockService stockService;

    @Value("${seckill.pay-expire-minutes:5}")
    private double payExpireMinutes;

    /**
     * 下单：DB 原子扣库存 + 限购校验 + 建订单（同一事务）
     *
     * @throws IllegalStateException 库存不足或超限购，事务回滚
     */
    @Transactional(rollbackFor = Exception.class)
    public Order createOrder(SeckillMessage msg) {
        // 1. DB 原子扣库存（WHERE stock > 0，DB 层防超卖双保险）
        int rows = ticketTierMapper.decreaseStock(msg.getTierId());
        if (rows == 0) {
            throw new IllegalStateException("库存不足，订单创建失败");
        }

        TicketTier tier = ticketTierMapper.selectById(msg.getTierId());

        // 2. 限购校验：已购数量 >= 每人限购数则拒绝
        Long purchased = userTicketLimitMapper.selectCount(new LambdaQueryWrapper<UserTicketLimit>()
                .eq(UserTicketLimit::getUserId, msg.getUserId())
                .eq(UserTicketLimit::getConcertId, msg.getConcertId())
                .apply("purchased_count >= {0}", tier.getLimitPerUser()));
        if (purchased != null && purchased > 0) {
            throw new IllegalStateException("超过限购数量");
        }

        // 3. 创建订单
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(msg.getUserId());
        order.setConcertId(msg.getConcertId());
        order.setTierId(msg.getTierId());
        order.setIdCard(msg.getIdCard());
        order.setPrice(tier.getPrice());
        order.setStatus(Order.STATUS_UNPAID);
        order.setCreatedAt(LocalDateTime.now());
        order.setExpireAt(LocalDateTime.now().plusSeconds((long) (payExpireMinutes * 60)));
        orderMapper.insert(order);

        // 4. 限购计数 +1（upsert）
        userTicketLimitMapper.increasePurchased(msg.getUserId(), msg.getConcertId());

        log.info("order created: orderNo={}, userId={}, tierId={}",
                order.getOrderNo(), msg.getUserId(), msg.getTierId());
        return order;
    }

    /**
     * 我的订单列表
     */
    public List<Order> listByUser(Long userId) {
        return orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .orderByDesc(Order::getCreatedAt));
    }

    /**
     * 支付：仅待支付且未过期订单可支付
     */
    @Transactional(rollbackFor = Exception.class)
    public Order pay(Long userId, String orderNo) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo)
                .eq(Order::getUserId, userId));
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        if (order.getStatus() != Order.STATUS_UNPAID) {
            throw new BusinessException(400, "订单状态不可支付");
        }
        if (order.getExpireAt().isBefore(LocalDateTime.now())) {
            // 过期：转为超时并释放库存（超时消费者可能还没跑，这里兜底）
            if (orderMapper.markTimeout(orderNo) > 0) {
                compensateStock(order);
            }
            throw new BusinessException(400, "订单已超时，库存已释放");
        }
        if (orderMapper.markPaid(orderNo, userId) == 0) {
            throw new BusinessException(400, "订单状态已变更");
        }
        order.setStatus(Order.STATUS_PAID);
        order.setPaidAt(LocalDateTime.now());
        log.info("order paid: orderNo={}", orderNo);
        return order;
    }

    /**
     * 超时释放：订单转超时 + 回补 DB/Redis 库存 + 限购计数回退
     * （由 order.timeout.queue 的消费者调用）
     */
    @Transactional(rollbackFor = Exception.class)
    public void releaseTimeoutOrder(String orderNo) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo));
        if (order == null || order.getStatus() != Order.STATUS_UNPAID) {
            return; // 已支付/已取消，忽略
        }
        if (order.getExpireAt().isAfter(LocalDateTime.now())) {
            return; // 尚未到期（安全兜底）
        }
        if (orderMapper.markTimeout(orderNo) == 0) {
            return; // 并发下已被处理
        }
        compensateStock(order);
        log.info("order timeout released: orderNo={}", orderNo);
    }

    /**
     * 库存补偿：DB 回补 + Redis 回补 + 去重标记移除 + 限购计数回退
     */
    private void compensateStock(Order order) {
        ticketTierMapper.increaseStock(order.getTierId());
        stockService.restoreStock(order.getTierId());
        stockService.removeBought(order.getTierId(), order.getUserId());
        userTicketLimitMapper.decreasePurchased(order.getUserId(), order.getConcertId());
    }

    private String generateOrderNo() {
        return System.currentTimeMillis() + String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
    }
}
