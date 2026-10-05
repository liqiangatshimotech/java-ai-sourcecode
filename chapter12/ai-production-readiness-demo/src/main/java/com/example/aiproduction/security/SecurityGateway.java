package com.example.aiproduction.security;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 请求进入模型前的安全网关。
 *
 * <p>它做三件事：先脱敏，再检测注入，再判断工具是否需要审批。生产环境可以把这个类放在 Controller 或 Agent Runtime 前面。
 */
public class SecurityGateway {

    private final SensitiveDataMasker masker;
    private final PromptInjectionDetector injectionDetector;
    private final ToolRiskPolicy toolRiskPolicy;

    public SecurityGateway(
            SensitiveDataMasker masker,
            PromptInjectionDetector injectionDetector,
            ToolRiskPolicy toolRiskPolicy) {
        this.masker = masker;
        this.injectionDetector = injectionDetector;
        this.toolRiskPolicy = toolRiskPolicy;
    }

    public SecurityReview review(String prompt, String requestedTool) {
        List<String> reasons = new ArrayList<>();
        String maskedPrompt = masker.mask(prompt);

        if (injectionDetector.looksSuspicious(prompt)) {
            reasons.add("检测到疑似 Prompt Injection");
        }
        if (toolRiskPolicy.requiresApproval(requestedTool)) {
            reasons.add("工具 %s 属于高风险写操作，需要人工审批".formatted(requestedTool));
        }

        RiskLevel riskLevel = reasons.isEmpty() ? RiskLevel.LOW : RiskLevel.HIGH;
        return new SecurityReview(!reasons.isEmpty(), riskLevel, reasons, maskedPrompt);
    }
}
