package com.example.langgraph4j.aftersales.ai;

import com.example.langgraph4j.aftersales.domain.AfterSalesIntent;
import com.example.langgraph4j.aftersales.domain.IntentAnalysis;
import com.example.langgraph4j.aftersales.domain.OrderSnapshot;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 无 API Key 时使用的离线替身。它不是用来模拟模型质量，而是让状态图、
 * checkpoint、审批恢复和幂等测试在任何机器上都可重复运行。
 */
public class RuleBasedAfterSalesAssistant implements AfterSalesAssistant {

    private static final Pattern ORDER_ID = Pattern.compile("(?i)\\bORD[-_]?\\d{4,12}\\b");

    @Override
    public IntentAnalysis analyzeRequest(String userMessage, String knownOrderId) {
        String message = userMessage == null ? "" : userMessage;
        String normalized = message.toLowerCase(Locale.ROOT);
        AfterSalesIntent intent = normalized.contains("退款") || normalized.contains("退货")
                ? AfterSalesIntent.REFUND
                : AfterSalesIntent.CONSULTATION;

        Matcher matcher = ORDER_ID.matcher(message);
        String orderId = matcher.find() ? matcher.group().replace('_', '-').toUpperCase(Locale.ROOT)
                : knownOrderId == null ? "" : knownOrderId;
        List<String> missing = orderId.isBlank() ? List.of("orderId") : List.of();
        return new IntentAnalysis(intent, orderId, missing);
    }

    @Override
    public String answerConsultation(String userMessage, OrderSnapshot order) {
        return "订单 %s 的商品是「%s」，实付 %s 元，当前可以继续处理售后咨询。"
                .formatted(order.orderId(), order.productName(), order.paidAmount());
    }

    @Override
    public String explainRefundResult(String decision, OrderSnapshot order, String detail) {
        return "订单 %s 的退款结论：%s。%s".formatted(order.orderId(), decision, detail);
    }
}
