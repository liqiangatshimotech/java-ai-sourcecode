package com.example.langgraph4j.aftersales.domain;

import java.util.List;

/** 模型理解请求后返回的结构化结果。 */
public record IntentAnalysis(AfterSalesIntent intent, String orderId, List<String> missingFields) {
    public IntentAnalysis {
        intent = intent == null ? AfterSalesIntent.UNKNOWN : intent;
        orderId = orderId == null ? "" : orderId.trim().toUpperCase();
        missingFields = missingFields == null ? List.of() : List.copyOf(missingFields);
    }

    public boolean needsMoreInformation() {
        return orderId.isBlank() || !missingFields.isEmpty();
    }
}
