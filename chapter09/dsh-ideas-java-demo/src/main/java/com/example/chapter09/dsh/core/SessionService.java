package com.example.chapter09.dsh.core;

import com.example.chapter09.dsh.event.SessionLog;
import com.example.chapter09.dsh.plugin.PluginContext;

/**
 * 会话服务：对应 ctx.sessions。
 *
 * 真实 dsh 里它背后有 jsonl / sqlite 两种持久化后端（并列的 Provider），
 * 这里保住核心行为：create 出来的每个 SessionLog 都是只追加事件流，
 * fork 支持从边界切出子会话。
 */
public final class SessionService {

    private final PluginContext ctx;

    public SessionService(PluginContext ctx) {
        this.ctx = ctx;
    }

    public SessionLog create(String sessionId) {
        return new SessionLog(sessionId, ctx);
    }
}
