package com.example.springaipersistentmemory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 5.3 Spring AI 持久化记忆与长期记忆的最小模型。
 *
 * <p>这个文件负责演示“可稳定运行”的核心逻辑：会话消息写入持久化仓库、
 * 服务重启后按 conversationId 读回消息、从用户输入中提炼可复用的长期偏好。
 * 同目录下的 config/JdbcChatMemoryConfiguration.java 则展示真实 Spring AI 项目里
 * 如何把 ChatMemoryRepository 换成 JDBC 实现。</p>
 *
 * <p>两条线要分清楚：
 * 会话消息持久化解决的是“同一会话下一轮能看到上一轮说过什么”；
 * 长期记忆解决的是“跨会话复用稳定偏好或事实”。它们不能简单混在一张表里。</p>
 */
public class SpringAiPersistentMemoryDemo {

    public static void main(String[] args) throws IOException {
        // 模型配置保留 DeepSeek / Ollama / ChatGPT 三种入口。
        // 本 demo 不真实调用模型，读取配置只是为了和后续真实项目的配置风格一致。
        ModelSettings settings = ModelSettings.fromEnv();

        // 用临时文件模拟 JDBC 表。这样运行 demo 不需要启动数据库，也能观察“重启后仍可读取”的效果。
        // 真实项目里，对应的实现是 Spring AI 的 JdbcChatMemoryRepository。
        Path storeFile = Files.createTempFile("spring-ai-memory-", ".txt");
        PersistentChatMemoryRepository repository = new FileChatMemoryRepository(storeFile);

        // MessageWindowChatMemory 负责窗口裁剪：只保留最近 maxMessages 条消息。
        // 持久化仓库负责“存到哪里”，窗口记忆负责“保留多少上下文”，这是两个职责。
        MessageWindowChatMemory chatMemory = new MessageWindowChatMemory(repository, 6);

        // 长期记忆单独维护，不直接复用原始聊天记录。
        // 这里用内存 List 演示提炼后的偏好存储，真实项目可以换成数据库或向量库。
        LongTermMemoryStore longTermMemoryStore = new LongTermMemoryStore();
        PreferenceExtractor extractor = new PreferenceExtractor();

        // conversationId 是会话记忆的隔离键。只有同一个 conversationId 下的消息才互相可见。
        String conversationId = "support-c-1001";

        // 第一轮：用户给出一个稳定偏好。它既会进入当前会话窗口，也会被提炼成长期记忆。
        addTurn(chatMemory, conversationId, "以后回答用中文，别太长。", "收到，我会保持中文和简短。");
        extractor.extract("u-1001", "以后回答用中文，别太长。").forEach(longTermMemoryStore::save);

        // 第二轮：这是当前会话上下文，但不一定适合直接沉淀为长期记忆。
        // 是否进入长期记忆，要看它是不是稳定、可复用、合规的信息。
        addTurn(chatMemory, conversationId, "我是华东区售后负责人。", "已记录本轮会话上下文。");

        // 模拟服务重启：重新创建 repository，再从同一个持久化文件读取消息。
        // 如果这里能读到消息，就说明会话消息确实落到了仓库里。
        PersistentChatMemoryRepository reloadedRepository = new FileChatMemoryRepository(storeFile);
        List<ChatMessage> reloaded = reloadedRepository.findByConversationId(conversationId);

        System.out.println("Provider: " + settings.provider().id());
        System.out.println("持久化文件: " + storeFile);
        System.out.println("服务重启后仍可读取消息数: " + reloaded.size());
        System.out.println("长期偏好: " + longTermMemoryStore.search("u-1001", "回答风格"));
        System.out.println("核心结论: 会话消息落库不等于长期记忆，长期记忆应提炼、带来源、可过期。");
    }

    /**
     * 写入一轮完整对话。
     *
     * <p>Chat Memory 一般同时保存 user 消息和 assistant 消息。只保存用户输入，
     * 下一轮模型会缺少自己上一轮的回答；只保存 assistant 消息，则丢失用户意图。</p>
     */
    private static void addTurn(MessageWindowChatMemory memory, String conversationId, String user, String assistant) throws IOException {
        memory.add(conversationId, ChatMessage.user(user));
        memory.add(conversationId, ChatMessage.assistant(assistant));
    }

    /**
     * 持久化会话消息仓库的最小接口。
     *
     * <p>这个接口刻意贴近 Spring AI ChatMemoryRepository 的语义：
     * 通过 conversationId 查找消息，通过 conversationId 保存裁剪后的消息列表。
     * 后续把 FileChatMemoryRepository 换成 JDBC、MongoDB 或 Cassandra 时，上层窗口逻辑不需要变化。</p>
     */
    interface PersistentChatMemoryRepository {
        List<ChatMessage> findByConversationId(String conversationId) throws IOException;

        void save(String conversationId, List<ChatMessage> messages) throws IOException;
    }

    /**
     * 带窗口裁剪的会话记忆。
     *
     * <p>它不关心消息最终写到文件、数据库还是缓存，只通过 PersistentChatMemoryRepository
     * 读写消息。这个拆分对应真实项目里的 ChatMemory + ChatMemoryRepository。</p>
     */
    static class MessageWindowChatMemory {
        private final PersistentChatMemoryRepository repository;
        private final int maxMessages;

        MessageWindowChatMemory(PersistentChatMemoryRepository repository, int maxMessages) {
            this.repository = repository;
            this.maxMessages = maxMessages;
        }

        void add(String conversationId, ChatMessage message) throws IOException {
            // 每次写入前先加载当前会话已有消息。真实 JDBC 实现也是按 conversationId 读写。
            Deque<ChatMessage> messages = new ArrayDeque<>(repository.findByConversationId(conversationId));
            messages.addLast(message);

            // 保留最近 maxMessages 条消息，防止历史无限增长。
            // 注意：被裁掉的是短期上下文，不代表长期记忆被删除。
            while (messages.size() > maxMessages) {
                messages.removeFirst();
            }

            // 保存的是裁剪后的窗口，而不是完整历史。
            // 如果需要审计全量历史，应使用独立的审计日志或业务流水表。
            repository.save(conversationId, new ArrayList<>(messages));
        }
    }

    /**
     * 用本地文件模拟持久化仓库。
     *
     * <p>文件里每一行是一条消息：conversationId | role | base64(content) | timestamp。
     * content 使用 Base64 是为了避免用户输入里的换行、竖线等字符破坏简单文本格式。
     * 这个类只是课堂演示用；生产项目应使用 JdbcChatMemoryRepository 等正式实现。</p>
     */
    static class FileChatMemoryRepository implements PersistentChatMemoryRepository {
        private final Path file;

        FileChatMemoryRepository(Path file) {
            this.file = file;
        }

        @Override
        public List<ChatMessage> findByConversationId(String conversationId) throws IOException {
            // 文件不存在说明还没有任何会话消息。
            if (!Files.exists(file)) {
                return List.of();
            }
            List<ChatMessage> result = new ArrayList<>();
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                // 最多拆成 4 段，避免内容字段中出现分隔符时影响后面的 timestamp。
                // 当前内容已经 Base64 编码，实际不会包含竖线，这里仍保留稳妥写法。
                String[] parts = line.split("\\|", 4);
                if (parts.length == 4 && parts[0].equals(conversationId)) {
                    result.add(new ChatMessage(parts[1], decode(parts[2]), Instant.parse(parts[3])));
                }
            }
            return result;
        }

        @Override
        public void save(String conversationId, List<ChatMessage> messages) throws IOException {
            // 先读取文件里的所有会话，再替换当前 conversationId 的窗口。
            // 这样不会因为保存一条会话而覆盖其他会话的数据。
            Map<String, List<ChatMessage>> all = new HashMap<>();
            if (Files.exists(file)) {
                for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                    String[] parts = line.split("\\|", 4);
                    if (parts.length == 4) {
                        all.computeIfAbsent(parts[0], ignored -> new ArrayList<>())
                                .add(new ChatMessage(parts[1], decode(parts[2]), Instant.parse(parts[3])));
                    }
                }
            }
            all.put(conversationId, messages);

            // 重新写回整个文件。真实数据库实现会用 delete + batch insert 或 upsert 完成类似动作。
            List<String> lines = new ArrayList<>();
            all.forEach((id, storedMessages) -> storedMessages.forEach(message ->
                    lines.add(id + "|" + message.role() + "|" + encode(message.content()) + "|" + message.createdAt())
            ));
            Files.write(file, lines, StandardCharsets.UTF_8);
        }

        private static String encode(String value) {
            // URL safe Base64 输出不会包含换行，适合这个一行一条记录的演示格式。
            return Base64.getUrlEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
        }

        private static String decode(String value) {
            return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
        }
    }

    /**
     * 从用户输入中提炼长期记忆。
     *
     * <p>真实系统里这一步可能由模型、规则、人工确认或多策略组合完成。
     * 本示例只识别“回答用中文、别太长”这类稳定偏好，避免把整段聊天直接当长期事实保存。</p>
     */
    static class PreferenceExtractor {
        List<LongTermMemory> extract(String userId, String userMessage) {
            if (userMessage.contains("回答用中文") || userMessage.contains("别太长")) {
                // 保存的是归一化后的偏好，而不是用户原话。
                // 这样后续召回时更稳定，也更容易做去重、过期和权限控制。
                return List.of(new LongTermMemory(userId, "preference", "回答用中文，保持简短", Instant.now(), null));
            }
            return List.of();
        }
    }

    /**
     * 长期记忆存储。
     *
     * <p>这里用 List 保持示例简洁。真实项目通常会额外保存来源、置信度、租户、权限标签、
     * 过期时间和删除标记，并可能用向量检索做语义召回。</p>
     */
    static class LongTermMemoryStore {
        private final List<LongTermMemory> memories = new ArrayList<>();

        void save(LongTermMemory memory) {
            memories.add(memory);
        }

        List<LongTermMemory> search(String userId, String query) {
            // query 参数在这个最小示例里没有做语义匹配，保留它是为了说明真实接口会有“按问题召回”的动作。
            // userId 过滤是必须的，否则不同用户的长期偏好会互相污染。
            return memories.stream()
                    .filter(memory -> memory.userId().equals(userId))
                    .filter(LongTermMemory::reusable)
                    .toList();
        }
    }

    /**
     * 会话消息。
     *
     * <p>role 区分 user / assistant，createdAt 用于排序和排查问题。
     * Spring AI JDBC schema 中也会保存消息类型和时间戳。</p>
     */
    record ChatMessage(String role, String content, Instant createdAt) {
        static ChatMessage user(String content) {
            return new ChatMessage("user", content, Instant.now());
        }

        static ChatMessage assistant(String content) {
            return new ChatMessage("assistant", content, Instant.now());
        }
    }

    /**
     * 提炼后的长期记忆。
     *
     * @param userId 记忆归属，避免跨用户召回
     * @param type 记忆类型，例如 preference、profile、summary
     * @param content 归一化后的记忆内容
     * @param observedAt 这条记忆被观察或生成的时间
     * @param expiresAt 过期时间；为空表示没有明确过期时间
     */
    record LongTermMemory(String userId, String type, String content, Instant observedAt, Instant expiresAt) {
        boolean reusable() {
            // 长期记忆不是永久事实。过期信息不能继续参与回答。
            return expiresAt == null || expiresAt.isAfter(Instant.now());
        }
    }

    /**
     * 模型配置。
     *
     * <p>本示例默认 DeepSeek，并保留 Ollama 和 ChatGPT 配置入口。
     * 即使这个文件不真实调用模型，配置结构也和后续真实项目保持一致。</p>
     */
    record ModelSettings(AiProvider provider, String model, String baseUrl) {
        static ModelSettings fromEnv() {
            AiProvider provider = AiProvider.parseOrDefault(System.getenv("APP_AI_DEFAULT_PROVIDER"));
            return switch (provider) {
                // DeepSeek 使用 OpenAI 兼容接口。真实项目还需要读取 DEEPSEEK_API_KEY。
                case DEEPSEEK -> new ModelSettings(provider, env("DEEPSEEK_MODEL", "deepseek-v4-flash"), env("DEEPSEEK_BASE_URL", "https://api.deepseek.com"));
                // Ollama 指向本地服务，适合无外部 API key 的开发环境。
                case OLLAMA -> new ModelSettings(provider, env("OLLAMA_MODEL", "qwen2.5:7b"), env("OLLAMA_BASE_URL", "http://localhost:11434"));
                // ChatGPT 使用 OpenAI 兼容 base URL，模型名可通过环境变量切换。
                case CHATGPT -> new ModelSettings(provider, env("CHATGPT_MODEL", "gpt-4.1-mini"), env("CHATGPT_BASE_URL", "https://api.openai.com/v1"));
            };
        }

        private static String env(String name, String fallback) {
            // 未设置环境变量时使用默认值，保证本地示例开箱可运行。
            String value = System.getenv(name);
            return value == null || value.isBlank() ? fallback : value;
        }
    }

    /**
     * 支持的模型供应商。
     *
     * <p>枚举的 id 使用小写字符串，便于和环境变量或配置文件里的值直接对应。</p>
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
            // 不指定 provider 时按课程约定默认使用 deepseek。
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
