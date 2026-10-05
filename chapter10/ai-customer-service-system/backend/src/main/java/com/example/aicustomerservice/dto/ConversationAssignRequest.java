package com.example.aicustomerservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 主管转接请求。operator 可省略，省略时审计操作人记录为 system-supervisor。 */
public record ConversationAssignRequest(
    @NotBlank(message = "owner 不能为空")
    @Size(max = 128, message = "owner 最多 128 个字符")
    String owner,
    @Size(max = 128, message = "operator 最多 128 个字符")
    String operator
) {
}
