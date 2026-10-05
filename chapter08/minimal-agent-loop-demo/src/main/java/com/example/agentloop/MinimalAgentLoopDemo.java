package com.example.agentloop;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

/** 从零实现一个可终止、可追踪、受工具白名单约束的 Agent 循环。 */
public final class MinimalAgentLoopDemo {
    private MinimalAgentLoopDemo() {}

    public static void main(String[] args) {
        ToolRegistry tools = new ToolRegistry()
                .register("query_order", id -> "A100".equals(id) ? "status=PAID,amount=299" : "NOT_FOUND")
                .register("calculate_refund", amount -> "refund=" + Math.min(Integer.parseInt(amount), 500));
        RunResult result = new AgentLoop(new ScriptedModel(), tools, 5).run("计算订单 A100 可退金额");
        result.trace().forEach(System.out::println);
        System.out.println("最终答案：" + result.answer());
    }

    /** 模型每次只能返回“调用工具”或“结束”两类动作。 */
    public sealed interface ModelAction permits CallTool, Finish {}
    public record CallTool(String toolName, String argument) implements ModelAction {}
    public record Finish(String answer) implements ModelAction {}
    public record RunResult(String answer, List<String> trace, int steps) {}

    public interface DecisionModel {
        ModelAction next(String goal, List<String> trace);
    }

    @FunctionalInterface public interface Tool { String execute(String argument); }

    public static final class ToolRegistry {
        private final Map<String, Tool> tools = new LinkedHashMap<>();
        public ToolRegistry register(String name, Tool tool) { tools.put(name, tool); return this; }
        public String execute(String name, String argument) {
            Tool tool = tools.get(name);
            if (tool == null) throw new IllegalStateException("模型请求了未注册工具：" + name);
            return tool.execute(argument);
        }
    }

    public static final class AgentLoop {
        private final DecisionModel model;
        private final ToolRegistry tools;
        private final int maxSteps;
        public AgentLoop(DecisionModel model, ToolRegistry tools, int maxSteps) {
            if (maxSteps < 1) throw new IllegalArgumentException("maxSteps 必须大于 0");
            this.model = model; this.tools = tools; this.maxSteps = maxSteps;
        }

        public RunResult run(String goal) {
            List<String> trace = new ArrayList<>();
            for (int step = 1; step <= maxSteps; step++) {
                ModelAction action = model.next(goal, List.copyOf(trace));
                trace.add("第 " + step + " 步决策：" + action);
                if (action instanceof Finish finish) {
                    return new RunResult(finish.answer(), List.copyOf(trace), step);
                }
                CallTool call = (CallTool) action;
                // 工具异常会立即冒泡，避免把失败观察伪装成成功结果继续执行。
                String observation = tools.execute(call.toolName(), call.argument());
                trace.add("工具观察：" + observation);
            }
            throw new IllegalStateException("达到最大步数 " + maxSteps + "，Agent 被安全停止");
        }
    }

    /** 模拟真实模型连续两次调用工具、第三次给出答案。 */
    public static final class ScriptedModel implements DecisionModel {
        @Override public ModelAction next(String goal, List<String> trace) {
            if (trace.isEmpty()) return new CallTool("query_order", "A100");
            if (trace.size() == 2) return new CallTool("calculate_refund", "299");
            return new Finish("订单 A100 可退款 299 元");
        }
    }

    /**
     * 真实模型适配器可读取这组配置。默认 DeepSeek，同时兼容本地 Ollama 与 ChatGPT；
     * 密钥只留在内存中，toString 也经过覆盖，避免排查问题时误打到日志。
     */
    public record ModelSettings(String provider, String baseUrl, String model, String apiKey) {
        public static ModelSettings fromEnvironment(Map<String, String> env) {
            String provider = env.getOrDefault("AI_PROVIDER", "deepseek").toLowerCase(Locale.ROOT);
            return switch (provider) {
                case "deepseek" -> new ModelSettings(provider, env.getOrDefault("AI_BASE_URL", "https://api.deepseek.com"), env.getOrDefault("AI_MODEL", "deepseek-v4-flash"), env.getOrDefault("DEEPSEEK_API_KEY", ""));
                case "ollama" -> new ModelSettings(provider, env.getOrDefault("AI_BASE_URL", "http://localhost:11434/v1"), env.getOrDefault("AI_MODEL", "deepseek-r1:7b"), "not-required");
                case "openai", "chatgpt" -> new ModelSettings("openai", env.getOrDefault("AI_BASE_URL", "https://api.openai.com/v1"), env.getOrDefault("AI_MODEL", "gpt-4.1-mini"), env.getOrDefault("OPENAI_API_KEY", ""));
                default -> throw new IllegalArgumentException("不支持的 AI_PROVIDER：" + provider);
            };
        }
        @Override public String toString() { return "ModelSettings[provider=" + provider + ", baseUrl=" + baseUrl + ", model=" + model + ", apiKey=***]"; }
    }
}
