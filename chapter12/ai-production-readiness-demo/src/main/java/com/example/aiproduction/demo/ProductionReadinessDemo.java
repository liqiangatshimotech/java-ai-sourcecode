package com.example.aiproduction.demo;

import com.example.aiproduction.config.ModelSettings;
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
import com.example.aiproduction.service.ProductionReadinessRequest;
import com.example.aiproduction.service.ProductionReadinessService;
import java.util.List;

/**
 * 12.6 项目实战入口。
 */
public class ProductionReadinessDemo {

    public static void main(String[] args) {
        ModelSettings settings = ModelSettings.fromEnvironment();
        System.out.println("当前模型配置：" + settings.summary());

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

        ProductionReadinessRequest request =
                new ProductionReadinessRequest(
                        "trace-prod-001",
                        "客服工单摘要",
                        "用户 13812345678 反馈 ORDER-2002 一直不发货，要求退款。",
                        "order.query",
                        900);

        List<EvaluationCase> cases =
                List.of(
                        new EvaluationCase(
                                "ticket-refund-001",
                                "ORDER-2002 一直不发货，能退款吗？",
                                "order.query",
                                "库存不足",
                                true));
        DemoAgentAnswer answer = new DemoAgentAnswer("order.query", "订单库存不足，建议人工复核退款诉求。", true);

        var report = service.inspect(request, cases, answer, false);
        System.out.println(report);
        System.out.println("审计事件数量：" + auditLogger.records().size());
        System.out.println("是否达到上线门禁：" + report.readyForProduction(0.8));
    }
}
