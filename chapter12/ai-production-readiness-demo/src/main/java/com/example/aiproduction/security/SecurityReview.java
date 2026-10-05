package com.example.aiproduction.security;

import java.util.List;

/**
 * 安全检查结果。
 *
 * <p>blocked 表示请求是否应该被拦截，reasons 用来解释拦截原因，maskedPrompt 是脱敏后的提示词，便于安全地进入日志。
 */
public record SecurityReview(boolean blocked, RiskLevel riskLevel, List<String> reasons, String maskedPrompt) {}
