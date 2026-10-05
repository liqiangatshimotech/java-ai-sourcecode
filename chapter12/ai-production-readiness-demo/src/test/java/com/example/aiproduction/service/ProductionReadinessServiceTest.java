package com.example.aiproduction.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.aiproduction.cost.CostCalculator;
import com.example.aiproduction.cost.ModelRouter;
import com.example.aiproduction.eval.EvaluationCase;
import com.example.aiproduction.eval.EvaluationRunner;
import com.example.aiproduction.eval.EvaluationRunner.DemoAgentAnswer;
import com.example.aiproduction.observability.InMemoryAuditLogger;
import com.example.aiproduction.security.PromptInjectionDetector;
import com.example.aiproduction.security.SecurityGateway;
import com.example.aiproduction.security.SensitiveDataMasker;
import com.example.aiproduction.security.ToolRiskPolicy;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProductionReadinessServiceTest {

    @Test
    void createsReadinessReport() {
        InMemoryAuditLogger auditLogger = new InMemoryAuditLogger();
        ProductionReadinessService service =
                new ProductionReadinessService(
                        new CostCalculator(),
                        new ModelRouter(),
                        new SecurityGateway(
                                new SensitiveDataMasker(),
                                new PromptInjectionDetector(),
                                new ToolRiskPolicy()),
                        new EvaluationRunner(),
                        auditLogger);

        var request =
                new ProductionReadinessRequest(
                        "trace-1", "客服工单摘要", "用户咨询 ORDER-2002 是否可以退款", "order.query", 600);
        var cases =
                List.of(new EvaluationCase("case-1", "退款问题", "order.query", "库存不足", true));
        var answer = new DemoAgentAnswer("order.query", "订单库存不足，需要人工复核。", true);

        ProductionReadinessReport report = service.inspect(request, cases, answer, false);

        assertTrue(report.readyForProduction(0.8));
        assertFalse(auditLogger.records().isEmpty());
    }
}
