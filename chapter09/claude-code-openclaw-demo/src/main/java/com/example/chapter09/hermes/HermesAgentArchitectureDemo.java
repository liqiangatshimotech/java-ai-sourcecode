package com.example.chapter09.hermes;

import com.example.chapter09.ModelSettings;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class HermesAgentArchitectureDemo {

    private final ModelSettings settings;
    private final HermesSessionStore sessions = new HermesSessionStore();
    private final HermesToolRegistry tools = new HermesToolRegistry();
    private final HermesSkillCatalog skills = new HermesSkillCatalog();
    private final HermesMemory memory = new HermesMemory();

    public HermesAgentArchitectureDemo(ModelSettings settings) {
        this.settings = Objects.requireNonNull(settings, "settings");

        // Hermes 的工具是注册表模型：工具文件在加载时注册 schema、handler、toolset、可用性检查。
        // demo 里只保留四类工具，方便看清楚运行时如何选择和调度。
        tools.register("terminal.run", "terminal", args -> "在隔离终端后端执行: " + args.get("command"));
        tools.register("file.patch", "file", args -> "生成补丁: " + args.get("path"));
        tools.register("memory.save", "memory", args -> "保存长期记忆: " + args.get("fact"));
        tools.register("delegate.task", "delegation", args -> "派生子 Agent 处理: " + args.get("goal"));

        // Skills 在 Hermes 里属于按需加载的程序化记忆。它不是每轮都塞进 prompt，
        // 而是根据任务意图加载相关 SKILL.md，再把核心流程交给主 Agent。
        skills.register(new HermesSkill("coding-agent-review", "代码审查、补丁、测试、PR 交付流程"));
        skills.register(new HermesSkill("gateway-ops", "消息网关、平台适配、会话路由排障"));

        memory.save("项目约定：外部 API 必须透传 requestId，日志也要打印同一个 requestId。");
    }

    public void run() {
        System.out.println("[4] Hermes agent architecture");
        System.out.println("Model profile: " + settings.summary());

        List<HermesInbound> inputs = List.of(
                new HermesInbound("cli", "user-1001", "repo-42", "terminal", "检查订单接口并给出修复计划"),
                new HermesInbound("gateway:telegram", "user-1001", "repo-42", "tg:1001", "继续，把需要修改的文件列出来"),
                new HermesInbound("cron", "system", "daily-report", "telegram:user-1001", "每天早上总结昨天的 Agent 工作"));

        for (HermesInbound input : inputs) {
            HermesSession session = sessions.findOrCreate(input.userId(), input.conversationRef());
            HermesReply reply = runTurn(session, input);
            deliver(input, reply);
        }

        System.out.println("Session traces:");
        sessions.all().forEach(session -> {
            System.out.println("- " + session.id() + " / " + session.key());
            session.traces().forEach(trace -> System.out.println("    " + trace));
        });
    }

    private HermesReply runTurn(HermesSession session, HermesInbound input) {
        session.append("entry: " + input.surface() + " -> " + input.replyTarget());

        String prompt = buildPrompt(input);
        session.append("prompt: " + prompt);

        List<HermesSkill> matchedSkills = skills.match(input.text());
        matchedSkills.forEach(skill -> session.append("skill: load " + skill.name()));

        List<String> recalledMemory = memory.recall(input.text());
        recalledMemory.forEach(item -> session.append("memory: " + item));

        String toolName = chooseTool(input.text(), input.surface());
        String toolResult = tools.call(toolName, Map.of(
                "command", "mvn test",
                "path", "src/main/java",
                "fact", "订单接口修复计划需要先检查 requestId 透传",
                "goal", input.text()));
        session.append("tool:" + toolName + " -> " + toolResult);

        memory.save("最近一次 Hermes turn 使用 " + toolName + " 处理 " + input.conversationRef());
        return new HermesReply(session.id(), "[" + settings.provider().id() + "] " + toolResult);
    }

    private String buildPrompt(HermesInbound input) {
        // Hermes 官方架构把 prompt 分成 identity/tool guidance/skills、context files、memory/profile 等层。
        // demo 只保留最小字段，真实系统会继续叠加 SOUL.md、AGENTS.md、HERMES.md、工具 schema 和上下文压缩。
        return "identity=Hermes Agent; surface=" + input.surface()
                + "; provider=" + settings.provider().id()
                + "; conversation=" + input.conversationRef();
    }

    private String chooseTool(String text, String surface) {
        String normalized = text.toLowerCase(Locale.ROOT);
        if (surface.startsWith("cron")) {
            return "memory.save";
        }
        if (normalized.contains("文件") || normalized.contains("修改") || normalized.contains("补丁")) {
            return "file.patch";
        }
        if (normalized.contains("计划") || normalized.contains("检查")) {
            return "delegate.task";
        }
        return "terminal.run";
    }

    private void deliver(HermesInbound input, HermesReply reply) {
        // Hermes 的核心 AIAgent 不应该直接依赖 Telegram/Discord/CLI。
        // Gateway 或 CLI 表面负责把统一回复投递回 replyTarget。
        System.out.println("  [" + input.surface() + " -> " + input.replyTarget() + "] " + reply.text());
    }

    public record HermesInbound(String surface,
                                String userId,
                                String conversationRef,
                                String replyTarget,
                                String text) {
    }

    public record HermesReply(String sessionId, String text) {
    }

    record HermesSkill(String name, String description) {
    }

    interface HermesTool {
        String call(Map<String, String> args);
    }

    static final class HermesToolRegistry {
        private final Map<String, RegisteredTool> tools = new LinkedHashMap<>();

        void register(String name, String toolset, HermesTool handler) {
            tools.put(name, new RegisteredTool(name, toolset, handler));
        }

        String call(String name, Map<String, String> args) {
            RegisteredTool tool = tools.get(name);
            if (tool == null) {
                return "未知工具: " + name;
            }
            return tool.handler().call(args);
        }

        record RegisteredTool(String name, String toolset, HermesTool handler) {
        }
    }

    static final class HermesSkillCatalog {
        private final List<HermesSkill> skills = new ArrayList<>();

        void register(HermesSkill skill) {
            skills.add(skill);
        }

        List<HermesSkill> match(String text) {
            String normalized = text.toLowerCase(Locale.ROOT);
            return skills.stream()
                    .filter(skill -> normalized.contains("代码")
                            || normalized.contains("接口")
                            || skill.description().toLowerCase(Locale.ROOT).contains("gateway"))
                    .limit(2)
                    .toList();
        }
    }

    static final class HermesMemory {
        private final List<String> facts = new ArrayList<>();

        void save(String fact) {
            facts.add(Instant.now() + " " + fact);
        }

        List<String> recall(String text) {
            String normalized = text.toLowerCase(Locale.ROOT);
            return facts.stream()
                    .filter(fact -> normalized.contains("接口") || fact.toLowerCase(Locale.ROOT).contains("requestid"))
                    .limit(2)
                    .toList();
        }
    }

    static final class HermesSessionStore {
        private final Map<String, HermesSession> sessions = new LinkedHashMap<>();

        HermesSession findOrCreate(String userId, String conversationRef) {
            String key = userId + ":" + conversationRef;
            return sessions.computeIfAbsent(key, HermesSession::new);
        }

        List<HermesSession> all() {
            return List.copyOf(sessions.values());
        }
    }

    static final class HermesSession {
        private final String key;
        private final String id;
        private final List<String> traces = new ArrayList<>();

        HermesSession(String key) {
            this.key = key;
            this.id = "hermes-" + Integer.toHexString(key.hashCode()).toUpperCase(Locale.ROOT);
        }

        void append(String trace) {
            traces.add(trace);
        }

        String id() {
            return id;
        }

        String key() {
            return key;
        }

        List<String> traces() {
            return List.copyOf(traces);
        }
    }
}
