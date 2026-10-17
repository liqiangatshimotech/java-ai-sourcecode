package com.example.structuredoutput.model;

/** 只负责返回模型原始文本；解析、校验和重试由分类服务统一处理。 */
@FunctionalInterface
public interface ModelClient {

    String generate(String prompt);
}
