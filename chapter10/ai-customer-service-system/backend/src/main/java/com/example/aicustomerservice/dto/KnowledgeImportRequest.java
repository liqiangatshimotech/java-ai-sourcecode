package com.example.aicustomerservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record KnowledgeImportRequest(
    @NotBlank(message = "title 不能为空")
    @Size(max = 120, message = "title 长度不能超过 120")
    String title,

    @NotBlank(message = "content 不能为空")
    @Size(max = 8000, message = "content 长度不能超过 8000")
    String content,

    @Size(max = 64, message = "version 长度不能超过 64")
    String version,

    @Size(max = 32, message = "sourceType 长度不能超过 32")
    String sourceType,

    @Size(max = 120, message = "sourceName 长度不能超过 120")
    String sourceName,

    @Size(max = 64, message = "operator 长度不能超过 64")
    String operator
) {
}
