package com.example.structuredoutput.model;

public class FakeModelClient implements ModelClient {

    private int callCount = 0;

    @Override
    public String generate(String prompt) {
        callCount++;

        if (callCount == 1) {
            return "这个用户是账单问题，优先级较高，需要客服尽快处理。";
        }

        return """
            {
              "category": "BILLING",
              "priority": "HIGH",
              "summary": "用户遇到重复扣费和发票开具失败问题",
              "requiredData": ["订单号", "扣费时间", "发票抬头"],
              "confidence": 0.86
            }
            """;
    }

    public int callCount() {
        return callCount;
    }
}
