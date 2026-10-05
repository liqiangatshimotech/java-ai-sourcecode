package com.example.codingagent.web;

import jakarta.validation.constraints.NotBlank;

public record ApproveCodingTaskRequest(
        @NotBlank(message = "proposalId 不能为空") String proposalId,
        @NotBlank(message = "sourceVersion 不能为空") String sourceVersion,
        @NotBlank(message = "approver 不能为空")
        String approver,

        String comment
) {
}
