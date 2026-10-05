package com.example.aiproduction.service;

import com.example.aiproduction.cost.CostCalculator;
import com.example.aiproduction.cost.ModelRouteDecision;
import com.example.aiproduction.cost.ModelRouter;
import com.example.aiproduction.cost.TokenUsage;
import com.example.aiproduction.eval.EvaluationCase;
import com.example.aiproduction.eval.EvaluationRunner;
import com.example.aiproduction.eval.EvaluationRunner.DemoAgentAnswer;
import com.example.aiproduction.observability.AgentTrace;
import com.example.aiproduction.observability.AuditLogger;
import com.example.aiproduction.security.SecurityGateway;
import java.util.List;
import java.util.Map;

/**
 * AI 应用生产化治理服务。
 *
 * <p>这个类把成本、模型路由、安全、评测和可观测性合在一起，模拟一次请求上线前应该经过的治理链路。
 */
public class ProductionReadinessService {

    private final CostCalculator costCalculator;
    private final ModelRouter modelRouter;
    private final SecurityGateway securityGateway;
    private final EvaluationRunner evaluationRunner;
    private final AuditLogger auditLogger;

    public ProductionReadinessService(
            CostCalculator costCalculator,
            ModelRouter modelRouter,
            SecurityGateway securityGateway,
            EvaluationRunner evaluationRunner,
            AuditLogger auditLogger) {
        this.costCalculator = costCalculator;
        this.modelRouter = modelRouter;
        this.securityGateway = securityGateway;
        this.evaluationRunner = evaluationRunner;
        this.auditLogger = auditLogger;
    }

    public ProductionReadinessReport inspect(
            ProductionReadinessRequest request,
            List<EvaluationCase> cases,
            DemoAgentAnswer demoAnswer,
            boolean localOnly) {
        AgentTrace trace = new AgentTrace(request.traceId());
        trace.add("request.received", "收到 AI 请求", Map.of("taskType", request.taskType()));

        TokenUsage usage = costCalculator.estimateTokenUsage(request.prompt(), request.expectedOutputTokens());
        trace.add(
                "cost.estimated",
                "完成 Token 预估",
                Map.of("inputTokens", String.valueOf(usage.inputTokens()), "outputTokens", String.valueOf(usage.outputTokens())));

        ModelRouteDecision routeDecision = modelRouter.route(request.taskType(), usage, localOnly);
        trace.add("model.routed", routeDecision.reason(), Map.of("model", routeDecision.selectedModel().modelName()));

        var costEstimate = costCalculator.estimate(routeDecision.selectedModel(), usage);
        var securityReview = securityGateway.review(request.prompt(), request.requestedTool());
        trace.add("security.reviewed", securityReview.blocked() ? "安全检查阻断" : "安全检查通过");

        var evaluationSummary = evaluationRunner.run(cases, demoAnswer);
        trace.add(
                "evaluation.finished",
                "完成离线评测",
                Map.of("passRate", "%.2f".formatted(evaluationSummary.passRate())));

        trace.events().forEach(auditLogger::record);
        return new ProductionReadinessReport(
                routeDecision, costEstimate, securityReview, evaluationSummary, trace.events());
    }
}
