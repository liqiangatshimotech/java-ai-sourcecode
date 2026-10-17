package com.example.springaipersistentmemory;
import com.example.springaipersistentmemory.web.PersistentMemoryController;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.*;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import static org.assertj.core.api.Assertions.*;

/** 真实 HTTP、ChatClient、Advisor、JdbcChatMemoryRepository；只有远端模型由测试 Bean 替换。 */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"DEEPSEEK_API_KEY=test-key"})
@Import(JdbcMemoryHttpTest.ModelTestConfiguration.class)
class JdbcMemoryHttpTest {
    static final Path DB=createPath();
    static Path createPath() {
        try{return Files.createTempDirectory("chapter53-http-").resolve("memory");}
        catch(Exception ex){throw new IllegalStateException(ex);}
    }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("APP_DATASOURCE_URL",()->"jdbc:h2:file:"+DB+";MODE=PostgreSQL");
    }
    @TestConfiguration static class ModelTestConfiguration {
        @Bean @Primary RecordingModel recordingModel() {return new RecordingModel();}
    }
    static class RecordingModel implements ChatModel {
        final List<Prompt> requests=new ArrayList<>();
        @Override public ChatResponse call(Prompt prompt) {
            requests.add(prompt);
            return new ChatResponse(List.of(new Generation(new AssistantMessage("收到"))));
        }
    }
    @Autowired RecordingModel model;
    @Autowired TestRestTemplate http;
    @Autowired ChatMemory memory;
    @Autowired JdbcTemplate jdbc;
    @BeforeEach void reset() {
        jdbc.update("delete from SPRING_AI_CHAT_MEMORY");jdbc.update("delete from user_preference_memory");model.requests.clear();
    }
    void chat(String user,String conversation,String message) {
        var response=http.postForEntity("/api/persistent-memory-chat",
                Map.of("userId",user,"conversationId",conversation,"message",message),String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
    @Test void preferencesCrossConversationsButNeverUsersAndHistoryStaysScoped() {
        chat("a","one","以后回答用中文，别太长。");
        chat("a","two","我之前的偏好是什么");
        chat("b","two","我之前的偏好是什么");
        assertThat(model.requests.get(1).getContents()).contains("回答用中文","回答保持简短");
        assertThat(model.requests.get(2).getContents()).doesNotContain("回答用中文","回答保持简短");
        assertThat(memory.get(PersistentMemoryController.conversationKey("a","one"))).hasSize(2);
        assertThat(memory.get(PersistentMemoryController.conversationKey("a","two"))).hasSize(2);
    }
    @Test void jdbcWindowPrunesOldTurnsAndCanBeCleared() {
        for(int i=0;i<7;i++)chat("a","one","编号-"+i);
        String key=PersistentMemoryController.conversationKey("a","one");
        assertThat(memory.get(key)).hasSize(10);
        assertThat(memory.get(key).toString()).doesNotContain("编号-0","编号-1").contains("编号-6");
        memory.clear(key);assertThat(memory.get(key)).isEmpty();
    }
    @Test void invalidInputIsRejectedAndCompositeKeyFitsSchemaWithoutDelimiterCollision() {
        assertThat(http.postForEntity("/api/persistent-memory-chat",Map.of("userId","","conversationId","c","message","hi"),String.class).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(model.requests).isEmpty();
        assertThat(PersistentMemoryController.conversationKey("a:b","c")).hasSize(36)
                .isNotEqualTo(PersistentMemoryController.conversationKey("a","b:c"));
    }
}
