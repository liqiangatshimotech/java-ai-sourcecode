package com.example.structuredoutput.model;

/** 离线失败桩：空摘要和超范围置信度用于验证耗尽重试后的人工复核标记。 */
public class AlwaysInvalidModelClient implements ModelClient {

    private int callCount = 0;

    @Override
    public String generate(String prompt) {
        callCount++;
        return """
            {
              "category": "BILLING",
              "priority": "HIGH",
              "summary": "",
              "requiredData": [],
              "confidence": 1.8
            }
            """;
    }

    public int callCount() {
        return callCount;
    }
}
