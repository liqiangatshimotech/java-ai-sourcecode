package com.example.langgraph4j.aftersales.domain;

import java.util.List;

/** 模型理解请求后返回的结构化结果。 */
public record IntentAnalysis(AfterSalesIntent intent, String orderId, List<String> missingFields) {
    public IntentAnalysis {
        intent = intent == null ? AfterSalesIntent.UNKNOWN : intent;
        orderId = orderId == null ? "" : orderId.trim().toUpperCase(java.util.Locale.ROOT);
        java.util.ArrayList<String> normalized = new java.util.ArrayList<>(missingFields == null ? List.of() : missingFields);
        // 模型可能漏报 missingFields，不能因此绕过追问直接查询空订单号。
        if (orderId.isBlank() && !normalized.contains("orderId")) normalized.add("orderId");
        missingFields = List.copyOf(normalized);
    }

    public boolean needsMoreInformation() {
        return orderId.isBlank() || !missingFields.isEmpty();
    }
}
