package com.example.aicustomerservice.ai;

public interface ChatModelClient {

    ModelSettings settings();

    /**
     * 校验当前模型是否具备调用条件。
     *
     * 真实大模型缺少 API Key、模型名或 Base URL 时，应该在业务请求里明确报错，
     * 不能退回规则模型或静默使用假回答。
     */
    default void validateConfiguration() {
    }

    /**
     * 生成客服回复。
     *
     * 生产代码必须走真实大模型客户端；测试代码可以注入可控的 fake client。
     */
    String generate(SupportPrompt prompt);

    /** 测试替身可仅实现 generate；正式客户端覆盖此方法返回供应商 usage。 */
    default ModelCompletion generateCompletion(SupportPrompt prompt) {
        return ModelCompletion.unmetered(generate(prompt));
    }
}
