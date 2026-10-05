package com.example.chapter09.mimocode.context;

/**
 * 模型窗口预算。MiMo-Code 的源码里会从 provider/model limit 中拿 context、
 * input 和 max output，再减去输出预留和 compaction buffer。这个类只保留
 * 计算思路：上下文窗口再大，也要先扣掉模型输出和恢复流程需要的安全空间。
 */
public record ModelWindow(
        int contextTokens,
        int maxOutputTokens,
        int compactionBufferTokens
) {
    private static final int OUTPUT_CAP = 20_000;

    public int usableInputTokens() {
        int outputReserve = Math.min(maxOutputTokens, OUTPUT_CAP);
        int reserved = Math.min(compactionBufferTokens, maxOutputTokens);
        return Math.max(0, contextTokens - outputReserve - reserved);
    }
}
