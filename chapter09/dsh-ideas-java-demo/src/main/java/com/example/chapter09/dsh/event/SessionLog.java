package com.example.chapter09.dsh.event;

import com.example.chapter09.dsh.agent.Message;
import com.example.chapter09.dsh.plugin.PluginContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 会话日志：只追加的事件流，对应 dsh 的 core/session 包（ctx.sessions）。
 *
 * 两条设计纪律直接抄自 dsh：
 * 1. “模型可见即已记录”——AgentLoop 不允许凭空拼模型输入，
 *    只能调 deriveMessages() 从事件流投影；
 * 2. 压缩不是删除历史，而是追加一条 compaction 事件，
 *    投影时把边界之前的内容折叠成一条摘要消息（对应 dsh-compaction-basic）。
 */
public final class SessionLog {

    private final String sessionId;
    private final List<SessionEvent> events = new ArrayList<>();
    private final PluginContext ctx;
    private long seq;

    public SessionLog(String sessionId, PluginContext ctx) {
        this.sessionId = sessionId;
        this.ctx = ctx;
    }

    public String sessionId() {
        return sessionId;
    }

    /** 追加一条事件：赋全局序号、广播 session/event、返回不可变事件。 */
    public synchronized SessionEvent append(String type, Map<String, Object> payload) {
        SessionEvent event = new SessionEvent(++seq, type, payload);
        events.add(event);
        // session/event 是持久事实的广播口：想看实时 transcript 就订阅它。
        ctx.bus().emit("session/event", event);
        return event;
    }

    public synchronized List<SessionEvent> events() {
        return new ArrayList<>(events);
    }

    public synchronized long lastSeq() {
        return seq;
    }

    /**
     * 投影：从事件流推导模型消息历史。
     *
     * 只有这几类事件能变成模型输入：user/message、assistant/message、
     * tool/result。compaction 之后，边界之前的事件整体折叠成一条摘要，
     * 原始事件仍留在日志里，随时可以审计回放。
     */
    public synchronized List<Message> deriveMessages() {
        long compactionBoundary = 0;
        String summary = null;
        for (SessionEvent event : events) {
            if ("session/compacted".equals(event.type())) {
                compactionBoundary = event.seq();
                summary = String.valueOf(event.payload().get("summary"));
            }
        }
        List<Message> messages = new ArrayList<>();
        if (summary != null) {
            messages.add(new Message("user", "[ Earlier conversation compacted ] " + summary));
        }
        for (SessionEvent event : events) {
            if (event.seq() <= compactionBoundary) {
                continue;
            }
            switch (event.type()) {
                case "user/message" -> messages.add(
                        new Message("user", String.valueOf(event.payload().get("text"))));
                case "assistant/message" -> {
                    // dsh 语义：空内容的 assistant 消息（例如纯工具调用回合）
                    // 记录在日志里保留用量，但不进派生历史。
                    String text = String.valueOf(event.payload().get("text"));
                    if (!text.isEmpty()) {
                        messages.add(new Message("assistant", text));
                    }
                }
                case "tool/result" -> messages.add(new Message("user",
                        "[tool " + event.payload().get("tool") + "] " + event.payload().get("output")));
                default -> {
                    // 其他事件（chunk、todo/write、fs/observed...）对模型不可见，
                    // 这正是“投影”的意义：记录得多，喂给模型的少。
                }
            }
        }
        return messages;
    }

    /**
     * 压缩：追加一条 session/compacted 事件而不是删改历史。
     * 真实 dsh 里这一步由 dsh-compaction-basic 插件在 agent/pre-step
     * 的窗口压力分支里触发。
     */
    public synchronized void compact(String summary) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("summary", summary);
        append("session/compacted", payload);
    }

    /**
     * 分叉：从 boundary 序号切出一个子会话日志，对应 ctx.sessions.fork。
 * 用途是委派子 Agent——子会话继承边界之前的全部事实，
     * 之后父子各自追加互不影响。
     */
    public static SessionLog fork(SessionLog source, long boundary, String childSessionId) {
        SessionLog child = new SessionLog(childSessionId, source.ctx);
        synchronized (source) {
            for (SessionEvent event : source.events) {
                if (event.seq() > boundary) {
                    break;
                }
                child.append(event.type(), new LinkedHashMap<>(event.payload()));
            }
        }
        return child;
    }
}
