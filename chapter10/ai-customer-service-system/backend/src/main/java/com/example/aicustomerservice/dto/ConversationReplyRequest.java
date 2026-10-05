package com.example.aicustomerservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 客户可见回复请求。content 会进入客户消息时间线。 */
public record ConversationReplyRequest(
    @NotBlank(message = "operator 不能为空") @Size(max = 128) String operator,
    @NotBlank(message = "content 不能为空") @Size(max = 5000) String content
) {
}
