package com.example.aicustomerservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerSupportRequest(
    @NotBlank(message = "sessionId 不能为空")
    @Size(max = 128, message = "sessionId 长度不能超过 128")
    String sessionId,

    @NotBlank(message = "customerId 不能为空")
    @Size(max = 64, message = "customerId 长度不能超过 64")
    String customerId,

    @NotBlank(message = "question 不能为空")
    @Size(max = 500, message = "question 长度不能超过 500")
    String question,

    @Size(max = 64, message = "orderNo 长度不能超过 64")
    String orderNo
) {
}
