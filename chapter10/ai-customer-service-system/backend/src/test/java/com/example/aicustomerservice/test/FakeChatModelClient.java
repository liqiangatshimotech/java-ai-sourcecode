package com.example.aicustomerservice.test;

import com.example.aicustomerservice.ai.AiProvider;
import com.example.aicustomerservice.ai.ChatModelClient;
import com.example.aicustomerservice.ai.ModelSettings;
import com.example.aicustomerservice.ai.SupportPrompt;
import com.example.aicustomerservice.entity.RetrievedKnowledge;
import com.example.aicustomerservice.entity.ToolTrace;
import java.util.stream.Collectors;

public class FakeChatModelClient implements ChatModelClient {

    private final ModelSettings settings = new ModelSettings(
        AiProvider.OLLAMA,
        "http://localhost:11434/v1",
        "test",
        "test-fake-model",
        0.0
    );

    @Override
    public ModelSettings settings() {
        return settings;
    }

    @Override
    public String generate(SupportPrompt prompt) {
        String evidence = prompt.knowledge().stream()
            .findFirst()
            .map(RetrievedKnowledge::text)
            .orElse("没有命中知识。");
        String toolSummary = prompt.toolTraces().stream()
            .map(ToolTrace::summary)
            .collect(Collectors.joining("；"));
        if (!toolSummary.isBlank()) {
            return "测试模型回复：" + toolSummary + "。" + evidence;
        }
        return "测试模型回复：" + evidence;
    }
}
