package com.example.agentconcepts;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 8.1 的重点不是框架 API，而是把 Agent 的职责边界看清楚。
 * 这里把售后机器人拆成模型、工具、记忆、护栏四个可替换零件，业务目标由 Agent 统一协调。
 */
public final class AgentConceptsDemo {
    private AgentConceptsDemo() {}

    public static void main(String[] args) {
        ModelSettings settings = ModelSettings.fromEnvironment(System.getenv());
        Agent agent = new Agent(new RuleBasedModel(), new OrderTool(), new Memory(), 500);
        String answer = agent.handle("请查询订单 A100，并判断能否退款");
        System.out.println(answer);
        // 只展示提供方、模型和地址，绝不打印 apiKey。
        System.out.printf("模型配置：provider=%s, model=%s, baseUrl=%s%n",
                settings.provider(), settings.model(), settings.baseUrl());
    }

    /** 模型负责“判断下一步做什么”；真实接入时只需替换这个接口的实现。 */
    public interface ReasoningModel {
        Decision decide(String goal, List<String> memory);
    }

    /** 决策用结构化数据表达，避免靠解析一段自然语言来驱动业务操作。 */
    public record Decision(String toolName, String argument) {}

    /** 工具是 Agent 访问业务系统的唯一入口。 */
    public interface Tool {
        String name();
        String execute(String argument);
    }

    /** 短期记忆保存本轮可追踪的事件；生产环境可替换为数据库或缓存。 */
    public static final class Memory {
        private final List<String> events = new ArrayList<>();
        public void add(String event) { events.add(event); }
        public List<String> snapshot() { return List.copyOf(events); }
    }

    public static final class Agent {
        private final ReasoningModel model;
        private final Tool orderTool;
        private final Memory memory;
        private final int autoRefundLimit;

        public Agent(ReasoningModel model, Tool orderTool, Memory memory, int autoRefundLimit) {
            this.model = model;
            this.orderTool = orderTool;
            this.memory = memory;
            this.autoRefundLimit = autoRefundLimit;
        }

        public String handle(String goal) {
            memory.add("目标：" + goal);
            Decision decision = model.decide(goal, memory.snapshot());
            if (!orderTool.name().equals(decision.toolName())) {
                return "护栏拒绝：模型请求了未注册工具 " + decision.toolName();
            }
            String observation = orderTool.execute(decision.argument());
            memory.add("工具观察：" + observation);
            int amount = Integer.parseInt(observation.split("amount=")[1]);
            // 金额护栏属于确定性业务规则，不能交给概率模型决定。
            return amount <= autoRefundLimit
                    ? "订单可自动退款；依据：" + observation
                    : "订单金额超过 " + autoRefundLimit + " 元，转人工审批；依据：" + observation;
        }

        public List<String> memory() { return memory.snapshot(); }
    }

    /** 离线模型稳定地产生工具调用，让测试不依赖网络或 API Key。 */
    public static final class RuleBasedModel implements ReasoningModel {
        @Override public Decision decide(String goal, List<String> memory) {
            // 不把缺失或未知订单静默替换成 A100，防止处理错单。
            var matcher = java.util.regex.Pattern.compile("\\bA[0-9]+\\b").matcher(goal);
            if (!matcher.find()) throw new IllegalArgumentException("请提供订单号");
            String orderId = matcher.group();
            return new Decision("query_order", orderId);
        }
    }

    public static final class OrderTool implements Tool {
        private final Map<String, Integer> orders = Map.of("A100", 299, "A200", 899);
        @Override public String name() { return "query_order"; }
        @Override public String execute(String orderId) {
            Integer amount = orders.get(orderId);
            if (amount == null) throw new IllegalArgumentException("订单不存在：" + orderId);
            return "orderId=" + orderId + ", status=PAID, amount=" + amount;
        }
    }

    /** DeepSeek、Ollama、ChatGPT 的连接信息只从环境变量读取。 */
    public record ModelSettings(String provider, String baseUrl, String model, String apiKey) {
        @Override public String toString() { return "ModelSettings[provider=" + provider + ", model=" + model + ", apiKey=***]"; }
        public static ModelSettings fromEnvironment(Map<String, String> env) {
            String provider = env.getOrDefault("AI_PROVIDER", "deepseek").toLowerCase(Locale.ROOT);
            return switch (provider) {
                case "deepseek" -> new ModelSettings(provider, env.getOrDefault("AI_BASE_URL", "https://api.deepseek.com"), env.getOrDefault("AI_MODEL", "deepseek-flash"), env.getOrDefault("DEEPSEEK_API_KEY", ""));
                case "ollama" -> new ModelSettings(provider, env.getOrDefault("AI_BASE_URL", "http://localhost:11434/v1"), env.getOrDefault("AI_MODEL", "deepseek-r1:7b"), "not-required");
                case "openai", "chatgpt" -> new ModelSettings("openai", env.getOrDefault("AI_BASE_URL", "https://api.openai.com/v1"), env.getOrDefault("AI_MODEL", "gpt-4.1-mini"), env.getOrDefault("OPENAI_API_KEY", ""));
                default -> throw new IllegalArgumentException("不支持的 AI_PROVIDER：" + provider);
            };
        }
    }
}
