package com.example.langchain4jrag.model;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;

import java.util.ArrayList;
import java.util.List;

public class GroundedChatModel implements ChatModel {

    @Override
    public ChatResponse doChat(ChatRequest request) {
        String userMessage = lastUserMessage(request.messages());
        String answer = answerFromInjectedContext(userMessage);

        return ChatResponse.builder()
            .aiMessage(AiMessage.from(answer))
            .modelName("deterministic-grounded-chat-model")
            .build();
    }

    private String lastUserMessage(List<ChatMessage> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage message = messages.get(i);
            if (message.type() == ChatMessageType.USER && message instanceof UserMessage userMessage) {
                return userMessage.singleText();
            }
        }
        return "";
    }

    private String answerFromInjectedContext(String prompt) {
        if (prompt.isBlank()) {
            return "资料不足，无法确认处理方案。请升级人工客服。";
        }

        List<String> facts = extractUsefulFacts(prompt);
        if (facts.isEmpty()) {
            return "资料不足，无法确认处理方案。请升级人工客服。";
        }

        StringBuilder answer = new StringBuilder();
        answer.append("根据知识库资料，建议这样处理：\n");
        for (int i = 0; i < Math.min(3, facts.size()); i++) {
            answer.append(i + 1).append(". ").append(facts.get(i)).append('\n');
        }
        answer.append("回复客户时要说明处理状态、预计时效和后续联系人。");
        return answer.toString().strip();
    }

    private List<String> extractUsefulFacts(String prompt) {
        List<String> facts = new ArrayList<>();
        for (String paragraph : prompt.split("\\R\\s*\\R|\\R")) {
            String normalized = paragraph
                .replace("{{contents}}", "")
                .replaceFirst("(?i)^content:\\s*", "")
                .replace("[", "")
                .replace("]", "")
                .strip();
            if (normalized.length() < 12) {
                continue;
            }
            if (looksLikeInstruction(normalized)) {
                continue;
            }
            if (normalized.endsWith("？") || normalized.endsWith("?") || normalized.startsWith("#")) {
                continue;
            }
            if (normalized.contains("重复扣费")
                || normalized.contains("退款")
                || normalized.contains("发票")
                || normalized.contains("发货")
                || normalized.contains("SLA")) {
                facts.add(trimFact(normalized));
            }
        }
        return facts;
    }

    private boolean looksLikeInstruction(String text) {
        return text.startsWith("Answer using")
            || text.startsWith("You are")
            || text.startsWith("用户")
            || text.startsWith("问题")
            || text.startsWith("根据以下")
            || text.startsWith("参考资料")
            || text.startsWith("metadata:");
    }

    private String trimFact(String text) {
        String compact = text.replaceAll("\\s+", " ");
        return compact.length() <= 160 ? compact : compact.substring(0, 157) + "...";
    }
}
