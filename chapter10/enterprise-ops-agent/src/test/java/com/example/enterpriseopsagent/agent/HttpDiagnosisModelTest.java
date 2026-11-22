package com.example.enterpriseopsagent.agent;

import com.example.enterpriseopsagent.config.*;
import com.example.enterpriseopsagent.domain.OpsAlert;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

/** 本地 HTTP 替身校验真实协议和错误路径，不消耗供应商费用。 */
class HttpDiagnosisModelTest {
    private HttpServer server;
    private final AtomicReference<String> request = new AtomicReference<>();
    @AfterEach void stop() { if (server != null) server.stop(0); }

    private JsonModelClient client(int status, String response) throws Exception {
        return new JsonModelClient(startStub(status, response), "test-only-key",
                "test-model", false, Duration.ofSeconds(2));
    }

    /** 启动一个只会返回固定响应的本地模型替身，返回它的 base URL。 */
    private String startStub(int status, String response) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", e -> {
            request.set(new String(e.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            e.getResponseHeaders().set("Content-Type", "application/json");
            e.sendResponseHeaders(status, bytes.length);
            e.getResponseBody().write(bytes); e.close();
        });
        server.start();
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    /** 用本地替身构造 HttpDiagnosisModel，替身返回的报告里故意带上模型自己声明的 requiresApproval。 */
    private HttpDiagnosisModel modelReturning(String recommendation, boolean claimedByModel) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String content = mapper.writeValueAsString(Map.of("summary", "待核验", "rootCauseHypothesis", "待验证假设",
                "recommendation", recommendation, "requiresApproval", claimedByModel));
        String completion = mapper.writeValueAsString(Map.of("choices",
                List.of(Map.of("finish_reason", "stop", "message", Map.of("content", content)))));
        // 用 OLLAMA 供应商是因为本地模型不要求 API Key，协议和云端模型一样。
        return new HttpDiagnosisModel(new ModelSettings(ModelProvider.OLLAMA, startStub(200, completion), "", "test-model", 0.2));
    }

    private OpsAlert stagingP3Alert() {
        return new OpsAlert("ALERT-MODEL-001", "payment-service", "staging", "P3",
                "支付服务错误率小幅波动", "预发环境支付回调偶发 timeout。", Instant.parse("2026-06-11T06:59:00Z"));
    }

    @Test void sendsJsonContractAndParsesResult() throws Exception {
        var c = client(200, "{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"content\":\"{\\\"summary\\\":\\\"待核验\\\"}\"}}]}");
        assertEquals("待核验", c.generate("只返回JSON", "证据").path("summary").asText());
        assertTrue(request.get().contains("json_object"));
        assertFalse(request.get().contains("test-only-key"));
    }

    @Test void httpErrorNeverReturnsFakeSuccessOrEchoesBody() throws Exception {
        var c = client(429, "sensitive-provider-body");
        assertEquals("MODEL_HTTP_429", assertThrows(IllegalStateException.class, () -> c.generate("JSON", "p")).getMessage());
    }

    @Test void rejectsTruncatedResponse() throws Exception {
        var c = client(200, "{\"choices\":[{\"finish_reason\":\"length\",\"message\":{\"content\":\"{}\"}}]}");
        assertEquals("MODEL_INCOMPLETE_RESPONSE", assertThrows(IllegalStateException.class, () -> c.generate("JSON", "p")).getMessage());
    }

    @Test void rejectsInvalidJson() throws Exception {
        var c = client(200, "not-json");
        assertEquals("MODEL_IO_OR_INVALID_JSON", assertThrows(IllegalStateException.class, () -> c.generate("JSON", "p")).getMessage());
    }

    @Test void backendRequiresApprovalEvenIfModelSaysNo() throws Exception {
        // 模型建议回滚，却声明“不需要审批”：后端按建议内容判定，仍然要求审批。
        var report = modelReturning("建议回滚到上一稳定版本", false)
                .generateReport(stagingP3Alert(), "Runbook", "日志证据：timeout");
        assertTrue(report.requiresApproval());
        assertEquals("ALERT-MODEL-001", report.alertId());
        assertTrue(report.evidence().contains("日志证据：timeout"));
    }

    @Test void backendSkipsApprovalForLowRiskAdviceEvenIfModelSaysYes() throws Exception {
        // 预发环境 P3，建议只是继续观察：模型声明“需要审批”也不算数，事件会停在 DIAGNOSED。
        var report = modelReturning("继续观察错误率和延迟", true)
                .generateReport(stagingP3Alert(), "Runbook", "日志证据：timeout");
        assertFalse(report.requiresApproval());
    }

    @Test void cloudModelRequiresKeyAndSettingsHideKey() {
        assertThrows(IllegalStateException.class, () -> new JsonModelClient("https://api.deepseek.com", "", "deepseek-v4-flash", false, Duration.ofSeconds(2)));
        var settings = new ModelSettings(ModelProvider.DEEPSEEK, "https://api.deepseek.com", "not-for-logs", "deepseek-v4-flash", 0.2);
        assertFalse(settings.toString().contains("not-for-logs"));
    }
}
