package com.example.aicustomerservice.dto;

import com.example.aicustomerservice.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OrderStatusUpdateRequest(
    @NotNull(message = "status 不能为空")
    OrderStatus status,

    @Size(max = 64, message = "operator 长度不能超过 64")
    String operator
) {
}
