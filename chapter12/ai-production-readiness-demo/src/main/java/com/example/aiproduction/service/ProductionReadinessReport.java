package com.example.aiproduction.service;

import com.example.aiproduction.cost.CostEstimate;
import com.example.aiproduction.cost.ModelRouteDecision;
import com.example.aiproduction.eval.EvaluationSummary;
import com.example.aiproduction.observability.TraceEvent;
import com.example.aiproduction.security.SecurityReview;
import java.util.List;

/**
 * AI 应用生产化检查报告。
 *
 * <p>这个报告可以理解成一次请求的上线治理视图：选了什么模型、成本多少、安全是否拦截、评测是否达标、Trace 记录了什么。
 */
public record ProductionReadinessReport(
        ModelRouteDecision routeDecision,
        CostEstimate costEstimate,
        SecurityReview securityReview,
        EvaluationSummary evaluationSummary,
        List<TraceEvent> traceEvents) {

    public boolean readyForProduction(double minimumPassRate) {
        return !securityReview.blocked() && evaluationSummary.passRate() >= minimumPassRate;
    }
}
