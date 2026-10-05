package com.example.codingagent.web;

import jakarta.validation.constraints.NotBlank;

public record CreateCodingTaskRequest(
        String repositoryName,

        String branchName,

        @NotBlank(message = "description 不能为空")
        String description,

        @NotBlank(message = "operator 不能为空")
        String operator
) {
}
