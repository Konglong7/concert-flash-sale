package com.flashsale.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单状态：0待支付 1已支付 2已取消 3超时
 */
@Data
@TableName("orders")
public class Order {

    public static final int STATUS_UNPAID = 0;
    public static final int STATUS_PAID = 1;
    public static final int STATUS_CANCELED = 2;
    public static final int STATUS_TIMEOUT = 3;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long userId;

    private Long concertId;

    private Long tierId;

    private String idCard;

    private BigDecimal price;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime paidAt;

    private LocalDateTime expireAt;
}
