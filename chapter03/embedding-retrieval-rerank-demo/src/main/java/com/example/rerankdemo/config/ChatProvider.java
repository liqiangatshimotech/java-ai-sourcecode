package com.example.rerankdemo.config;

import java.util.Locale;

/**
 * 课程统一保留的 Chat provider。
 *
 * <p>本章 demo 只演示召回和重排，不真实调用 Chat 模型。
 * 后面接完整 RAG 时，回答模型仍然默认 DeepSeek，并支持 Ollama 和 ChatGPT。
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
            case "openai", "chatgpt" -> CHATGPT;
            default -> throw new IllegalArgumentException("不支持的 Chat provider：" + rawValue);
        };
    }
}
