package com.flashsale.mq;

import com.flashsale.config.RabbitMQConfig;
import com.flashsale.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 超时消费者：延时队列 TTL 到期后消息经死信路由至此，检查订单：
 * 仍未支付且已到期 -> 订单转超时 + 释放库存
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutConsumer {

    private final OrderService orderService;

    @RabbitListener(queues = RabbitMQConfig.ORDER_TIMEOUT_QUEUE)
    public void onTimeout(String orderNo) {
        log.info("timeout check for order: {}", orderNo);
        orderService.releaseTimeoutOrder(orderNo);
    }
}
