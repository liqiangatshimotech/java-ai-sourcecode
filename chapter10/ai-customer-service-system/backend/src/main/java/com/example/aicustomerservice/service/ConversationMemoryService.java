package com.example.aicustomerservice.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话记忆服务。
 *
 * 这里保存的是客服会话摘要，不保存无限长的聊天全文。生产系统要避免把所有历史消息
 * 原样塞进模型上下文，否则成本和延迟都会失控。
 */
public class ConversationMemoryService {

    private final Map<String, List<String>> summaries = new java.util.LinkedHashMap<>(16, 0.75f, true);

    public synchronized void append(String sessionId, String question, String answer) {
        List<String> sessionSummaries = summaries.computeIfAbsent(sessionId, ignored -> new ArrayList<>());
        sessionSummaries.add(Instant.now() + " Q: " + bounded(question) + " A: " + bounded(answer));
        // 限制活跃会话数量，过期会话由业务消息存储重建，避免长运行进程无限增长。
        while (summaries.size() > 1000) summaries.remove(summaries.keySet().iterator().next());
        if (sessionSummaries.size() > 5) {
            sessionSummaries.remove(0);
        }
    }

    /** 每段只保留 800 字；手机号脱敏，历史最多 5 段，不接纳坐席内部备注。 */
    private String bounded(String text) {
        String safe = text.replaceAll("(?<![0-9])1[3-9][0-9]{9}(?![0-9])", "[手机号已隐藏]");
        return safe.substring(0, Math.min(800, safe.length()));
    }

    public synchronized List<String> recentSummaries(String sessionId) {
        return List.copyOf(summaries.getOrDefault(sessionId, List.of()));
    }
}
