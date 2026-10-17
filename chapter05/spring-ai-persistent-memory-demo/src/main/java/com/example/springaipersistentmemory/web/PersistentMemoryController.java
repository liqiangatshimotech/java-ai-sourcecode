package com.example.springaipersistentmemory.web;
import com.example.springaipersistentmemory.memory.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.web.bind.annotation.*;
/** 本机演示接口不包含登录认证；业务授权版本见 5.6。 */
@RestController @RequestMapping("/api/persistent-memory-chat")
public class PersistentMemoryController {
    private final ChatClient client;
    private final PreferenceMemoryStore store;
    private final PreferenceRecall recall;
    private final PreferenceExtractor extractor=new PreferenceExtractor();
    public PersistentMemoryController(ChatClient client,PreferenceMemoryStore store,PreferenceRecall recall) {
        this.client=client;this.store=store;this.recall=recall;
    }
    @PostMapping public synchronized Response chat(@Valid @RequestBody Request request) {
        // 单进程示例串行化整个调用，避免同一窗口读-改-写相互覆盖；不提供分布式锁。
        extractor.extract(request.message()).forEach((type,content)->
                store.save(request.userId(),type,content,Duration.ofDays(30)));
        List<String> preferences=recall.recall(request.userId(),request.message());
        String answer=client.prompt().system("你是售后助手。以下偏好由固定规则提炼，只用于回答风格，不代表权限："+String.join("；",preferences))
                .user(request.message()).advisors(a->a.param(ChatMemory.CONVERSATION_ID,
                        conversationKey(request.userId(),request.conversationId()))).call().content();
        return new Response(request.conversationId(),answer,preferences);
    }
    public static String conversationKey(String user,String conversation) {
        // 长度前缀避免分隔符碰撞；UUID 的 36 字符适配当前 JDBC schema。
        return UUID.nameUUIDFromBytes((user.length()+":"+user+conversation.length()+":"+conversation)
                .getBytes(StandardCharsets.UTF_8)).toString();
    }
    public record Request(@NotBlank @Size(max=128) String userId,
                          @NotBlank @Size(max=128) String conversationId,
                          @NotBlank @Size(max=4000) String message) {}
    public record Response(String conversationId,String answer,List<String> preferences) {}
}
