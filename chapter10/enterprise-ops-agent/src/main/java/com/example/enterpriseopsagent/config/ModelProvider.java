package com.example.enterpriseopsagent.config;

/**
 * 当前项目支持的模型供应商。
 *
 * DEEPSEEK 是课程示例里的默认模型。
 * OLLAMA 用于本地模型调试，适合不希望把数据发到外部服务的场景。
 * OPENAI 对应 ChatGPT / OpenAI API，适合需要更强通用能力的场景。
 */
public enum ModelProvider {
    DEEPSEEK,
    OLLAMA,
    OPENAI
}
