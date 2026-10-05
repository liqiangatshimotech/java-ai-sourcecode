package com.example.aicustomerservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 团队内部备注请求。该内容不会对客户可见。 */
public record ConversationNoteRequest(
    @NotBlank(message = "operator 不能为空") @Size(max = 128) String operator,
    @NotBlank(message = "content 不能为空") @Size(max = 5000) String content
) {
}
