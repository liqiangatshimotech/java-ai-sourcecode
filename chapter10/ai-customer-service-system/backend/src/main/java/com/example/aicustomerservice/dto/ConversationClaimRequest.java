package com.example.aicustomerservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 领取会话请求。operator 是当前客服的展示名称或稳定工号。 */
public record ConversationClaimRequest(
    @NotBlank(message = "operator 不能为空")
    @Size(max = 128, message = "operator 最多 128 个字符")
    String operator
) {
}
