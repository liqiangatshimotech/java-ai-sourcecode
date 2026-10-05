package com.example.embeddingdemo.config;

import java.util.Locale;

/**
 * 课程统一保留的 Chat provider 枚举。
 *
 * <p>Embedding 负责“找资料”，Chat 模型负责“组织答案”。
 * 本 demo 只跑检索，但依然保留 DeepSeek / Ollama / ChatGPT 配置入口，方便后面接到完整 RAG。
 */
public enum ChatProvider {
    DEEPSEEK,
    OLLAMA,
    CHATGPT;

    public static ChatProvider from(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return DEEPSEEK;
        }

        return switch (rawValue.trim().toLowerCase(Locale.ROOT)) {
            case "deepseek" -> DEEPSEEK;
            case "ollama" -> OLLAMA;
            case "chatgpt", "openai" -> CHATGPT;
            default -> throw new IllegalArgumentException("不支持的 Chat provider：" + rawValue);
        };
    }
}
