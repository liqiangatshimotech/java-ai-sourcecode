package com.example.aicustomerservice.dto;

import jakarta.validation.constraints.NotBlank;

public record AddKnowledgeRequest(
    @NotBlank(message = "title 不能为空")
    String title,

    @NotBlank(message = "content 不能为空")
    String content,

    String version
) {
}
