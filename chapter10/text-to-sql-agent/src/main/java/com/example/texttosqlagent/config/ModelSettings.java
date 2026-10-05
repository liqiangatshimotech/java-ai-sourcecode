package com.example.texttosqlagent.config;

/**
 * 模型连接配置。
 *
 * 用普通 Java record 表达配置含义：供应商、端点、密钥、模型名、温度。
 * 密钥一律从环境变量读取，绝不写进代码仓库——这是企业项目的底线。
 */
public record ModelSettings(
    ModelProvider provider,
    String baseUrl,
    String apiKey,
    String modelName,
    double temperature
) {
    /**
     * 默认使用 DeepSeek。
     * DeepSeek 提供 OpenAI 兼容端点，所以可以直接复用 OpenAI 协议的客户端。
     * 密钥从环境变量 DEEPSEEK_API_KEY 读取；端点和模型名也允许用环境变量覆盖。
     */
    public static ModelSettings deepSeekDefault() {
        String baseUrl = System.getenv().getOrDefault("DEEPSEEK_BASE_URL", "https://api.deepseek.com");
        String modelName = System.getenv().getOrDefault("DEEPSEEK_MODEL", "deepseek-v4-flash");
        return new ModelSettings(
            ModelProvider.DEEPSEEK,
            baseUrl,
            System.getenv("DEEPSEEK_API_KEY"),
            modelName,
            0.2
        );
    }

    /**
     * Ollama 通常运行在本机，适合离线演示和本地开发，不需要 API Key。
     */
    public static ModelSettings ollamaLocal() {
        String baseUrl = System.getenv().getOrDefault("OLLAMA_BASE_URL", "http://localhost:11434/v1");
        String modelName = System.getenv().getOrDefault("OLLAMA_MODEL", "qwen2.5:7b");
        return new ModelSettings(
            ModelProvider.OLLAMA,
            baseUrl,
            "",
            modelName,
            0.2
        );
    }

    /**
     * ChatGPT / OpenAI 配置。
     * 保留这个入口，是为了让同一套 Agent 编排代码可以不改动就切换模型。
     */
    public static ModelSettings openAiDefault() {
        String baseUrl = System.getenv().getOrDefault("OPENAI_BASE_URL", "https://api.openai.com/v1");
        String modelName = System.getenv().getOrDefault("OPENAI_MODEL", "gpt-4.1-mini");
        return new ModelSettings(
            ModelProvider.OPENAI,
            baseUrl,
            System.getenv("OPENAI_API_KEY"),
            modelName,
            0.2
        );
    }

    /**
     * 根据供应商枚举返回默认配置。
     * 业务代码只依赖 ModelSettings，不需要知道每个供应商的默认地址和模型名。
     */
    public static ModelSettings forProvider(ModelProvider provider) {
        return switch (provider) {
            case DEEPSEEK -> deepSeekDefault();
            case OLLAMA -> ollamaLocal();
            case OPENAI -> openAiDefault();
        };
    }

    /**
     * 从配置项或环境变量中解析模型供应商。
     *
     * 支持值：deepseek / ollama / openai / chatgpt，空值默认 DeepSeek。
     * 非法值直接抛异常——配置错误要尽早暴露，不要带病启动。
     */
    public static ModelSettings fromProviderName(String providerName) {
        if (providerName == null || providerName.isBlank()) {
            return deepSeekDefault();
        }

        String normalizedName = providerName.trim().toLowerCase();
        return switch (normalizedName) {
            case "deepseek" -> deepSeekDefault();
            case "ollama" -> ollamaLocal();
            case "openai", "chatgpt" -> openAiDefault();
            default -> throw new IllegalArgumentException("不支持的模型供应商：" + providerName);
        };
    }

    /**
     * 当前配置是否具备调用真实模型的条件。
     * Ollama 是本机服务不需要密钥；DeepSeek / OpenAI 必须配了 API Key。
     */
    public boolean hasCredential() {
        if (provider == ModelProvider.OLLAMA) {
            return true;
        }
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * 日志里只输出供应商、模型名和端点，绝不输出密钥。
     */
    public String safeSummary() {
        return provider + " / " + modelName + " / " + baseUrl;
    }
}
