package com.example.a2a.protocol;

import java.util.Map;

/**
 * A2A Message 或 Artifact 中的一段内容。
 *
 * <p>规范里 Part 可以是 text、file URL、raw bytes 或 structured data。示例先实现 text 和 data，
 * 已经足够覆盖普通消息和结构化输出。
 */
public record Part(
        String text,
        String url,
        String raw,
        Object data,
        Map<String, Object> metadata) {

    public static Part text(String text) {
        return new Part(text, null, null, null, null);
    }

    public static Part data(Object data) {
        return new Part(null, null, null, data, null);
    }
}
