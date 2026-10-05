package com.example.springairag.config;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/** 本机 HTTP 夹具验证真实工厂的兼容路径与三种配置，不调用外部模型。 */
class CourseChatModelFactoryTest {
    @Test void defaultsToDeepSeekV4FlashAndDoesNotPrintKey() {
        var settings=CourseChatSettings.from(Map.of("DEEPSEEK_API_KEY","test-only-secret"));
        assertEquals("deepseek",settings.provider());
        assertEquals("deepseek-v4-flash",settings.model());
        assertFalse(settings.toString().contains("test-only-secret"));
    }
    @Test void missingCloudKeyFailsBeforeNetworkCall() {
        assertThrows(IllegalStateException.class,()->CourseChatModelFactory.create(Map.of()));
    }
    @Test void rejectsUnknownProviderAndCredentialBearingUrl() {
        assertThrows(IllegalArgumentException.class,()->CourseChatSettings.from(Map.of("AI_PROVIDER","unknown")));
        assertThrows(IllegalArgumentException.class,()->CourseChatSettings.from(Map.of("AI_BASE_URL","https://user:secret@example.com/v1")));
    }
    @ParameterizedTest @ValueSource(strings={"deepseek","ollama","openai","chatgpt"})
    void realFactoryUsesConfiguredCompatibleEndpoint(String provider) throws Exception {
        AtomicReference<String> received=new AtomicReference<>();
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/v1/chat/completions",exchange->{
            received.set(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));
            byte[] response="{\"id\":\"fixture\",\"object\":\"chat.completion\",\"created\":1,\"model\":\"fixture-model\",\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\",\"content\":\"fixture answer\"},\"finish_reason\":\"stop\"}],\"usage\":{\"prompt_tokens\":1,\"completion_tokens\":1,\"total_tokens\":2}}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type","application/json");
            exchange.sendResponseHeaders(200,response.length);exchange.getResponseBody().write(response);exchange.close();
        });server.start();
        try {
            Map<String,String> env=Map.of("AI_PROVIDER",provider,"AI_BASE_URL","http://127.0.0.1:"+server.getAddress().getPort(),"AI_API_KEY","test-only-key","AI_MODEL","fixture-model");
            String answer=CourseChatModelFactory.create(env).call(new org.springframework.ai.chat.prompt.Prompt("hello")).getResult().getOutput().getText();
            assertEquals("fixture answer",answer);
            assertTrue(received.get().contains("fixture-model"));
            assertFalse(received.get().contains("test-only-key"));
        } finally {server.stop(0);}
    }
}
