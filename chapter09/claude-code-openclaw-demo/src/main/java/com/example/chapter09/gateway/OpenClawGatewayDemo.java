package com.example.chapter09.gateway;

import com.example.chapter09.ModelSettings;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class OpenClawGatewayDemo {

    private final ModelSettings settings;
    private final SessionRegistry sessions = new SessionRegistry();
    private final ToolRegistry tools = new ToolRegistry();

    public OpenClawGatewayDemo(ModelSettings settings) {
        this.settings = Objects.requireNonNull(settings, "settings");
        // Gateway 本身不写死业务能力，只把工具注册进 ToolRegistry。
        // 后续换成真实代码审查、Git 操作、PR 创建工具时，Runtime 的调用方式不需要变化。
        tools.register("summarizeContext", args ->
                "会话 " + args.get("sessionId") + " 已统一到 Gateway，入口只做标准化，Runtime 只做执行。");
        tools.register("draftReply", args ->
                "回复草稿：多入口消息已经归到同一个 session，后续输出会回到原渠道。");
    }

    public void run() {
        System.out.println("[2] OpenClaw gateway");
        System.out.println("Model profile: " + settings.summary());

        // 三条消息模拟从不同入口进入：Telegram、Web 控制台、CLI。
        // 它们的 conversationRef 相同，所以应该归并到同一个 AgentSession。
        List<InboundMessage> messages = List.of(
                new InboundMessage("telegram", "user-1001", "repo-42", "agent-code", "tg:1001",
                        "请检查这个仓库的源码分层"),
                new InboundMessage("web", "user-1001", "repo-42", "agent-code", "web:panel-7",
                        "把上一步的结论整理给我"),
                new InboundMessage("cli", "user-1001", "repo-42", "agent-code", "cli:stdout",
                        "继续，顺便给出一个简短回复"));

        for (InboundMessage message : messages) {
            AgentTurn turn = route(message);
            AgentReply reply = runTurn(turn);
            deliver(message, reply);
        }

        System.out.println("Session traces:");
        for (AgentSession session : sessions.all()) {
            System.out.println("- " + session.sessionId() + " / " + session.sessionKey());
            for (String trace : session.traces()) {
                System.out.println("    " + trace);
            }
        }
    }

    private AgentTurn route(InboundMessage message) {
        // OpenClaw 式 Gateway 的关键不是“哪个渠道先进来”，而是把消息映射到稳定会话键。
        // agentId 表示要调用哪个 Agent，conversationRef 表示业务上下文，例如某个仓库或某个工单。
        String sessionKey = message.agentId() + ":" + message.conversationRef();
        AgentSession session = sessions.findOrCreate(sessionKey, message.agentId(), message.conversationRef());
        session.append("gateway: " + message.channel() + " -> " + message.replyTarget());
        return new AgentTurn(session, message, Instant.now(), session.nextTurnNo());
    }

    private AgentReply runTurn(AgentTurn turn) {
        AgentSession session = turn.session();
        session.append("runtime: start turn " + turn.turnNo());

        // demo 用简单规则代替大模型规划器。真实系统里，这里通常是模型根据上下文选择工具。
        String toolName = chooseTool(turn.message().text());
        session.append("planner: choose " + toolName);

        // 工具只接收与执行相关的结构化参数，不关心消息来自 Telegram 还是 CLI。
        Map<String, String> args = Map.of(
                "sessionId", session.sessionId(),
                "conversationRef", turn.message().conversationRef(),
                "provider", settings.provider().id());
        String toolResult = tools.call(toolName, args);
        session.append("tool:" + toolName + " -> " + toolResult);

        String replyText = "[" + settings.provider().id() + "] " + toolResult;
        session.append("reply: " + replyText);
        return new AgentReply(session.sessionId(), replyText);
    }

    private String chooseTool(String text) {
        String normalized = text.toLowerCase(Locale.ROOT);
        if (normalized.contains("检查") || normalized.contains("总结") || normalized.contains("结构")) {
            return "summarizeContext";
        }
        return "draftReply";
    }

    private void deliver(InboundMessage message, AgentReply reply) {
        // 输出阶段再回到原始渠道。Runtime 不需要知道 Telegram API 或 WebSocket API 的细节。
        System.out.println("  [" + message.channel() + " -> " + message.replyTarget() + "] " + reply.text());
    }

    public record InboundMessage(String channel,
                                 String accountId,
                                 String conversationRef,
                                 String agentId,
                                 String replyTarget,
                                 String text) {
    }

    public record AgentTurn(AgentSession session,
                            InboundMessage message,
                            Instant receivedAt,
                            int turnNo) {
    }

    public record AgentReply(String sessionId, String text) {
    }

    interface ToolHandler {
        String apply(Map<String, String> args);
    }

    static final class ToolRegistry {
        private final Map<String, ToolHandler> handlers = new LinkedHashMap<>();

        void register(String name, ToolHandler handler) {
            handlers.put(name, handler);
        }

        String call(String name, Map<String, String> args) {
            ToolHandler handler = handlers.get(name);
            if (handler == null) {
                // 生产环境不要静默吞掉未知工具，可以转成审批事件或告警。
                return "未知工具: " + name;
            }
            return handler.apply(args);
        }
    }

    static final class SessionRegistry {
        private final Map<String, AgentSession> sessions = new LinkedHashMap<>();

        AgentSession findOrCreate(String sessionKey, String agentId, String conversationRef) {
            // computeIfAbsent 保证同一个 sessionKey 只会创建一次会话对象。
            // 换成 Redis 或数据库时，也需要保持这个“查找或创建”的原子语义。
            return sessions.computeIfAbsent(sessionKey,
                    key -> new AgentSession(key, agentId, conversationRef));
        }

        List<AgentSession> all() {
            return List.copyOf(sessions.values());
        }
    }

    static final class AgentSession {
        private final String sessionId;
        private final String sessionKey;
        private final String agentId;
        private final String conversationRef;
        private final List<String> traces = new ArrayList<>();
        private int turnCounter;

        AgentSession(String sessionKey, String agentId, String conversationRef) {
            this.sessionKey = sessionKey;
            this.agentId = agentId;
            this.conversationRef = conversationRef;
            // demo 用 hash 生成短 ID，便于阅读输出；生产环境应使用 UUID/ULID 或数据库主键。
            this.sessionId = "sess-" + Integer.toHexString(sessionKey.hashCode()).toUpperCase(Locale.ROOT);
        }

        int nextTurnNo() {
            return ++turnCounter;
        }

        void append(String trace) {
            traces.add(trace);
        }

        String sessionId() {
            return sessionId;
        }

        String sessionKey() {
            return sessionKey;
        }

        List<String> traces() {
            return List.copyOf(traces);
        }

        @SuppressWarnings("unused")
        String agentId() {
            return agentId;
        }

        @SuppressWarnings("unused")
        String conversationRef() {
            return conversationRef;
        }
    }
}
