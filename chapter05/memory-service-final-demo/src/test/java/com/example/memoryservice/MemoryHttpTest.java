package com.example.memoryservice;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.*;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import static org.assertj.core.api.Assertions.*;
/** 启动真实安全过滤器和 HTTP Controller；模型 Bean 用无网络测试实现。 */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties={"DEEPSEEK_API_KEY=test-key","MEMORY_USER_A_PASSWORD=test-a","MEMORY_USER_B_PASSWORD=test-b"})
@Import(MemoryHttpTest.ModelTestConfiguration.class)
class MemoryHttpTest {
    @Autowired TestRestTemplate http;
    @TestConfiguration static class ModelTestConfiguration {
        @Bean @Primary ChatModel testModel() {
            return prompt->new ChatResponse(List.of(new Generation(new AssistantMessage("收到"))));
        }
    }
    @Test void authenticationIsRequiredAndCallerCannotForgeIdentity() {
        assertThat(http.getForEntity("/api/memory/c",String.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(http.withBasicAuth("user-a","wrong").getForEntity("/api/memory/c",String.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        var a=http.withBasicAuth("user-a","test-a");
        var b=http.withBasicAuth("user-b","test-b");
        assertThat(a.postForEntity("/api/memory/http-test/chat",Map.of("message","以后回答用中文"),String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(a.postForEntity("/api/memory/http-test/chat",Map.of("message","hello","userId","user-b"),String.class).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(b.getForEntity("/api/memory/http-test",String.class).getBody()).doesNotContain("回答用中文");
        assertThat(a.getForEntity("/api/memory/http-test",String.class).getBody()).contains("回答用中文");
        assertThat(a.exchange("/api/memory",HttpMethod.DELETE,null,String.class).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(a.getForEntity("/api/memory/http-test",String.class).getBody()).doesNotContain("回答用中文");
    }
}
