package com.example.enterpriseopsagent.web;

import com.example.enterpriseopsagent.domain.DecisionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record IncidentDecisionRequest(
    @NotNull(message = "decision 不能为空")
    DecisionType decision,

    @NotBlank(message = "operator 不能为空")
    String operator,

    String comment
) {
}
