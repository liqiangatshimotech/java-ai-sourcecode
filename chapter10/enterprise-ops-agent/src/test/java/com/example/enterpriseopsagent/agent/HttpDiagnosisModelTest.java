package com.example.enterpriseopsagent.agent;

import com.example.enterpriseopsagent.config.*;
import com.example.enterpriseopsagent.domain.OpsAlert;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

/** 本地 HTTP 替身校验真实协议和错误路径，不消耗供应商费用。 */
class HttpDiagnosisModelTest {
    private HttpServer server;
    private final AtomicReference<String> request = new AtomicReference<>();
    @AfterEach void stop() { if (server != null) server.stop(0); }

    private JsonModelClient client(int status, String response) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", e -> {
            request.set(new String(e.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            e.getResponseHeaders().set("Content-Type", "application/json");
            e.sendResponseHeaders(status, bytes.length);
            e.getResponseBody().write(bytes); e.close();
        });
        server.start();
        return new JsonModelClient("http://127.0.0.1:" + server.getAddress().getPort(), "test-only-key",
                "test-model", false, Duration.ofSeconds(2));
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

    @Test void cloudModelRequiresKeyAndSettingsHideKey() {
        assertThrows(IllegalStateException.class, () -> new JsonModelClient("https://api.deepseek.com", "", "deepseek-v4-flash", false, Duration.ofSeconds(2)));
        var settings = new ModelSettings(ModelProvider.DEEPSEEK, "https://api.deepseek.com", "not-for-logs", "deepseek-v4-flash", 0.2);
        assertFalse(settings.toString().contains("not-for-logs"));
    }
}
