package com.flashsale.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SeckillRequest {

    @NotNull(message = "演出ID不能为空")
    private Long concertId;

    @NotNull(message = "票档ID不能为空")
    private Long tierId;

    @NotBlank(message = "观演人身份证不能为空")
    private String idCard;
}
