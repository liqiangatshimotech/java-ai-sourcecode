package com.example.aicustomerservice.ai;

/** 模型返回的真实计量；供应商未返回 usage 时用 null，不能用字符数冒充 Token。 */
public record ModelCompletion(String answer, Long inputTokens, Long outputTokens, String actualModel) {
    public static ModelCompletion unmetered(String answer) {
        return new ModelCompletion(answer, null, null, null);
    }
}
