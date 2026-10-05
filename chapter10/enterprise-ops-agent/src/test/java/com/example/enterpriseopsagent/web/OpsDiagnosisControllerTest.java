package com.example.enterpriseopsagent.web;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "ops.storage.type=memory",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"
})
@AutoConfigureMockMvc
class OpsDiagnosisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldExposeRuntimeInfo() throws Exception {
        mockMvc.perform(get("/api/ops/runtime"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.service").value("enterprise-ops-agent"))
            .andExpect(jsonPath("$.provider").value("DEEPSEEK"));
    }

    @Test
    void shouldDiagnoseAlertThroughHttpApi() throws Exception {
        String requestBody = """
            {
              "alertId": "ALERT-HTTP-001",
              "serviceName": "payment-service",
              "environment": "prod",
              "severity": "P1",
              "title": "支付服务错误率升高",
              "description": "支付回调接口出现大量 timeout，P95 延迟明显升高。"
            }
            """;

        mockMvc.perform(post("/api/ops/diagnose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.alertId").value("ALERT-HTTP-001"))
            .andExpect(jsonPath("$.modelProvider").value("DEEPSEEK"))
            .andExpect(jsonPath("$.requiresApproval").value(true))
            .andExpect(jsonPath("$.evidence", containsString("发布证据")));
    }

    @Test
    void shouldRejectInvalidAlertRequest() throws Exception {
        String requestBody = """
            {
              "alertId": "",
              "serviceName": "payment-service",
              "environment": "prod",
              "severity": "P1",
              "title": "支付服务错误率升高",
              "description": "支付回调接口出现大量 timeout。"
            }
            """;

        mockMvc.perform(post("/api/ops/diagnose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRunIncidentLifecycleThroughHttpApi() throws Exception {
        String requestBody = """
            {
              "alertId": "ALERT-LIFECYCLE-001",
              "serviceName": "payment-service",
              "environment": "prod",
              "severity": "P1",
              "title": "支付服务错误率升高",
              "description": "支付回调接口出现大量 timeout，P95 延迟明显升高。"
            }
            """;

        MvcResult createResult = mockMvc.perform(post("/api/ops/alerts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.incidentId").exists())
            .andExpect(jsonPath("$.status").value("WAITING_APPROVAL"))
            .andExpect(jsonPath("$.approvalTicketId").value("APPROVAL-payment-service-prod-ROLLBACK"))
            .andExpect(jsonPath("$.report.evidence", containsString("发布证据")))
            .andExpect(jsonPath("$.timeline[2].type").value("APPROVAL_REQUESTED"))
            .andReturn();

        String incidentId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.incidentId");

        mockMvc.perform(get("/api/ops/incidents/{incidentId}", incidentId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.incidentId").value(incidentId))
            .andExpect(jsonPath("$.status").value("WAITING_APPROVAL"));

        mockMvc.perform(get("/api/ops/incidents")
                .param("status", "WAITING_APPROVAL")
                .param("serviceName", "payment-service")
                .param("environment", "prod"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].incidentId").value(incidentId));

        mockMvc.perform(post("/api/ops/incidents/{incidentId}/decision", incidentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "decision": "APPROVE",
                      "operator": "sre-zhangsan",
                      "comment": "同意回滚到上一稳定版本"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APPROVED"))
            .andExpect(jsonPath("$.timeline[3].type").value("APPROVAL_APPROVE"));

        mockMvc.perform(post("/api/ops/incidents/{incidentId}/resolve", incidentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "operator": "sre-zhangsan",
                      "summary": "回滚后错误率恢复到 0.3%"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("RESOLVED"))
            .andExpect(jsonPath("$.resolvedAt").exists());
    }

    @Test
    void shouldReturnExistingIncidentForDuplicatedAlert() throws Exception {
        String requestBody = """
            {
              "alertId": "ALERT-DUP-HTTP-001",
              "serviceName": "payment-service",
              "environment": "prod",
              "severity": "P1",
              "title": "支付服务错误率升高",
              "description": "支付回调接口出现大量 timeout，P95 延迟明显升高。"
            }
            """;

        MvcResult firstResult = mockMvc.perform(post("/api/ops/alerts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andReturn();

        String incidentId = JsonPath.read(firstResult.getResponse().getContentAsString(), "$.incidentId");

        mockMvc.perform(post("/api/ops/alerts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.incidentId").value(incidentId))
            .andExpect(jsonPath("$.duplicateCount").value(1))
            .andExpect(jsonPath("$.timeline[3].type").value("DUPLICATE_ALERT_RECEIVED"));
    }

    @Test
    void shouldReturnNotFoundWhenIncidentDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/ops/incidents/INC-NOT-FOUND"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("排障事件不存在：INC-NOT-FOUND"));
    }
}
