package com.flashsale.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 抢票 Redis 操作：库存预载、Lua 原子扣减、去重标记、结果存取
 *
 * Key 设计：
 *   seckill:stock:{tierId}        -> 库存余量
 *   seckill:bought:{tierId}       -> Set，已抢到的 userId（防重复）
 *   seckill:result:{userId}:{tierId} -> PROCESSING / SUCCESS:{orderNo} / FAIL:{reason}
 */
@Service
@RequiredArgsConstructor
public class StockService {

    public static final String STOCK_KEY = "seckill:stock:";
    public static final String BOUGHT_KEY = "seckill:bought:";
    public static final String RESULT_KEY = "seckill:result:";

    /**
     * 原子抢票脚本：GET 库存 -> 判断 >0 -> DECR
     * 判断与扣减在同一脚本内执行，天然防超卖
     * 返回：>=0 扣减后的库存；-1 库存不足
     */
    public static final DefaultRedisScript<Long> SECKILL_SCRIPT = new DefaultRedisScript<>("""
            local stock = tonumber(redis.call('GET', KEYS[1]) or '-1')
            if stock > 0 then
                return redis.call('DECR', KEYS[1])
            else
                return -1
            end
            """, Long.class);

    private final StringRedisTemplate redis;

    /**
     * 将 DB 库存预载入 Redis（应用启动时调用）
     */
    public void preloadStock(Long tierId, Integer stock) {
        redis.opsForValue().set(STOCK_KEY + tierId, String.valueOf(stock));
    }

    /**
     * Lua 原子扣减，返回 true 表示抢票成功
     */
    public boolean decreaseStock(Long tierId) {
        Long result = redis.execute(SECKILL_SCRIPT, java.util.List.of(STOCK_KEY + tierId));
        return result != null && result >= 0;
    }

    /**
     * 扣减失败后的补偿：回补库存
     */
    public void restoreStock(Long tierId) {
        redis.opsForValue().increment(STOCK_KEY + tierId);
    }

    /**
     * 一人一票档只能抢一次；返回 false 表示重复抢票
     */
    public boolean markBought(Long tierId, Long userId) {
        Long added = redis.opsForSet().add(BOUGHT_KEY + tierId, String.valueOf(userId));
        return added != null && added > 0;
    }

    /**
     * 抢票失败时移除去重标记，允许用户再次尝试
     */
    public void removeBought(Long tierId, Long userId) {
        redis.opsForSet().remove(BOUGHT_KEY + tierId, String.valueOf(userId));
    }

    public void setResultProcessing(Long userId, Long tierId) {
        redis.opsForValue().set(resultKey(userId, tierId), "PROCESSING", Duration.ofSeconds(60));
    }

    public void setResultSuccess(Long userId, Long tierId, String orderNo) {
        redis.opsForValue().set(resultKey(userId, tierId), "SUCCESS:" + orderNo, Duration.ofHours(1));
    }

    public void setResultFail(Long userId, Long tierId, String reason) {
        redis.opsForValue().set(resultKey(userId, tierId), "FAIL:" + reason, Duration.ofMinutes(30));
    }

    public String getResult(Long userId, Long tierId) {
        return redis.opsForValue().get(resultKey(userId, tierId));
    }

    private String resultKey(Long userId, Long tierId) {
        return RESULT_KEY + userId + ":" + tierId;
    }
}
