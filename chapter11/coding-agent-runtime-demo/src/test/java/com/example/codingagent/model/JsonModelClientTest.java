package com.example.codingagent.model;

import com.sun.net.httpserver.HttpServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.codingagent.config.ModelSettings;
import com.example.codingagent.domain.CodingTask;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/** 只访问本机假 HTTP 服务，验证协议契约；不把这些用例当成真实模型质量测试。 */
class JsonModelClientTest {
    private HttpServer server;
    private String base;
    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicReference<String> response = new AtomicReference<>();
    private final AtomicReference<String> request = new AtomicReference<>();
    private volatile long delay;
    private volatile int status = 200;

    @BeforeEach void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            request.set(new String(exchange.getRequestBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
            try { if (delay > 0) Thread.sleep(delay); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
            byte[] body = response.get().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            try { exchange.sendResponseHeaders(status, body.length); exchange.getResponseBody().write(body); }
            finally { exchange.close(); }
        });
        server.start();
        base = "http://127.0.0.1:" + server.getAddress().getPort();
        response.set(envelope("{\"targetPath\":\"a.java\",\"rationale\":\"业务修复\",\"newContent\":\"class A {}\"}", "stop", null));
    }
    @AfterEach void stop() { server.stop(0); }
    private String envelope(String content, String reason, String refusal) throws Exception {
        var message = new java.util.HashMap<String,Object>(); message.put("content",content); if(refusal!=null)message.put("refusal",refusal);
        return mapper.writeValueAsString(Map.of("choices", java.util.List.of(Map.of("finish_reason",reason,"message",message))));
    }
    private JsonModelClient client(Duration timeout) { return new JsonModelClient(base,"test-only-token","unit-test",true,timeout); }
    @Test void acceptsObjectAndSendsExpectedJson() throws Exception {
        assertEquals("a.java", client(Duration.ofSeconds(2)).generate("system", "task").path("targetPath").asText());
        var body=mapper.readTree(request.get()); assertEquals("json_object",body.path("response_format").path("type").asText());
        assertFalse(request.get().contains("test-only-token"));
    }
    @Test void rejectsTruncation() throws Exception {
        response.set(envelope("{}","length",null));
        assertEquals("MODEL_INCOMPLETE_RESPONSE",assertThrows(IllegalStateException.class,()->client(Duration.ofSeconds(2)).generate("s","p")).getMessage());
    }
    @Test void rejectsRefusal() throws Exception {
        response.set(envelope("{}","stop","cannot comply"));
        assertEquals("MODEL_REFUSAL",assertThrows(IllegalStateException.class,()->client(Duration.ofSeconds(2)).generate("s","p")).getMessage());
    }
    @Test void rejectsMarkdownInsteadOfJson() throws Exception {
        response.set(envelope("```json\n{}\n```","stop",null));
        assertThrows(IllegalStateException.class,()->client(Duration.ofSeconds(2)).generate("s","p"));
    }
    @Test void rejectsArrayResponse() throws Exception {
        response.set(envelope("[]","stop",null));
        assertEquals("MODEL_EXPECTED_JSON_OBJECT",assertThrows(IllegalStateException.class,()->client(Duration.ofSeconds(2)).generate("s","p")).getMessage());
    }
    @Test void doesNotEchoProviderErrorBody() {
        status=401;response.set("sensitive-provider-body");
        String error=assertThrows(IllegalStateException.class,()->client(Duration.ofSeconds(2)).generate("s","p")).getMessage();
        assertEquals("MODEL_HTTP_401",error);
    }
    @Test void appliesDeadline() {
        delay=200;
        assertEquals("MODEL_TIMEOUT",assertThrows(IllegalStateException.class,()->client(Duration.ofMillis(30)).generate("s","p")).getMessage());
    }
    @Test void stopsOversizedResponseDuringReception() {
        response.set("x".repeat(1_000_001));
        assertThrows(IllegalStateException.class,()->client(Duration.ofSeconds(2)).generate("s","p"));
    }
    @Test void rejectsModelChangingTargetPath() {
        var model=new HttpCodingModel("test-only-token");
        var settings=ModelSettings.from(Map.of("AI_PROVIDER","deepseek","AI_BASE_URL",base));
        assertEquals("MODEL_TARGET_PATH_CHANGED",assertThrows(IllegalStateException.class,()->model.proposeChange(new CodingTask("t","fix","u"),"expected.java","class A {}",settings)).getMessage());
    }
    @Test void malformedFieldsAreNotAccepted() throws Exception {
        assertThrows(IllegalStateException.class,()->JsonModelClient.requiredText(mapper.readTree("{\"targetPath\":42}"),"targetPath",100));
    }
}
