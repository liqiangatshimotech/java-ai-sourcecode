package com.example.agentruntime.event;

import com.example.agentruntime.domain.AgentEvent;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
/** 每个运行独立事件日志。回放与注册共用锁，避免两者之间漏事件或重复发送。 */
public final class InMemoryAgentEventPublisher implements AgentEventPublisher {
    private static final class EventLog {
        final List<AgentEvent> events = new ArrayList<>();
        final Set<SseEmitter> subscribers = new HashSet<>();
    }
    private final Map<String, EventLog> logs = new ConcurrentHashMap<>();
    private EventLog log(String id) { return logs.computeIfAbsent(id, ignored -> new EventLog()); }
    public void publish(String runId, String type, String message) {
        EventLog log = log(runId);
        synchronized (log) {
            AgentEvent event = new AgentEvent(log.events.size() + 1L, runId, type, message, Instant.now());
            log.events.add(event);
            for (SseEmitter emitter : List.copyOf(log.subscribers)) {
                try { send(emitter, event); }
                catch (Exception disconnected) { log.subscribers.remove(emitter); emitter.complete(); }
            }
            if (terminal(type)) {
                List.copyOf(log.subscribers).forEach(SseEmitter::complete);
                log.subscribers.clear();
            }
        }
    }
    public List<AgentEvent> findByRunId(String runId) {
        EventLog log = log(runId);
        synchronized (log) { return List.copyOf(log.events); }
    }
    public SseEmitter subscribe(String runId) {
        EventLog log = log(runId);
        SseEmitter emitter = new SseEmitter(300_000L);
        Runnable remove = () -> { synchronized (log) { log.subscribers.remove(emitter); } };
        emitter.onCompletion(remove);
        emitter.onTimeout(() -> { remove.run(); emitter.complete(); });
        emitter.onError(ignored -> remove.run());
        synchronized (log) {
            try {
                for (AgentEvent event : log.events) send(emitter, event);
                if (!log.events.isEmpty() && terminal(log.events.getLast().type())) emitter.complete();
                else log.subscribers.add(emitter);
            } catch (Exception disconnected) { emitter.completeWithError(disconnected); }
        }
        return emitter;
    }
    private static boolean terminal(String status) {
        return Set.of("COMPLETED", "FAILED", "STOPPED").contains(status);
    }
    private static void send(SseEmitter emitter, AgentEvent event) throws java.io.IOException {
        emitter.send(SseEmitter.event().id(Long.toString(event.sequence())).name(event.type()).data(event));
    }
}
