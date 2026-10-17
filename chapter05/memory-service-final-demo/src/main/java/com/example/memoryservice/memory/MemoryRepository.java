package com.example.memoryservice.memory;
import java.time.Instant;
import java.util.*;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Repository;
/** 单进程演示存储；窗口、长期记忆、审计 History 分开保存，重启会清空。 */
@Repository
public class MemoryRepository {
    private final Map<String,List<Message>> windows=new HashMap<>();
    private final Map<String,List<Message>> histories=new HashMap<>();
    private final Map<String,List<MemoryRecord>> memories=new HashMap<>();
    public synchronized List<Message> window(MemoryScope scope) { return List.copyOf(windows.getOrDefault(scope.shortTermKey(),List.of())); }
    public synchronized void appendTurn(MemoryScope scope,Message user,Message assistant) {
        String key=scope.shortTermKey();
        var history=histories.computeIfAbsent(key,k->new ArrayList<>());
        history.add(user);history.add(assistant);
        var recent=new ArrayList<>(windows.getOrDefault(key,List.of()));
        recent.add(user);recent.add(assistant);
        windows.put(key,new ArrayList<>(recent.subList(Math.max(0,recent.size()-10),recent.size())));
    }
    public synchronized void save(MemoryScope scope,MemoryRecord record) {
        var list=memories.computeIfAbsent(scope.longTermKey(),k->new ArrayList<>());
        list.removeIf(old->old.type().equals(record.type())); // 同一类偏好更新，不无限追加重复记录。
        list.add(record);
    }
    public synchronized List<MemoryRecord> find(MemoryScope scope) { return List.copyOf(memories.getOrDefault(scope.longTermKey(),List.of())); }
    public synchronized void clearConversation(MemoryScope scope) { windows.remove(scope.shortTermKey()); }
    public synchronized void clearUser(MemoryScope scope) {
        String prefix=scope.longTermKey()+":";
        windows.keySet().removeIf(key->key.startsWith(prefix));
        memories.remove(scope.longTermKey());
        // History 属于独立审计生命周期，不会再次用于模型推理。
    }
    public synchronized int purgeExpired(Instant now) {
        int before=memories.values().stream().mapToInt(List::size).sum();
        memories.values().forEach(list->list.removeIf(record->!record.reusable(now)));
        memories.values().removeIf(List::isEmpty);
        return before-memories.values().stream().mapToInt(List::size).sum();
    }
    public synchronized int historySize(MemoryScope scope) { return histories.getOrDefault(scope.shortTermKey(),List.of()).size(); }
}
