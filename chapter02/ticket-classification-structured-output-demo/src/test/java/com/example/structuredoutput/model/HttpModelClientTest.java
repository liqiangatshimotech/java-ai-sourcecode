package com.example.structuredoutput.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;

/** 用本机 HTTP 服务验证供应商路径、鉴权头、请求 JSON 和响应解析，不花费模型额度。 */
class HttpModelClientTest {
    @ParameterizedTest @ValueSource(strings = {"deepseek", "chatgpt", "ollama"})
    void sendsProviderRequestAndReadsResponse(String provider) throws Exception {
        AtomicReference<String> path = new AtomicReference<>();
        AtomicReference<String> auth = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            path.set(exchange.getRequestURI().getPath());
            auth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String result = provider.equals("ollama")
                    ? "{\"message\":{\"content\":\"模型文本\"}}"
                    : "{\"choices\":[{\"message\":{\"content\":\"模型文本\"}}]}";
            byte[] bytes = result.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        try {
            String prefix = provider.toUpperCase(java.util.Locale.ROOT);
            Map<String, String> config = Map.of("APP_AI_DEFAULT_PROVIDER", provider,
                    prefix + "_API_KEY", "test-key", prefix + "_MODEL", "test-model",
                    prefix + "_BASE_URL", "http://127.0.0.1:" + server.getAddress().getPort()
                            + (provider.equals("ollama") ? "/" : "/v1/"));
            assertThat(new HttpModelClient(config::get).generate("引号\"和换行\n")).isEqualTo("模型文本");
            assertThat(path.get()).isEqualTo(provider.equals("ollama") ? "/api/chat" : "/v1/chat/completions");
            assertThat(auth.get()).isEqualTo(provider.equals("ollama") ? null : "Bearer test-key");
            var json = new ObjectMapper().readTree(body.get());
            assertThat(json.path("model").asText()).isEqualTo("test-model");
            assertThat(json.path("messages").get(0).path("content").asText()).isEqualTo("引号\"和换行\n");
            assertThat(json.path("stream").asBoolean()).isFalse();
        } finally { server.stop(0); }
    }

    @ParameterizedTest @ValueSource(strings = {"status", "missing", "malformed"})
    void responseFailuresDoNotExposeRawBody(String failure) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            byte[] body = (failure.equals("missing") ? "{}" : "sensitive-provider-response")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(failure.equals("status") ? 503 : 200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            Map<String, String> config = Map.of("DEEPSEEK_API_KEY", "test-key",
                    "DEEPSEEK_BASE_URL", "http://127.0.0.1:" + server.getAddress().getPort());
            assertThatThrownBy(() -> new HttpModelClient(config::get).generate("hello"))
                    .isInstanceOf(IllegalStateException.class).hasMessageNotContaining("sensitive-provider-response");
        } finally { server.stop(0); }
    }

    @Test void defaultDeepseekRequiresKeyAndUnknownProviderFailsFast() {
        assertThatThrownBy(() -> new HttpModelClient(key -> null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("DEEPSEEK_API_KEY");
        assertThatThrownBy(() -> new HttpModelClient(Map.of("APP_AI_DEFAULT_PROVIDER", "unknown")::get))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
