package com.flashsale.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ticket_tier")
public class TicketTier {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long concertId;

    private String name;

    private BigDecimal price;

    private Integer totalStock;

    private Integer stock;

    private Integer limitPerUser;

    private LocalDateTime createdAt;
}
