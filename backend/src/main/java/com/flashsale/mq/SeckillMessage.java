package com.flashsale.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 抢票消息体（接口线程 → MQ → 下单消费者）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeckillMessage {

    private Long userId;

    private Long concertId;

    private Long tierId;

    private String idCard;
}
