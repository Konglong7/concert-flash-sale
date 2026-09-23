package com.flashsale.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 演出列表项 / 详情基础信息
 */
@Data
public class ConcertVO {

    private Long id;

    private String name;

    private String venue;

    private LocalDateTime showTime;

    private LocalDateTime saleStartTime;

    private LocalDateTime saleEndTime;

    /**
     * 实时状态：0未开售 1销售中 2已结束（按当前时间计算）
     */
    private Integer status;

    /**
     * 倒计时秒数：
     * 未开售 → 距开售秒数；销售中 → 距停售秒数；已结束 → 0
     */
    private Long countdownSeconds;
}
