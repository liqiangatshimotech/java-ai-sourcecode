package com.example.aiproduction.cost;

/**
 * 模型画像。
 *
 * <p>生产项目里，模型选择不应该只看“哪个最强”。这里把成本、上下文窗口和适合任务写成结构化对象，方便路由器做判断。
 */
public record ModelProfile(
        String modelName,
        int contextWindow,
        double inputPricePerMillionTokens,
        double outputPricePerMillionTokens,
        String bestFor) {

    public static ModelProfile smallDeepSeek() {
        return new ModelProfile("deepseek-v4-flash", 64_000, 0.27, 1.10, "分类、摘要、普通问答");
    }

    public static ModelProfile strongDeepSeek() {
        return new ModelProfile("deepseek-v4-pro", 64_000, 0.55, 2.19, "复杂推理、代码审查、疑难排障");
    }

    public static ModelProfile localOllama() {
        return new ModelProfile("ollama-local", 32_000, 0.0, 0.0, "本地开发、无密钥验证、低风险离线任务");
    }
}
