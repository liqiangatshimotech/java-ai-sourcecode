package com.example.enterpriseopsagent.web;

import jakarta.validation.constraints.NotBlank;

public record IncidentResolveRequest(
    @NotBlank(message = "operator 不能为空")
    String operator,

    @NotBlank(message = "summary 不能为空")
    String summary
) {
}
