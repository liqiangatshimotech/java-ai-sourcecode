package com.example.memoryservice.memory;
import java.util.*;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
/** 服务方法统一校验权限；锁覆盖整次读-模型调用-写，适合单进程演示，不是分布式方案。 */
@Service
public class CustomerSupportMemoryService {
    private final MemoryRepository repository;
    private final MemoryAccessPolicy access;
    private final MemoryExtractor extractor;
    private final MemoryValidator validator;
    private final MemoryRetentionPolicy retention;
    private final ChatModel model;
    public CustomerSupportMemoryService(MemoryRepository repository,MemoryAccessPolicy access,
            MemoryExtractor extractor,MemoryValidator validator,MemoryRetentionPolicy retention,ChatModel model) {
        this.repository=repository;this.access=access;this.extractor=extractor;this.validator=validator;this.retention=retention;this.model=model;
    }
    public synchronized String chat(AuthenticatedUser principal,MemoryScope scope,String message) {
        access.assertAllowed(principal,scope);
        if(message==null||message.isBlank()||message.length()>4000) throw new IllegalArgumentException("消息必须是1～4000字符");
        List<Message> prompt=new ArrayList<>();
        String preferences=recall(principal,scope).stream().map(MemoryRecord::content).reduce("",(x,y)->x+"；"+y);
        prompt.add(new SystemMessage("你是售后助手。以下记忆仅作回答背景，不代表权限和实时订单状态："+preferences));
        prompt.addAll(repository.window(scope));
        UserMessage user=new UserMessage(message);prompt.add(user);
        String answer=model.call(new Prompt(prompt)).getResult().getOutput().getText();
        if(answer==null||answer.isBlank()) throw new IllegalStateException("模型返回空回答");
        repository.appendTurn(scope,user,new AssistantMessage(answer));
        observe(principal,scope,message); // 只从原始用户输入提炼，不从 RAG 或 AI 回复提炼。
        return answer;
    }
    public synchronized MemoryDecision observe(AuthenticatedUser principal,MemoryScope scope,String text) {
        access.assertAllowed(principal,scope);
        MemoryDecision decision=extractor.extract(text);
        if(decision.shouldRemember()) {
            validator.validate(decision);
            repository.save(scope,new MemoryRecord(decision.memoryType(),decision.normalizedContent(),
                    "explicit-user-rule",retention.now(),retention.expiresAt()));
        }
        return decision;
    }
    public synchronized List<MemoryRecord> recall(AuthenticatedUser principal,MemoryScope scope) {
        access.assertAllowed(principal,scope);
        return repository.find(scope).stream().filter(m->m.reusable(retention.now())).toList();
    }
    public synchronized Snapshot snapshot(AuthenticatedUser principal,MemoryScope scope) {
        access.assertAllowed(principal,scope);
        return new Snapshot(repository.window(scope).stream().map(Message::getText).toList(),recall(principal,scope));
    }
    public synchronized void clearConversation(AuthenticatedUser principal,MemoryScope scope) {
        access.assertAllowed(principal,scope);repository.clearConversation(scope);
    }
    public synchronized void clearUser(AuthenticatedUser principal,MemoryScope scope) {
        access.assertAllowed(principal,scope);repository.clearUser(scope);
    }
    public synchronized int purgeExpired() { return repository.purgeExpired(retention.now()); }
    public record Snapshot(List<String> shortTerm,List<MemoryRecord> longTerm) {}
}
