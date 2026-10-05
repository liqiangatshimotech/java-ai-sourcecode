package com.example.aicustomerservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 解决会话请求。resolution 将用于后续质检和运营统计。 */
public record ConversationResolveRequest(
    @NotBlank(message = "operator 不能为空") @Size(max = 128) String operator,
    @NotBlank(message = "resolution 不能为空") @Size(max = 2000) String resolution
) {
}
