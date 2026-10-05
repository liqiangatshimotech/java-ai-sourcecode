package com.example.springairag.rag;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class RagPromptBuilder {

    private static final String SYSTEM_PROMPT = """
        你是企业知识库问答助手。
        你只能基于用户问题和检索上下文回答。
        如果检索上下文没有足够依据，请明确说“知识库中没有找到足够依据”，不要编造。
        回答要简洁、专业，涉及流程时给出步骤。
        回答末尾必须列出引用来源。
        """;

    public RagPrompt build(String question, String tenantId, List<RetrievedChunk> chunks) {
        String context = chunks.isEmpty()
            ? "无"
            : chunks.stream()
                .map(this::formatChunk)
                .collect(Collectors.joining("\n\n"));

        String user = """
            用户问题：
            %s

            当前租户：
            %s

            检索上下文：
            %s

            输出要求：
            1. 先直接回答用户问题。
            2. 再列出你使用的引用来源，格式为“来源：标题 - source”。
            3. 如果上下文不足，不要猜测，直接拒答并说明还缺什么资料。
            """.formatted(question, tenantId, context);

        return new RagPrompt(SYSTEM_PROMPT, user, question, tenantId, List.copyOf(chunks));
    }

    private String formatChunk(RetrievedChunk retrieved) {
        DocumentChunk chunk = retrieved.chunk();
        return """
            [chunkId=%s, score=%.4f]
            标题：%s
            来源：%s
            内容：
            %s
            """.formatted(
            chunk.id(),
            retrieved.score(),
            chunk.title(),
            chunk.source(),
            chunk.content()
        );
    }
}
