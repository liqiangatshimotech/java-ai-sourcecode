package com.example.chapter09.dsh.llm;

import com.example.chapter09.dsh.agent.Message;
import com.example.chapter09.dsh.tools.ToolCall;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 回放提供方：对应 dsh 生态里的 llm-replay 适配器。
 *
 * 测试 Agent Runtime 时最忌讳依赖真实模型：慢、贵、不确定。
 * dsh 自己就内置了 replay 适配器，把一次“模型会怎么回”写成剧本，
 * runtime 的所有事件、工具流水线、审批逻辑都能离线跑通。
 * 这个 demo 用同样的思路，保证 mvn exec:java 不需要任何 API Key。
 */
public final class ReplayLlmProvider implements LlmSeam {

    private final List<LlmSeam.LlmReply> script;
    private final AtomicInteger cursor = new AtomicInteger();

    public ReplayLlmProvider(List<LlmSeam.LlmReply> script) {
        this.script = List.copyOf(script);
    }

    @Override
    public LlmSeam.LlmReply complete(String systemPrompt, List<Message> messages) {
        int index = cursor.getAndIncrement();
        if (index >= script.size()) {
            return LlmSeam.LlmReply.text("(replay script exhausted)");
        }
        return script.get(index);
    }

    @Override
    public String providerName() {
        return "llm-replay";
    }
}
