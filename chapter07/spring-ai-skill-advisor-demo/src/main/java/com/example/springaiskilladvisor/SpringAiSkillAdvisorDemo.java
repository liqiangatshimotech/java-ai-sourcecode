package com.example.springaiskilladvisor;

import java.util.List;
import java.util.Locale;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;

/** 真实 ChatClient 与 CallAdvisor 集成；offline 只替换最后一跳模型，不绕过框架。 */
public final class SpringAiSkillAdvisorDemo {
    public static void main(String[] args) {
        boolean live = "live".equalsIgnoreCase(System.getenv("APP_AI_MODE"));
        ChatModel model = live ? liveModel() : new OfflineModel();
        System.out.println("mode=" + (live ? "live" : "offline（模拟模型，非 DeepSeek 回答）"));
        System.out.println(client(model).prompt().user("请评审订单 Spring Boot 接口的权限和兼容性").call().content());
    }

    public static ChatClient client(ChatModel model) {
        return ChatClient.builder(model)
                .defaultSystem("你是 Java 接口评审助手。只依据提供的代码作判断，缺少 diff 时先索取代码。")
                .defaultAdvisors(new SkillAdvisor()).build();
    }

    /** Skill 决定评审方法；此 Advisor 不授予文件读取、命令执行或业务写入权限。 */
    public static final class SkillAdvisor implements CallAdvisor {
        public String getName() { return "course-skill-advisor"; }
        public int getOrder() { return 100; }
        public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
            String task = request.prompt().getUserMessage().getText().toLowerCase(Locale.ROOT);
            if (!task.contains("接口") && !task.contains("controller")) {
                // 未命中时保留原请求，让后续 Advisor 与模型正常处理。
                return chain.nextCall(request);
            }
            Prompt enriched = request.prompt().augmentSystemMessage(system ->
                    new org.springframework.ai.chat.messages.SystemMessage(system.getText()
                            + "\nSkill: spring-boot-api-review\n先确认接口契约，再检查兼容性、权限、异常处理与测试覆盖。"));
            // 保留用户消息、模型选项及上下文，只追加受信任的本地 Skill 指令。
            return chain.nextCall(request.mutate().prompt(enriched).build());
        }
    }

    /** 用于回归测试的模型替身；真实框架仍负责 Advisor 排序与请求传递。 */
    public static final class OfflineModel implements ChatModel {
        private Prompt lastPrompt;
        public Prompt lastPrompt() { return lastPrompt; }
        public ChatResponse call(Prompt prompt) {
            lastPrompt = prompt;
            String answer = prompt.getSystemMessage().getText().contains("spring-boot-api-review")
                    ? "[offline] 已收到接口评审 Skill；请提供接口代码或 diff。"
                    : "[offline] 普通请求，未注入接口评审 Skill。";
            return new ChatResponse(List.of(new Generation(new AssistantMessage(answer))));
        }
    }

    /** 三家均使用兼容协议；BASE_URL 为主机根地址，请勿重复追加 /v1。 */
    public static ChatModel liveModel() {
        String provider = env("APP_AI_DEFAULT_PROVIDER", "deepseek").toLowerCase(Locale.ROOT);
        String prefix = switch (provider) {
            case "deepseek" -> "DEEPSEEK";
            case "ollama" -> "OLLAMA";
            case "chatgpt", "openai" -> "OPENAI";
            default -> throw new IllegalArgumentException("仅支持 deepseek、ollama、chatgpt/openai");
        };
        String url = env(prefix + "_BASE_URL", switch (prefix) {
            case "DEEPSEEK" -> "https://api.deepseek.com";
            case "OLLAMA" -> "http://localhost:11434";
            default -> "https://api.openai.com";
        });
        String key = env(prefix + "_API_KEY", prefix.equals("OLLAMA") ? "ollama" : "");
        if (key.isBlank()) throw new IllegalStateException("live 模式需要 " + prefix + "_API_KEY");
        String model = env(prefix + "_MODEL", switch (prefix) {
            case "DEEPSEEK" -> "deepseek-v4-flash";
            case "OLLAMA" -> "qwen2.5:7b";
            default -> "gpt-4.1-mini";
        });
        var api = OpenAiApi.builder().baseUrl(url).completionsPath("/v1/chat/completions").apiKey(key).build();
        return OpenAiChatModel.builder().openAiApi(api)
                .defaultOptions(OpenAiChatOptions.builder().model(model).temperature(0.0).build()).build();
    }
    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
