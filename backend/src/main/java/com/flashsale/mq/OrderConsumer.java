package com.flashsale.mq;

import com.flashsale.config.RabbitMQConfig;
import com.flashsale.entity.Order;
import com.flashsale.service.OrderService;
import com.flashsale.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 下单消费者：消费抢票消息落库；任何失败都会补偿 Redis 并告知用户失败原因
 * 成功后投递延时消息（TTL = 支付有效期），到期经死信队列触发超时释放检查
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderConsumer {

    private final OrderService orderService;
    private final StockService stockService;
    private final RabbitTemplate rabbitTemplate;

    @Value("${seckill.pay-expire-minutes:5}")
    private double payExpireMinutes;

    @RabbitListener(queues = RabbitMQConfig.ORDER_QUEUE)
    public void onMessage(SeckillMessage msg) {
        Long userId = msg.getUserId();
        Long tierId = msg.getTierId();
        try {
            Order order = orderService.createOrder(msg);
            stockService.setResultSuccess(userId, tierId, order.getOrderNo());
            sendTimeoutCheck(order.getOrderNo());
        } catch (Exception e) {
            log.error("create order failed, compensate redis. userId={}, tierId={}", userId, tierId, e);
            // 补偿：回补 Redis 库存 + 移除去重标记 + 写入失败结果（DB 侧由事务自动回滚）
            stockService.restoreStock(tierId);
            stockService.removeBought(tierId, userId);
            String reason = e instanceof IllegalStateException ? e.getMessage() : "下单失败，请重试";
            stockService.setResultFail(userId, tierId, reason);
        }
    }

    /**
     * 投递超时检查消息：TTL 到期后经死信路由到 order.timeout.queue
     */
    private void sendTimeoutCheck(String orderNo) {
        long ttlMs = (long) (payExpireMinutes * 60_000);
        rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_EXCHANGE, RabbitMQConfig.ORDER_DELAY_KEY,
                orderNo, m -> {
                    m.getMessageProperties().setExpiration(String.valueOf(ttlMs));
                    return m;
                });
    }
}
