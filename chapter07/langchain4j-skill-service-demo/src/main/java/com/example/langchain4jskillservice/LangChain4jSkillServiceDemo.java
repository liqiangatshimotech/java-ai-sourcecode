package com.example.langchain4jskillservice;

import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.skills.Skill;
import dev.langchain4j.skills.Skills;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

/** AI Services、官方 MemoryId 与原生 Skills Tool 模式，固定 1.20.0 / beta30。 */
public final class LangChain4jSkillServiceDemo {
    public interface Assistant {
        // id 必须由认证层组装为 tenant:user:conversation，不能直接信任客户端提交的租户。
        String chat(@MemoryId String conversationId, @UserMessage String message);
    }
    public static Assistant create(ChatModel model) {
        Skill review = Skill.builder().name("spring-boot-api-review")
                .description("评审 Java Spring Boot 接口变更")
                .content("先确认接口契约，再检查兼容性、权限、异常处理和测试覆盖；未提供 diff 时先索取代码，不编造结论。")
                .build();
        Skills skills = Skills.from(review);
        return AiServices.builder(Assistant.class).chatModel(model)
                // 不同 MemoryId 创建不同窗口。此处为进程内记忆，重启不持久化。
                .chatMemoryProvider(id -> MessageWindowChatMemory.withMaxMessages(20))
                .toolProvider(skills.toolProvider())
                .systemMessageProvider(id -> "你是 Java 评审助手。相关任务先调用 activate_skill，读完 Skill 后再回答。\n"
                        + skills.formatAvailableSkills())
                .build();
    }
    public static void main(String[] args) {
        boolean live = "live".equalsIgnoreCase(System.getenv("APP_AI_MODE"));
        System.out.println("mode=" + (live ? "live" : "offline（模拟模型，非 DeepSeek 回答）"));
        Assistant assistant = create(live ? liveModel() : new OfflineModel());
        System.out.println(assistant.chat("tenant-a:user-1:review-1", "帮我评审订单接口变更"));
    }
    /** 模拟两轮协议：先请求 activate_skill，框架执行后再生成答案。 */
    public static final class OfflineModel implements ChatModel {
        public final List<ChatRequest> requests = new CopyOnWriteArrayList<>();
        public ChatResponse doChat(ChatRequest request) {
            requests.add(request);
            boolean activated = request.messages().stream().anyMatch(m ->
                    m instanceof ToolExecutionResultMessage t && t.toolName().equals("activate_skill"));
            AiMessage answer = activated ? AiMessage.from("[offline] 已通过原生 activate_skill 读取评审要求；请提供 diff。")
                    : AiMessage.from(ToolExecutionRequest.builder().id("activate-review")
                            .name("activate_skill").arguments("{\"skill_name\":\"spring-boot-api-review\"}").build());
            return ChatResponse.builder().aiMessage(answer).build();
        }
    }
    /** Ollama 使用 /v1 兼容接口，需要模型支持 tool calling；只切换地址无法补足模型能力。 */
    public static ChatModel liveModel() {
        String provider = env("APP_AI_DEFAULT_PROVIDER", "deepseek").toLowerCase(Locale.ROOT);
        String prefix = switch (provider) {
            case "deepseek" -> "DEEPSEEK"; case "ollama" -> "OLLAMA";
            case "openai", "chatgpt" -> "OPENAI";
            default -> throw new IllegalArgumentException("仅支持 deepseek、ollama、chatgpt/openai");
        };
        String key = env(prefix + "_API_KEY", prefix.equals("OLLAMA") ? "ollama" : "");
        if (key.isBlank()) throw new IllegalStateException("live 模式需要 " + prefix + "_API_KEY");
        String base = env(prefix + "_BASE_URL", switch (prefix) {
            case "DEEPSEEK" -> "https://api.deepseek.com/v1";
            case "OLLAMA" -> "http://localhost:11434/v1"; default -> "https://api.openai.com/v1";
        });
        String model = env(prefix + "_MODEL", switch (prefix) {
            case "DEEPSEEK" -> "deepseek-v4-flash"; case "OLLAMA" -> "qwen2.5:7b"; default -> "gpt-4.1-mini";
        });
        return OpenAiChatModel.builder().baseUrl(base).apiKey(key).modelName(model)
                .temperature(0.0).timeout(Duration.ofSeconds(45)).maxRetries(0).build();
    }
    private static String env(String name, String fallback) {
        String value = System.getenv(name); return value == null || value.isBlank() ? fallback : value;
    }
}
