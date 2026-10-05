package com.example.texttosqlagent.config;

/**
 * 模型供应商枚举。
 *
 * 课程默认用 DeepSeek 举例；Ollama 适合本机离线跑真实模型；
 * OPENAI 同时覆盖 ChatGPT 官方接口和一切 OpenAI 兼容端点。
 */
public enum ModelProvider {
    DEEPSEEK,
    OLLAMA,
    OPENAI
}
