package com.example.aicustomerservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 将会话设为等待客户的请求。reason 只写入内部时间线。 */
public record ConversationWaitCustomerRequest(
    @NotBlank(message = "operator 不能为空") @Size(max = 128) String operator,
    @NotBlank(message = "reason 不能为空") @Size(max = 1000) String reason
) {
}
