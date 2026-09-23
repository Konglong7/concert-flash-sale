package com.flashsale.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 演出详情（含票档）
 */
@Data
public class ConcertDetailVO {

    private Long id;

    private String name;

    private String venue;

    private java.time.LocalDateTime showTime;

    private java.time.LocalDateTime saleStartTime;

    private java.time.LocalDateTime saleEndTime;

    /**
     * 实时状态：0未开售 1销售中 2已结束
     */
    private Integer status;

    /**
     * 倒计时秒数
     */
    private Long countdownSeconds;

    private List<TierVO> tiers;

    @Data
    public static class TierVO {

        private Long id;

        private String name;

        private BigDecimal price;

        private Integer totalStock;

        private Integer stock;

        /**
         * 每人限购数量
         */
        private Integer limitPerUser;
    }
}
