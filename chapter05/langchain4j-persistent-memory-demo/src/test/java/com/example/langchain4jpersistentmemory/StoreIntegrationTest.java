package com.example.langchain4jpersistentmemory;
import com.example.langchain4jpersistentmemory.assistant.*;
import com.example.langchain4jpersistentmemory.rag.RagMemoryBoundary;
import com.example.langchain4jpersistentmemory.store.JdbcChatMemoryStore;
import dev.langchain4j.data.message.*;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.*;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.service.AiServices;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.assertj.core.api.Assertions.*;

/** 真实 H2 + ChatMemoryStore + AI Service + RAG，对事务、窗口和恢复做行为验证。 */
class StoreIntegrationTest {
    @TempDir Path dir;
    JdbcTemplate jdbc;
    JdbcChatMemoryStore store;
    @BeforeEach void setup() {
        jdbc=jdbc("jdbc:h2:file:"+dir.resolve("memory"));store=new JdbcChatMemoryStore(jdbc);
    }
    static JdbcTemplate jdbc(String url) {
        var jdbc=new JdbcTemplate(new DriverManagerDataSource(url,"sa",""));
        jdbc.execute("create table if not exists ai_chat_memory(memory_id varchar(128) primary key,messages_json clob not null)");
        return jdbc;
    }
    static class RecordingModel implements ChatModel {
        final List<List<ChatMessage>> requests=new ArrayList<>();
        @Override public ChatResponse doChat(ChatRequest request) {
            requests.add(List.copyOf(request.messages()));
            return ChatResponse.builder().aiMessage(AiMessage.from("回答可能转述退款政策")).build();
        }
    }
    @Test void newProcessCanReadCommittedMessages() throws Exception {
        store.updateMessages("restart",List.of(UserMessage.from("persisted")));
        String javaExecutable=Path.of(System.getProperty("java.home"),"bin","java").toString();
        Process process=new ProcessBuilder(javaExecutable,"-cp",System.getProperty("java.class.path"),
                PersistenceProbe.class.getName(),"jdbc:h2:file:"+dir.resolve("memory")).redirectErrorStream(true).start();
        assertThat(process.waitFor(30,TimeUnit.SECONDS)).isTrue();
        String output=new String(process.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
        assertThat(process.exitValue()).isZero();assertThat(output).contains("persisted");
    }
    @Test void failedInsertRollsBackDelete() {
        store.updateMessages("a",List.of(UserMessage.from("old")));
        jdbc.execute("alter table ai_chat_memory add constraint limit_json check (length(messages_json)<1000)");
        assertThatThrownBy(()->store.updateMessages("a",List.of(UserMessage.from("x".repeat(2000))))).isInstanceOf(RuntimeException.class);
        assertThat(store.getMessages("a").toString()).contains("old");
    }
    @Test void realWindowPrunesStoreAndReloadsAcrossServiceInstances() {
        var model=new RecordingModel();
        var first=new PersistentAssistantFactory().create(model,store);
        for(int i=0;i<12;i++)first.chat("a","input-"+i);
        assertThat(store.getMessages("a")).hasSize(20);
        assertThat(store.getMessages("a").stream().filter(UserMessage.class::isInstance).map(m->((UserMessage)m).singleText()))
                .doesNotContain("input-0","input-1");
        new PersistentAssistantFactory().create(model,new JdbcChatMemoryStore(jdbc)).chat("a","next");
        assertThat(model.requests.getLast().toString()).contains("input-11");
        assertThat(store.getMessages("b")).isEmpty();
    }
    @ParameterizedTest @ValueSource(booleans={false,true})
    void ragFlagControlsStoredUserMessageButStillSendsRetrievedContent(boolean save) {
        var model=new RecordingModel();var assistant=RagMemoryBoundary.create(model,store,save);
        assistant.chat("a","退款一般多久到账？");
        assertThat(model.requests.getFirst().toString()).contains("3 个工作日");
        UserMessage user=(UserMessage)store.getMessages("a").stream().filter(UserMessage.class::isInstance).findFirst().orElseThrow();
        if(save)assertThat(user.singleText()).contains("3 个工作日");
        else assertThat(user.singleText()).isEqualTo("退款一般多久到账？");
        // 关闭开关不等于清除 AI 回答中的知识内容。
        assertThat(store.getMessages("a").toString()).contains("回答可能转述退款政策");
    }
    @Test void frameworkDefaultIsTrue() {
        var model=new RecordingModel();
        var assistant=AiServices.builder(Assistant.class).chatModel(model)
                .chatMemoryProvider(id->MessageWindowChatMemory.builder().id(id).maxMessages(20).chatMemoryStore(store).build())
                .retrievalAugmentor(DefaultRetrievalAugmentor.builder().contentRetriever(q->List.of(Content.from("retrieved-marker"))).build()).build();
        assistant.chat("default","question");
        assertThat(((UserMessage)store.getMessages("default").getFirst()).singleText()).contains("retrieved-marker");
    }
    @Test void clearDeletesOnlyTargetIdAndRejectsInvalidKey() {
        store.updateMessages("a",List.of(UserMessage.from("A")));
        store.updateMessages("b",List.of(UserMessage.from("B")));
        MessageWindowChatMemory.builder().id("a").maxMessages(20).chatMemoryStore(store).build().clear();
        assertThat(store.getMessages("a")).isEmpty();assertThat(store.getMessages("b")).hasSize(1);
        assertThatThrownBy(()->store.getMessages(1)).isInstanceOf(IllegalArgumentException.class);
    }
}
