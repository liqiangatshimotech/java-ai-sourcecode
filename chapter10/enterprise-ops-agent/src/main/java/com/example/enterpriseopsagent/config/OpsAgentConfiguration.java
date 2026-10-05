package com.example.enterpriseopsagent.config;

import com.example.enterpriseopsagent.agent.AuditLogService;
import com.example.enterpriseopsagent.agent.ConsoleAuditLogService;
import com.example.enterpriseopsagent.agent.DiagnosisModel;
import com.example.enterpriseopsagent.agent.OpsAgentService;
import com.example.enterpriseopsagent.agent.OpsToolExecutor;
import com.example.enterpriseopsagent.agent.RuleBasedDiagnosisModel;
import com.example.enterpriseopsagent.agent.RunbookRetriever;
import com.example.enterpriseopsagent.incident.DefaultIncidentIdGenerator;
import com.example.enterpriseopsagent.incident.InMemoryIncidentRepository;
import com.example.enterpriseopsagent.incident.IncidentIdGenerator;
import com.example.enterpriseopsagent.incident.IncidentRepository;
import com.example.enterpriseopsagent.incident.IncidentWorkflowService;
import com.example.enterpriseopsagent.incident.MySqlIncidentRepository;
import com.example.enterpriseopsagent.incident.SlaPolicy;
import com.example.enterpriseopsagent.tool.ApprovalService;
import com.example.enterpriseopsagent.tool.InMemoryOpsServices;
import com.example.enterpriseopsagent.tool.OpsTools;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;

/**
 * Agent 运行时装配。
 *
 * 这里先把真实企业系统抽象成接口。incident 默认写入 MySQL；
 * 日志、监控、发布和审批暂时使用内存实现，后续替换对应 Bean 即可。
 */
@Configuration
public class OpsAgentConfiguration {

    @Bean
    public ModelSettings modelSettings(@Value("${ops.ai.provider:deepseek}") String providerName) {
        return ModelSettings.fromProviderName(providerName);
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public ApprovalService approvalService() {
        return InMemoryOpsServices.approvals();
    }

    @Bean
    public OpsTools opsTools(ApprovalService approvalService) {
        return new OpsTools(
            InMemoryOpsServices.logs(),
            InMemoryOpsServices.metrics(),
            InMemoryOpsServices.deployments(),
            approvalService
        );
    }

    @Bean
    public RunbookRetriever runbookRetriever() {
        return (serviceName, description) ->
            "如果 " + serviceName + " 出现 timeout，先检查最近发布、下游超时、线程池、网关错误率和数据库连接池。";
    }

    @Bean
    public DiagnosisModel diagnosisModel(ModelSettings settings, @Value("${ops.ai.live:false}") boolean live) {
        return live ? new com.example.enterpriseopsagent.agent.HttpDiagnosisModel(settings) : new RuleBasedDiagnosisModel();
    }

    @Bean
    public AuditLogService auditLogService() {
        return new ConsoleAuditLogService();
    }

    @Bean
    public OpsAgentService opsAgentService(
        RunbookRetriever runbookRetriever,
        OpsTools opsTools,
        DiagnosisModel diagnosisModel,
        AuditLogService auditLogService
    ) {
        return new OpsAgentService(
            runbookRetriever,
            new OpsToolExecutor(opsTools),
            diagnosisModel,
            auditLogService
        );
    }

    @Bean
    @ConditionalOnProperty(name = "ops.storage.type", havingValue = "memory")
    public IncidentRepository inMemoryIncidentRepository() {
        return new InMemoryIncidentRepository();
    }

    @Bean
    @ConditionalOnProperty(name = "ops.storage.type", havingValue = "mysql", matchIfMissing = true)
    public IncidentRepository mySqlIncidentRepository(
        JdbcTemplate jdbcTemplate,
        TransactionTemplate transactionTemplate
    ) {
        return new MySqlIncidentRepository(jdbcTemplate, transactionTemplate);
    }

    @Bean
    public IncidentIdGenerator incidentIdGenerator(Clock clock) {
        return new DefaultIncidentIdGenerator(clock);
    }

    @Bean
    public SlaPolicy slaPolicy() {
        return new SlaPolicy();
    }

    @Bean
    public IncidentWorkflowService incidentWorkflowService(
        OpsAgentService opsAgentService,
        ApprovalService approvalService,
        IncidentRepository incidentRepository,
        IncidentIdGenerator incidentIdGenerator,
        SlaPolicy slaPolicy,
        Clock clock
    ) {
        return new IncidentWorkflowService(
            opsAgentService,
            approvalService,
            incidentRepository,
            incidentIdGenerator,
            slaPolicy,
            clock
        );
    }
}
