package com.example.chapter09.dsh.tools;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 一次工具调用请求：模型说要调哪个工具、带什么参数。
 *
 * 参数刻意用 Map&lt;String, String&gt; 而不是强类型对象——
 * 工具参数来自模型输出，天然是“外部数据”，校验发生在流水线里，
 * 而不是让每个工具自己防御一遍。
 */
public record ToolCall(String tool, Map<String, String> args) {

    public ToolCall(String tool, Map<String, String> args) {
        this.tool = tool;
        this.args = new LinkedHashMap<>(args);
    }

    public static ToolCall of(String tool, String... keyValuePairs) {
        Map<String, String> args = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValuePairs.length; i += 2) {
            args.put(keyValuePairs[i], keyValuePairs[i + 1]);
        }
        return new ToolCall(tool, args);
    }

    @Override
    public String toString() {
        return tool + args;
    }
}
