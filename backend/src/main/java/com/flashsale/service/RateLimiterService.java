package com.flashsale.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 防刷限流：Redis 固定窗口计数（同账号限频）
 */
@Service
@RequiredArgsConstructor
public class RateLimiterService {

    /**
     * INCR + 首次 EXPIRE，窗口计数原子化
     */
    private static final DefaultRedisScript<Long> RATE_SCRIPT = new DefaultRedisScript<>("""
            local v = redis.call('INCR', KEYS[1])
            if v == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return v
            """, Long.class);

    private final StringRedisTemplate redis;

    @Value("${seckill.rate-limit-count:5}")
    private int limitCount;

    @Value("${seckill.rate-limit-window-seconds:10}")
    private int windowSeconds;

    /**
     * @return true=放行 false=超限
     */
    public boolean tryAcquire(String key) {
        Long count = redis.execute(RATE_SCRIPT,
                List.of("rate:" + key), String.valueOf(windowSeconds));
        return count != null && count <= limitCount;
    }
}
