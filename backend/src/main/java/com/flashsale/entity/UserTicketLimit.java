package com.flashsale.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("user_ticket_limit")
public class UserTicketLimit {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long concertId;

    private Integer purchasedCount;
}
