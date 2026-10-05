package com.example.aiproduction.cost;

/**
 * 一次模型请求的 Token 用量。
 *
 * <p>真实接口一般会返回更精确的 usage 字段。课程示例里先用字符数粗略估算，重点看生产化项目应该如何记录和消费这些数据。
 */
public record TokenUsage(int inputTokens, int outputTokens) {

    public int totalTokens() {
        return inputTokens + outputTokens;
    }
}
