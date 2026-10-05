package com.example.langgraph4j.aftersales.web;

import com.example.langgraph4j.aftersales.domain.ApprovalDecision;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 在 human_approval 断点写入人工结论。 */
public record ApprovalRequest(
        @NotNull(message = "decision 不能为空") ApprovalDecision decision,
        @NotBlank(message = "approver 不能为空") String approver) {
}
