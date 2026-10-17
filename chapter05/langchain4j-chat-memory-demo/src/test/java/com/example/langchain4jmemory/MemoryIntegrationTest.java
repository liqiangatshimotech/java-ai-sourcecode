package com.example.langchain4jmemory;
import com.example.langchain4jmemory.assistant.*;
import com.example.langchain4jmemory.config.ModelFactory;
import dev.langchain4j.data.message.*;
import dev.langchain4j.memory.chat.*;
import dev.langchain4j.model.chat.*;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;

/** 框架是真实 AI Services；只有外部模型被替换为可检查请求的测试实现。 */
class MemoryIntegrationTest {
    @Test void realAiServiceKeepsIdsIsolatedAndCachesWindows() {
        List<List<ChatMessage>> requests=new ArrayList<>();
        ChatModel model=new ChatModel() {
            @Override public ChatResponse doChat(ChatRequest request) {
                requests.add(List.copyOf(request.messages()));
                return ChatResponse.builder().aiMessage(AiMessage.from("收到")).build();
            }
        };
        Assistant assistant=new AssistantFactory().create(model);
        assistant.chat("a","负责华东区");
        assistant.chat("b","负责华南区");
        assistant.chat("a","我在哪个区域");
        assertThat(requests.get(2).toString()).contains("华东区","收到").doesNotContain("华南区");
        assertThat(requests.get(1).toString()).doesNotContain("华东区");
    }
    @Test void messageWindowEvictsOldMessagesAndPreservesSystemMessage() {
        var memory=MessageWindowChatMemory.withMaxMessages(4);
        memory.add(SystemMessage.from("system"));
        for(int i=0;i<5;i++)memory.add(UserMessage.from("message-"+i));
        assertThat(memory.messages()).hasSize(4);
        assertThat(memory.messages().get(0)).isInstanceOf(SystemMessage.class);
        assertThat(memory.messages().toString()).doesNotContain("message-0","message-1").contains("message-4");
    }
    @Test void actualTokenEstimatorBoundsActualFrameworkWindow() {
        var estimator=new OpenAiTokenCountEstimator("gpt-4o-mini");
        var memory=TokenWindowChatMemory.builder().id("x").maxTokens(80,estimator).build();
        memory.add(UserMessage.from("old"));
        memory.add(UserMessage.from("很长的消息".repeat(100)));
        memory.add(UserMessage.from("new"));
        assertThat(estimator.estimateTokenCountInMessages(memory.messages())).isLessThanOrEqualTo(80);
        assertThat(memory.messages().toString()).contains("new").doesNotContain("很长的消息");
    }
    @ParameterizedTest @ValueSource(strings={"deepseek","ollama","chatgpt"})
    void providerFactoryUsesRealOpenAiCompatibleHttp(String provider) throws Exception {
        AtomicReference<String> path=new AtomicReference<>(),body=new AtomicReference<>();
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/",exchange->{
            path.set(exchange.getRequestURI().getPath());
            body.set(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));
            byte[] bytes="{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"ok\"},\"finish_reason\":\"stop\"}]}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type","application/json");exchange.sendResponseHeaders(200,bytes.length);
            exchange.getResponseBody().write(bytes);exchange.close();
        });server.start();
        try {
            String prefix=provider.toUpperCase(Locale.ROOT);
            var config=Map.of("APP_AI_DEFAULT_PROVIDER",provider,prefix+"_API_KEY","test-key",prefix+"_MODEL","test-model",prefix+"_BASE_URL","http://127.0.0.1:"+server.getAddress().getPort()+"/v1/");
            assertThat(ModelFactory.create(config::get).chat("hello")).isEqualTo("ok");
            assertThat(path.get()).isEqualTo("/v1/chat/completions");
            assertThat(body.get()).contains("test-model","hello");
        } finally {server.stop(0);}
    }
    @Test void missingKeyAndInvalidProviderFailClearly() {
        assertThatThrownBy(()->ModelFactory.create(k->null)).hasMessageContaining("DEEPSEEK_API_KEY");
        assertThatThrownBy(()->ModelFactory.create(Map.of("APP_AI_DEFAULT_PROVIDER","bad")::get)).isInstanceOf(IllegalArgumentException.class);
    }
}
