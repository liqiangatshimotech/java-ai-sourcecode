package com.example.aicustomerservice.controller;

import com.example.aicustomerservice.ai.AiProvider;
import com.example.aicustomerservice.ai.ModelSettings;
import com.example.aicustomerservice.entity.EnterpriseProblemType;
import com.example.aicustomerservice.entity.IntentType;
import com.example.aicustomerservice.entity.ModelTier;
import com.example.aicustomerservice.entity.ProductionGuardrail;
import com.example.aicustomerservice.entity.SupportDecision;
import com.example.aicustomerservice.entity.SupportIntent;
import com.example.aicustomerservice.entity.SupportTicket;
import com.example.aicustomerservice.entity.TicketPriority;
import com.example.aicustomerservice.service.CustomerSupportService;
import com.example.aicustomerservice.service.AiRunService;
import com.example.aicustomerservice.service.EnterpriseProblemCatalog;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@org.springframework.test.context.TestPropertySource(properties="support.auth.enabled=false")
@WebMvcTest(controllers = {CustomerSupportController.class, ProductionController.class})
@Import(CustomerSupportControllerTest.ControllerTestConfiguration.class)
class CustomerSupportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomerSupportService supportService;

    @MockBean
    private AiRunService aiRunService;

    @TestConfiguration
    static class ControllerTestConfiguration {

        @Bean
        ModelSettings modelSettings() {
            return new ModelSettings(
                AiProvider.DEEPSEEK,
                "https://api.deepseek.com/v1",
                "test",
                "test-model",
                0.0
            );
        }

        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-06-22T00:00:00Z"), ZoneOffset.UTC);
        }

        @Bean
        EnterpriseProblemCatalog enterpriseProblemCatalog() {
            return new EnterpriseProblemCatalog();
        }
    }

    @Test
    void failedModelRequestMustStillHaveRequestIdAndFailureAudit() throws Exception {
        var failure = new com.example.aicustomerservice.common.BusinessException("MODEL_CALL_FAILED", "测试模型故障");
        org.mockito.Mockito.when(supportService.handle(
            org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.isNull())).thenThrow(failure);
        mockMvc.perform(post("/api/support/chat").contentType(MediaType.APPLICATION_JSON)
                .content("{\"sessionId\":\"S-failed\",\"customerId\":\"C10003\",\"question\":\"发票\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().exists("X-Request-Id"));
        org.mockito.Mockito.verify(aiRunService).begin(org.mockito.ArgumentMatchers.anyString(), eq("S-failed"),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        org.mockito.Mockito.verify(aiRunService).failed(org.mockito.ArgumentMatchers.anyString(), eq("S-failed"),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), eq(failure), org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void shouldReturnRuntimeConfig() throws Exception {
        mockMvc.perform(get("/api/support/runtime"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.provider").value("DEEPSEEK"))
            .andExpect(jsonPath("$.storage").value("postgres"));
    }

    @Test
    void shouldChatAndCreateTicketWhenRefundNeedsHumanCheck() throws Exception {
        when(supportService.handle(
            eq("S-web-1"),
            eq("C10001"),
            eq("这个订单能直接退款吗？"),
            eq("SO202606210001")
        )).thenReturn(refundDecision());

        mockMvc.perform(post("/api/support/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "sessionId": "S-web-1",
                      "customerId": "C10001",
                      "orderNo": "SO202606210001",
                      "question": "这个订单能直接退款吗？"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.handoffRequired").value(true))
            .andExpect(jsonPath("$.ticketId", containsString("CS-")))
            .andExpect(jsonPath("$.modelRuntime", containsString("test-runtime")))
            .andExpect(jsonPath("$.guardrails[0].section").exists());
    }

    @Test
    void shouldExposeEnterpriseProblemCatalog() throws Exception {
        mockMvc.perform(get("/api/support/production/issues"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].section").value("8.1"))
            .andExpect(jsonPath("$[3].problem").value("高并发延迟"));
    }

    private SupportDecision refundDecision() {
        Instant now = Instant.parse("2026-06-22T00:00:00Z");
        SupportTicket ticket = SupportTicket.create(
            "CS-20260622-TEST0001",
            "C10001",
            "S-web-1",
            "这个订单能直接退款吗？",
            "已创建售后工单。",
            IntentType.REFUND.name(),
            TicketPriority.URGENT,
            "idem-test",
            now
        );
        return new SupportDecision(
            "S-web-1",
            "C10001",
            new SupportIntent(IntentType.REFUND, 0.86, "测试意图"),
            "已创建售后工单。",
            true,
            ticket,
            List.of(),
            List.of(),
            List.of(new ProductionGuardrail(
                EnterpriseProblemType.HALLUCINATION_RISK,
                "test-signal",
                "test-action",
                true
            )),
            false,
            ModelTier.STANDARD,
            false,
            0,
            "test-runtime"
        );
    }
}
