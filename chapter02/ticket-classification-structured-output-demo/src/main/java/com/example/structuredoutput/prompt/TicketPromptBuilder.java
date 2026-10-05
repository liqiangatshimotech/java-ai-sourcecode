package com.example.structuredoutput.prompt;

public final class TicketPromptBuilder {

    private TicketPromptBuilder() {
    }

    public static String build(String ticketText, String repairHint) {
        return """
            你是 SaaS 客服系统的工单分类器。

            任务：
            根据用户提交的工单内容，判断工单类别、优先级，并给出一句话摘要。

            category 只能从以下枚举选择：
            - BILLING：账单、退款、发票、扣费问题
            - BUG：系统异常、报错、功能不可用
            - ACCOUNT：登录、权限、账号、安全问题
            - FEATURE_REQUEST：功能建议和产品需求
            - OTHER：无法归类的问题

            priority 只能从以下枚举选择：
            - LOW：不影响主流程
            - MEDIUM：影响单个用户，但可以绕过
            - HIGH：阻断用户关键流程，或涉及客户投诉
            - URGENT：资金风险、账号安全、大面积不可用或重大客户阻断

            输出要求：
            1. 只返回一个 JSON 对象。
            2. 不要返回 Markdown 代码块。
            3. 不要解释分类理由。
            4. 不要编造用户没有提供的信息。
            5. 如果继续处理还缺少信息，写入 requiredData。
            6. confidence 必须是 0 到 1 的小数。

            JSON 字段：
            {
              "category": "BILLING | BUG | ACCOUNT | FEATURE_REQUEST | OTHER",
              "priority": "LOW | MEDIUM | HIGH | URGENT",
              "summary": "不超过 80 个字符的一句话摘要",
              "requiredData": ["继续处理前缺失的信息"],
              "confidence": 0.0
            }

            %s

            用户工单：
            %s
            """.formatted(
                repairHint == null ? "" : repairHint,
                ticketText
            );
    }
}
