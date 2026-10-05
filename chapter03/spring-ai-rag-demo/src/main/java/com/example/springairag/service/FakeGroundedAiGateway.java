package com.example.springairag.service;

import com.example.springairag.rag.DocumentChunk;
import com.example.springairag.rag.RagPrompt;
import com.example.springairag.rag.RetrievedChunk;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(name = "app.rag.ai-mode", havingValue = "fake", matchIfMissing = true)
public class FakeGroundedAiGateway implements AiGateway {

    @Override
    public String answer(RagPrompt prompt) {
        if (prompt.retrievedChunks().isEmpty()) {
            return """
                知识库中没有找到足够依据，不能直接回答这个问题。
                建议先补充相关制度文档、FAQ 或业务处理流程后再提问。
                """;
        }

        RetrievedChunk best = prompt.retrievedChunks().getFirst();
        String supportingFacts = prompt.retrievedChunks().stream()
            .limit(2)
            .map(RetrievedChunk::chunk)
            .map(this::shortFact)
            .collect(Collectors.joining("\n"));

        String sources = prompt.retrievedChunks().stream()
            .map(RetrievedChunk::chunk)
            .map(chunk -> "- 来源：" + chunk.title() + " - " + chunk.source())
            .distinct()
            .collect(Collectors.joining("\n"));

        return """
            【Fake 模型演示回答】
            根据知识库中最相关的资料《%s》，可以按下面方式处理：
            %s

            这段回答由 FakeGroundedAiGateway 生成，用于无模型环境下稳定演示 RAG 链路。切换 RAG_AI_MODE=spring-ai 后，会改用 Spring AI ChatClient 调用真实模型。

            %s
            """.formatted(best.chunk().title(), supportingFacts, sources);
    }

    private String shortFact(DocumentChunk chunk) {
        String content = chunk.content().replaceAll("\\s+", " ").strip();
        if (content.length() > 220) {
            content = content.substring(0, 220) + "...";
        }
        return "- " + content;
    }
}
