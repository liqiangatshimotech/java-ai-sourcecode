package com.example.enterpriseopsagent.web;

import com.example.enterpriseopsagent.agent.OpsAgentService;
import com.example.enterpriseopsagent.config.ModelSettings;
import com.example.enterpriseopsagent.domain.DiagnosisReport;
import com.example.enterpriseopsagent.domain.IncidentStatus;
import com.example.enterpriseopsagent.incident.IncidentQuery;
import com.example.enterpriseopsagent.incident.IncidentWorkflowService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.Clock;
import java.util.List;
import java.util.Map;

/**
 * 运维排障 Agent 的 HTTP API。
 *
 * 企业项目要能被其他系统调用，所以这里暴露真实 REST 接口，
 * 告警平台、工单系统或内部平台都可以通过 HTTP 把告警交给 Agent。
 */
@RestController
@RequestMapping("/api/ops")
public class OpsDiagnosisController {

    private final OpsAgentService opsAgentService;
    private final IncidentWorkflowService incidentWorkflowService;
    private final ModelSettings modelSettings;
    private final Clock clock;

    public OpsDiagnosisController(
        OpsAgentService opsAgentService,
        IncidentWorkflowService incidentWorkflowService,
        ModelSettings modelSettings,
        Clock clock
    ) {
        this.opsAgentService = opsAgentService;
        this.incidentWorkflowService = incidentWorkflowService;
        this.modelSettings = modelSettings;
        this.clock = clock;
    }

    @GetMapping("/runtime")
    public Map<String, Object> runtime() {
        return Map.of(
            "service", "enterprise-ops-agent",
            "provider", modelSettings.provider().name(),
            "model", modelSettings.modelName(),
            "baseUrl", modelSettings.baseUrl()
        );
    }

    @PostMapping("/diagnose")
    public OpsDiagnosisResponse diagnose(@Valid @RequestBody OpsDiagnosisRequest request) {
        DiagnosisReport report = opsAgentService.diagnose(request.toOpsAlert());
        return OpsDiagnosisResponse.from(report, modelSettings, clock.instant());
    }

    @PostMapping("/alerts")
    public IncidentResponse ingestAlert(@Valid @RequestBody OpsDiagnosisRequest request) {
        return IncidentResponse.from(
            incidentWorkflowService.ingestAlert(request.toOpsAlert()),
            clock.instant()
        );
    }

    @GetMapping("/incidents/{incidentId}")
    public IncidentResponse getIncident(@PathVariable("incidentId") String incidentId) {
        return IncidentResponse.from(
            incidentWorkflowService.getIncident(incidentId),
            clock.instant()
        );
    }

    @GetMapping("/incidents")
    public List<IncidentSummaryResponse> searchIncidents(
        @RequestParam(name = "status", required = false) IncidentStatus status,
        @RequestParam(name = "serviceName", required = false) String serviceName,
        @RequestParam(name = "environment", required = false) String environment
    ) {
        Instant now = clock.instant();
        return incidentWorkflowService.search(new IncidentQuery(status, serviceName, environment))
            .stream()
            .map(incident -> IncidentSummaryResponse.from(incident, now))
            .toList();
    }

    @PostMapping("/incidents/{incidentId}/decision")
    public IncidentResponse decideIncident(
        @PathVariable("incidentId") String incidentId,
        @Valid @RequestBody IncidentDecisionRequest request
    ) {
        return IncidentResponse.from(
            incidentWorkflowService.decide(incidentId, request.decision(), request.operator(), request.comment()),
            clock.instant()
        );
    }

    @PostMapping("/incidents/{incidentId}/resolve")
    public IncidentResponse resolveIncident(
        @PathVariable("incidentId") String incidentId,
        @Valid @RequestBody IncidentResolveRequest request
    ) {
        return IncidentResponse.from(
            incidentWorkflowService.resolve(incidentId, request.operator(), request.summary()),
            clock.instant()
        );
    }
}
