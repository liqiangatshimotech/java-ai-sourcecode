package com.example.chapter09.dsh.llm;

import com.example.chapter09.dsh.agent.Message;
import com.example.chapter09.dsh.tools.ToolCall;

import java.util.List;

/**
 * LLM 接缝：对应 dsh 的 llm/llm 包（ctx.llm）。
 *
 * dsh 把“消息与流式词汇表 + 适配器接缝”单独抽成一个很薄的包，
 * agent loop 只消费这个接口，不关心背后是 DeepSeek、Ollama、ChatGPT
 * 还是回放器。这正是 seam 设计：Service Definition 声明接口，
 * Provider 可并列替换，Consumer 只依赖接口。
 */
public interface LlmSeam {

    /** 一次模型请求：输入系统提示 + 投影出的消息历史，返回文本和可选工具调用。 */
    LlmReply complete(String systemPrompt, List<Message> messages);

    /** 提供方名字，用来在日志里区分是谁在回答。 */
    String providerName();

    /**
     * 模型回复：一段文本，外加零到多个工具调用。
     * 真实适配器从模型 JSON 里解出来；回放器直接从剧本里吐。
     */
    record LlmReply(String text, List<ToolCall> toolCalls) {
        public static LlmReply text(String text) {
            return new LlmReply(text, List.of());
        }

        public static LlmReply toolCalls(List<ToolCall> calls) {
            return new LlmReply("", calls);
        }
    }
}
