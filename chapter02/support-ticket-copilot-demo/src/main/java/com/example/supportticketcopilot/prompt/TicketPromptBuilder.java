package com.example.supportticketcopilot.prompt;

import com.example.supportticketcopilot.dto.AnalyzeTicketRequest;
import com.example.supportticketcopilot.dto.CustomerChannel;

public final class TicketPromptBuilder {

    public static final String ANALYSIS_SYSTEM = """
        你是 SaaS 企业客服系统的工单分析助手。
        你必须遵守输出契约，只返回可解析成 Java DTO 的结构化结果。
        不要编造用户没有提供的信息，不要输出 Markdown 代码块。
        """;

    public static final String REPLY_SYSTEM = """
        你是 SaaS 企业客服团队的中文回复助手。
        你只生成给客户看的回复草稿，语气专业、克制、清楚。
        不承诺无法确认的赔付、退款或完成时间。
        """;

    private TicketPromptBuilder() {
    }

    public static String buildAnalysisPrompt(AnalyzeTicketRequest request, String repairHint) {
        return """
            请分析下面的客户工单，并返回一个结构化对象。

            业务上下文：
            - 渠道：%s
            - 客户等级：%s

            分类枚举：
            - BILLING：账单、退款、发票、扣费问题
            - BUG：系统异常、报错、功能不可用
            - ACCOUNT：登录、权限、账号、安全问题
            - FEATURE_REQUEST：功能建议和产品需求
            - OTHER：无法归类的问题

            优先级枚举：
            - LOW：不影响主流程
            - MEDIUM：影响单个用户，但可以绕过
            - HIGH：阻断用户关键流程，或涉及客户投诉
            - URGENT：资金风险、账号安全、大面积不可用或重大客户阻断

            输出字段契约：
            {
              "category": "BILLING | BUG | ACCOUNT | FEATURE_REQUEST | OTHER",
              "priority": "LOW | MEDIUM | HIGH | URGENT",
              "summary": "不超过 80 个字符的一句话摘要",
              "requiredData": ["继续处理前还缺失的信息，没有则返回空数组"],
              "nextActions": [
                {
                  "ownerTeam": "CUSTOMER_SUPPORT | BILLING | ENGINEERING | SECURITY | PRODUCT",
                  "description": "要做什么",
                  "nextStep": "下一步动作"
                }
              ],
              "customerReply": {
                "subject": "不超过 80 个字符的回复主题",
                "body": "不超过 600 个字符的客户回复草稿",
                "tone": "APOLOGETIC | PROFESSIONAL | REASSURING"
              },
              "confidence": 0.0
            }

            约束：
            1. 只返回 JSON 对象，不返回解释。
            2. 所有枚举值必须完全匹配上面的英文枚举。
            3. summary、requiredData、nextActions 和 customerReply 都必须基于原始工单。
            4. 不能补充订单号、金额、时间等用户没有提供的事实。
            5. 企业客户或资金相关问题，优先级至少为 HIGH。

            %s

            原始工单：
            %s
            """.formatted(
            request.channel(),
            request.customerTier(),
            repairHint == null ? "" : repairHint,
            request.content()
        );
    }

    public static String buildRepairHint(RuntimeException ex) {
        String message = ex.getClass().getSimpleName();
        if (message != null && message.length() > 300) {
            message = message.substring(0, 300);
        }

        return """
            上一次输出没有通过 Java 解析或 Bean Validation 校验。
            请严格修复输出结构，只返回合法 JSON。
            错误摘要：%s
            """.formatted(message == null ? "未知错误" : message);
    }

    public static String buildReplyPrompt(String ticketContent, CustomerChannel channel) {
        return """
            请基于下面的客户工单生成中文客服回复草稿。

            渠道：%s

            写作要求：
            1. 先确认已经收到问题。
            2. 对客户造成的不便表达歉意。
            3. 清楚说明需要客户补充哪些信息。
            4. 不承诺具体退款、赔偿或修复时间。
            5. 语气适合直接复制到客服系统。

            客户工单：
            %s
            """.formatted(channel, ticketContent);
    }
}
