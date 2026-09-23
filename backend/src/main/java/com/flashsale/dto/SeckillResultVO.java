package com.flashsale.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 抢票结果轮询响应
 */
@Data
@AllArgsConstructor
public class SeckillResultVO {

    /**
     * processing - 排队处理中
     * success    - 抢票成功
     * fail       - 抢票失败
     * none       - 无记录（未抢或已过期）
     */
    private String status;

    /**
     * 成功时返回订单号
     */
    private String orderNo;

    /**
     * 失败原因
     */
    private String message;
}
