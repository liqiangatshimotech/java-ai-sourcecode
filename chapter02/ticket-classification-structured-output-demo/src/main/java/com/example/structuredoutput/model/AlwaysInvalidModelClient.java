package com.example.structuredoutput.model;

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
