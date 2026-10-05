package com.example.langgraph4j.aftersales.ai;

import com.example.langgraph4j.aftersales.domain.AfterSalesIntent;
import com.example.langgraph4j.aftersales.domain.IntentAnalysis;
import com.example.langgraph4j.aftersales.domain.OrderSnapshot;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.model.ChatModel;

import java.util.ArrayList;
import java.util.List;

/** 使用 Spring AI {@link ChatModel} 实现课程定义的模型能力接口。 */
public class SpringAiAfterSalesAssistant implements AfterSalesAssistant {

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;

    public SpringAiAfterSalesAssistant(ChatModel chatModel, ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
    }

    @Override
    public IntentAnalysis analyzeRequest(String userMessage, String knownOrderId) {
        String prompt = """
                你是订单售后意图分析器。只返回 JSON，不要返回 Markdown。
                JSON 格式：{"intent":"REFUND|CONSULTATION|UNKNOWN","orderId":"","missingFields":[]}
                如果是退款、退货请求，intent=REFUND；其他订单咨询为 CONSULTATION。
                找不到订单号时，orderId 留空且 missingFields 包含 orderId。
                已知订单号：%s
                当前用户消息：%s
                """.formatted(knownOrderId == null ? "" : knownOrderId, userMessage);
        String raw = requireText(chatModel.call(prompt));
        return parseIntent(stripCodeFence(raw));
    }

    @Override
    public String answerConsultation(String userMessage, OrderSnapshot order) {
        return requireText(chatModel.call("""
                请用简洁、准确的中文回答订单售后咨询。不要虚构未提供的信息。
                用户问题：%s
                订单：%s，商品：%s，实付：%s，签收日期：%s
                """.formatted(userMessage, order.orderId(), order.productName(),
                order.paidAmount(), order.deliveredDate())));
    }

    @Override
    public String explainRefundResult(String decision, OrderSnapshot order, String detail) {
        return requireText(chatModel.call("""
                请把下面的退款执行结果改写成一段简洁中文，不得改变结论。
                订单：%s，结论：%s，细节：%s
                """.formatted(order.orderId(), decision, detail)));
    }

    private IntentAnalysis parseIntent(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            AfterSalesIntent intent = AfterSalesIntent.valueOf(root.path("intent").asText("UNKNOWN"));
            String orderId = root.path("orderId").asText("");
            List<String> missing = new ArrayList<>();
            root.path("missingFields").forEach(item -> missing.add(item.asText()));
            return new IntentAnalysis(intent, orderId, missing);
        }
        catch (Exception exception) {
            throw new IllegalArgumentException("模型没有返回可解析的意图 JSON", exception);
        }
    }

    private static String stripCodeFence(String text) {
        return text.replace("```json", "").replace("```", "").trim();
    }

    private static String requireText(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("模型返回了空内容");
        }
        return value.trim();
    }
}
