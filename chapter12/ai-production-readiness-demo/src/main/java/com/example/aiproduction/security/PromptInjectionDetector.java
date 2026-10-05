package com.example.aiproduction.security;

import java.util.Locale;

/**
 * Prompt Injection 简单检测器。
 *
 * <p>生产环境不能只靠关键词规则，但关键词规则适合作为第一层保护。命中后可以阻断，也可以转人工复核。
 */
public class PromptInjectionDetector {

    public boolean looksSuspicious(String prompt) {
        String value = prompt.toLowerCase(Locale.ROOT);
        return value.contains("ignore previous instructions")
                || value.contains("忽略之前的指令")
                || value.contains("system prompt")
                || value.contains("泄露提示词")
                || value.contains("delete all data")
                || value.contains("删除所有数据");
    }
}
