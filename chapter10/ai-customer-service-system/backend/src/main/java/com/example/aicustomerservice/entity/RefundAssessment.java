package com.example.aicustomerservice.entity;

/**
 * 退款工具的结构化评估结果。
 *
 * canAutoRefund 和 requiresHumanReview 是两件事：日用品这类规则明确拒绝的订单，既不能自动退款，
 * 也不需要人工介入，AI 可以直接把规则结论告诉客户；拣货中、已发货、退款复核中的订单才需要人工确认。
 * 护栏只读 requiresHumanReview，不去匹配 reason / nextStep 里的中文措辞。
 */
public record RefundAssessment(
    String orderNo,
    boolean canAutoRefund,
    boolean requiresHumanReview,
    String reason,
    String nextStep
) {
}
