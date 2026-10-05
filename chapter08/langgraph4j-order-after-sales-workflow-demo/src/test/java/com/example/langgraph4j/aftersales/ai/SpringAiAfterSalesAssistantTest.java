package com.example.langgraph4j.aftersales.ai;

import com.example.langgraph4j.aftersales.domain.AfterSalesIntent;
import com.example.langgraph4j.aftersales.domain.OrderSnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Fake ChatModel 直接实现 Spring AI 接口，不发网络请求，验证适配层提示和解析逻辑。
 */
class SpringAiAfterSalesAssistantTest {

    @Test
    void parsesIntentJsonAndUsesModelForBothKindsOfAnswers() {
        ScriptedChatModel fake = new ScriptedChatModel(
                "```json\n{\"intent\":\"REFUND\",\"orderId\":\"ORD-1001\",\"missingFields\":[]}\n```",
                "这是咨询结果",
                "这是退款结果");
        SpringAiAfterSalesAssistant assistant = new SpringAiAfterSalesAssistant(fake, new ObjectMapper());
        OrderSnapshot order = new OrderSnapshot("ORD-1001", "course", new BigDecimal("129"),
                LocalDate.now(), false, false);

        var analysis = assistant.analyzeRequest("我要退款", "");
        assertEquals(AfterSalesIntent.REFUND, analysis.intent());
        assertEquals("ORD-1001", analysis.orderId());
        assertEquals("这是咨询结果", assistant.answerConsultation("查订单", order));
        assertEquals("这是退款结果", assistant.explainRefundResult("REFUNDED", order, "ok"));
        assertEquals(3, fake.prompts.size());
    }

    @Test
    void invalidJsonFailsClearly() {
        SpringAiAfterSalesAssistant assistant = new SpringAiAfterSalesAssistant(
                new ScriptedChatModel("not-json"), new ObjectMapper());
        assertThrows(IllegalArgumentException.class,
                () -> assistant.analyzeRequest("退款", ""));
    }

    private static final class ScriptedChatModel implements ChatModel {
        private final Deque<String> responses;
        private final List<Prompt> prompts = new java.util.ArrayList<>();

        private ScriptedChatModel(String... responses) {
            this.responses = new ArrayDeque<>(List.of(responses));
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            prompts.add(prompt);
            String response = responses.removeFirst();
            return new ChatResponse(List.of(new Generation(new AssistantMessage(response))));
        }
    }
}
