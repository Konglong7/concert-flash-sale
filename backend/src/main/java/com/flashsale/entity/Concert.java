package com.flashsale.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("concert")
public class Concert {

    /** 状态：未开售 */
    public static final int STATUS_NOT_ON_SALE = 0;
    /** 状态：销售中 */
    public static final int STATUS_ON_SALE = 1;
    /** 状态：已结束 */
    public static final int STATUS_ENDED = 2;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String venue;

    private LocalDateTime showTime;

    private LocalDateTime saleStartTime;

    private LocalDateTime saleEndTime;

    private Integer status;

    private LocalDateTime createdAt;
}
