package com.example.springaichatmemory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 5.2 Spring AI Chat Memory 的最小模型。
 *
 * <p>这个示例没有直接调用真实大模型，目的是把会话记忆的核心链路稳定地跑出来：
 * 请求带上 conversationId -> 读取该会话历史 -> 生成回答 -> 写入 user/assistant 消息 ->
 * 按窗口大小裁剪历史。真实接入 Spring AI 时，这条链路通常由
 * MessageChatMemoryAdvisor + MessageWindowChatMemory + ChatMemoryRepository 完成。</p>
 */
public class SpringAiChatMemoryDemo {

    public static void main(String[] args) {
        // 模型配置从环境变量读取。默认使用 deepseek，同时保留 ollama 和 chatgpt 的配置入口。
        // 这个 demo 的模型客户端是确定性实现，不会真的发起网络请求，方便在课堂和 CI 中稳定运行。
        ModelSettings settings = ModelSettings.fromEnv();

        // MessageWindowMemory 对应 Spring AI 里的 MessageWindowChatMemory：
        // 只保留最近 maxMessages 条消息，避免每次请求都把完整历史塞进上下文。
        MessageWindowMemory memory = new MessageWindowMemory(10);

        // AdvisorStyleChatService 用来模拟 Spring AI Advisor 的工作方式：
        // 调用模型前读取记忆，调用模型后把本轮 user/assistant 消息写回记忆。
        AdvisorStyleChatService chatService = new AdvisorStyleChatService(memory, new DeterministicModelClient(settings));

        // c-east 第一次写入区域信息，第二次在同一个 conversationId 下提问，可以读到上一轮历史。
        System.out.println(chatService.chat("c-east", "我是华东区售后负责人，回答要短。"));
        System.out.println(chatService.chat("c-east", "我负责哪个区域？"));

        // c-south 是另一条会话线。即使问题相同，也不能读取 c-east 的记忆。
        System.out.println(chatService.chat("c-south", "我负责哪个区域？"));
        System.out.println("核心结论: 每次调用都要带 conversationId，记忆才不会串到别的会话。");
    }

    /**
     * 用一个很小的服务类模拟 Spring AI 的 Advisor 调用链。
     *
     * <p>真实项目里，业务代码通常调用 ChatClient。ChatClient 执行前后会经过 Advisor：
     * 前置阶段把历史消息追加到请求上下文，后置阶段把本轮消息保存到 ChatMemory。</p>
     */
    static class AdvisorStyleChatService {
        private final MessageWindowMemory memory;
        private final ModelClient modelClient;

        AdvisorStyleChatService(MessageWindowMemory memory, ModelClient modelClient) {
            this.memory = memory;
            this.modelClient = modelClient;
        }

        String chat(String conversationId, String userMessage) {
            // 1. 按 conversationId 取当前会话的历史。
            //    Spring AI 里通常通过 ChatMemory.CONVERSATION_ID 传入这个值。
            List<ChatMessage> history = memory.messages(conversationId);

            // 2. 模型生成回答时只能看到当前 conversationId 下的历史。
            //    这里用 DeterministicModelClient 替代真实模型，方便观察记忆隔离效果。
            String answer = modelClient.generate(history, userMessage);

            // 3. 模型返回后，把本轮用户输入和助手回答都写入记忆。
            //    只保存 user 而不保存 assistant，会导致下一轮上下文不完整。
            memory.add(conversationId, ChatMessage.user(userMessage));
            memory.add(conversationId, ChatMessage.assistant(answer));

            // 返回时带上 conversationId，方便从控制台直接看出是哪条会话线的结果。
            return "[" + conversationId + "] " + answer;
        }
    }

    /**
     * 按会话隔离的短期消息窗口。
     *
     * <p>Map 的 key 是 conversationId，value 是该会话自己的消息队列。这个结构展示了
     * Spring AI 记忆里最重要的工程边界：同一个用户可以有多条会话，同一应用也会同时服务
     * 很多用户，记忆必须明确绑定到会话标识上。</p>
     */
    static class MessageWindowMemory {
        private final int maxMessages;
        private final Map<String, Deque<ChatMessage>> messagesByConversation = new HashMap<>();

        MessageWindowMemory(int maxMessages) {
            this.maxMessages = maxMessages;
        }

        void add(String conversationId, ChatMessage message) {
            // computeIfAbsent 确保每个 conversationId 都拥有独立的消息队列。
            // 如果这里错误地共用一个队列，不同用户或不同会话的记忆就会互相污染。
            Deque<ChatMessage> messages = messagesByConversation.computeIfAbsent(conversationId, ignored -> new ArrayDeque<>());
            messages.addLast(message);

            // 窗口裁剪只保留最近 maxMessages 条消息。
            // 这解决的是上下文长度问题，不解决长期偏好或业务状态存储问题。
            while (messages.size() > maxMessages) {
                messages.removeFirst();
            }
        }

        List<ChatMessage> messages(String conversationId) {
            // 返回副本，避免调用方直接修改内部队列。
            // 如果没有找到 conversationId，说明这条会话没有历史，直接返回空列表。
            return new ArrayList<>(messagesByConversation.getOrDefault(conversationId, new ArrayDeque<>()));
        }
    }

    /**
     * 模型客户端接口。
     *
     * <p>真实项目里这里可以替换成 Spring AI ChatClient、OpenAI 兼容接口、Ollama 本地模型等。
     * 示例保留接口，是为了把“记忆管理”和“模型调用”两个职责拆开。</p>
     */
    interface ModelClient {
        String generate(List<ChatMessage> history, String userMessage);
    }

    /**
     * 确定性模型客户端。
     *
     * <p>它不会访问 DeepSeek、Ollama 或 ChatGPT，只根据 history 里的内容返回固定结果。
     * 这样可以稳定展示记忆是否隔离、是否被正确写入，而不会受到网络、API key 或模型随机性的影响。</p>
     */
    static class DeterministicModelClient implements ModelClient {
        private final ModelSettings settings;

        DeterministicModelClient(ModelSettings settings) {
            this.settings = settings;
        }

        @Override
        public String generate(List<ChatMessage> history, String userMessage) {
            // 当用户问“哪个区域”时，模型只在当前会话历史中查找区域信息。
            // c-east 写入过“华东区”，所以能回答；c-south 没有历史，所以不能回答。
            if (userMessage.contains("哪个区域")) {
                return history.stream()
                        .map(ChatMessage::content)
                        // 这里只识别两个区域词，保持示例足够小，重点放在记忆链路而不是 NLP 解析。
                        .filter(content -> content.contains("华东区") || content.contains("华南区"))
                        // 如果同一会话里多次更新区域，以最后一次出现的区域为准。
                        .reduce((first, second) -> second)
                        .map(content -> "你前面提到负责" + (content.contains("华东区") ? "华东区" : "华南区") + "。")
                        .orElse("这个会话里还没有区域信息。");
            }

            // 普通输入只确认写入会话窗口。provider 名称来自配置，便于观察默认模型选择。
            return "已记录到 " + settings.provider().id() + " 会话记忆窗口。";
        }
    }

    /**
     * 简化版消息结构。
     *
     * <p>Spring AI 里会使用 UserMessage、AssistantMessage、SystemMessage、ToolResponseMessage 等类型。
     * 这里保留 role + content 两个字段，足够表达本节的 user/assistant 轮次关系。</p>
     */
    record ChatMessage(String role, String content) {
        static ChatMessage user(String content) {
            return new ChatMessage("user", content);
        }

        static ChatMessage assistant(String content) {
            return new ChatMessage("assistant", content);
        }
    }

    /**
     * 模型配置。
     *
     * <p>虽然当前 demo 不会真实调用模型，但配置结构按后续项目的习惯保留：
     * deepseek 作为默认 provider，ollama 用于本地模型，chatgpt 用于 OpenAI 兼容调用。</p>
     */
    record ModelSettings(AiProvider provider, String model, String baseUrl) {
        static ModelSettings fromEnv() {
            AiProvider provider = AiProvider.parseOrDefault(System.getenv("APP_AI_DEFAULT_PROVIDER"));
            return switch (provider) {
                // DeepSeek 走 OpenAI 兼容接口。真实接入时还需要读取 DEEPSEEK_API_KEY。
                case DEEPSEEK -> new ModelSettings(provider, env("DEEPSEEK_MODEL", "deepseek-v4-flash"), env("DEEPSEEK_BASE_URL", "https://api.deepseek.com"));
                // Ollama 默认指向本机服务。真实运行前需要先执行 ollama serve 并拉取模型。
                case OLLAMA -> new ModelSettings(provider, env("OLLAMA_MODEL", "qwen2.5:7b"), env("OLLAMA_BASE_URL", "http://localhost:11434"));
                // ChatGPT 使用 OpenAI 兼容 base URL。真实接入时可通过环境变量切换模型。
                case CHATGPT -> new ModelSettings(provider, env("CHATGPT_MODEL", "gpt-4.1-mini"), env("CHATGPT_BASE_URL", "https://api.openai.com/v1"));
            };
        }

        private static String env(String name, String fallback) {
            // 环境变量为空时使用默认值，避免本地运行 demo 时必须准备完整配置。
            String value = System.getenv(name);
            return value == null || value.isBlank() ? fallback : value;
        }
    }

    /**
     * 支持的模型供应商。
     *
     * <p>课程后续示例统一保留 deepseek、ollama、chatgpt 三种入口。枚举的 id 使用小写，
     * 方便直接和环境变量、HTTP 参数或配置文件中的字符串对应。</p>
     */
    enum AiProvider {
        DEEPSEEK("deepseek"),
        OLLAMA("ollama"),
        CHATGPT("chatgpt");

        private final String id;

        AiProvider(String id) {
            this.id = id;
        }

        String id() {
            return id;
        }

        static AiProvider parseOrDefault(String raw) {
            // 不传 provider 时使用 DeepSeek，符合课程示例的默认模型选择。
            if (raw == null || raw.isBlank()) {
                return DEEPSEEK;
            }
            String normalized = raw.trim().toLowerCase(Locale.ROOT);
            for (AiProvider provider : values()) {
                if (provider.id.equals(normalized)) {
                    return provider;
                }
            }
            throw new IllegalArgumentException("provider only supports deepseek, ollama, chatgpt");
        }
    }
}
