package com.flashsale.mq;

import com.flashsale.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 抢票消息生产者：接口线程只负责发消息，削峰由 MQ 承担
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderProducer {

    private final RabbitTemplate rabbitTemplate;

    public void send(SeckillMessage message) {
        log.info("send seckill message: userId={}, tierId={}", message.getUserId(), message.getTierId());
        rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_EXCHANGE, RabbitMQConfig.ORDER_KEY, message);
    }
}
