package com.example.aicustomerservice.dto;

import jakarta.validation.constraints.Size;

public record KnowledgeActionRequest(
    @Size(max = 64, message = "operator 长度不能超过 64")
    String operator
) {
}
