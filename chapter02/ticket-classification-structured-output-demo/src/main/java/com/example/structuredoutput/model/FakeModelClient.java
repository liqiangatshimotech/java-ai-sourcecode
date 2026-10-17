package com.example.structuredoutput.model;

/** 离线演示：先返回不合法的自然语言，再返回合法 JSON，不访问外部模型。 */
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

    /** 测试用调用次数，证明第二次成功来自重试。 */
    public int callCount() {
        return callCount;
    }
}
