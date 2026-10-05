package com.example.langchain4jchatmemory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LangChain4jChatMemoryDemo {

    public static void main(String[] args) {
        ModelSettings settings = ModelSettings.fromEnv();
        ChatMemoryProvider provider = new CachingChatMemoryProvider();
        Assistant assistant = new AssistantFactory(settings, provider).create();

        System.out.println(assistant.chat("user-a", "我叫老李，负责华东区售后。"));
        System.out.println(assistant.chat("user-b", "我叫小周，负责华南区售前。"));
        System.out.println(assistant.chat("user-a", "我负责哪个区域？"));
        System.out.println(assistant.chat("user-b", "我负责哪个区域？"));
        System.out.println("核心结论: @MemoryId 决定使用哪一份 ChatMemory。");
    }

    interface Assistant {
        String chat(@MemoryId String memoryId, String userMessage);
    }

    static class AssistantFactory {
        private final ModelSettings settings;
        private final ChatMemoryProvider memoryProvider;

        AssistantFactory(ModelSettings settings, ChatMemoryProvider memoryProvider) {
            this.settings = settings;
            this.memoryProvider = memoryProvider;
        }

        Assistant create() {
            return (memoryId, userMessage) -> {
                ChatMemory memory = memoryProvider.get(memoryId);
                String answer = generate(memory.messages(), userMessage);
                memory.add(ChatMessage.user(userMessage));
                memory.add(ChatMessage.assistant(answer));
                return "[" + settings.provider().id() + "/" + memoryId + "] " + answer;
            };
        }

        private String generate(List<ChatMessage> history, String userMessage) {
            if (userMessage.contains("哪个区域")) {
                return history.stream()
                        .map(ChatMessage::content)
                        .filter(content -> content.contains("华东区") || content.contains("华南区"))
                        .reduce((first, second) -> second)
                        .map(content -> content.contains("华东区") ? "你负责华东区。" : "你负责华南区。")
                        .orElse("当前 memoryId 下还没有区域信息。");
            }
            return "已写入当前 memoryId 的会话窗口。";
        }
    }

    interface ChatMemoryProvider {
        ChatMemory get(String memoryId);
    }

    interface ChatMemory {
        void add(ChatMessage message);

        List<ChatMessage> messages();
    }

    static class MessageWindowChatMemory implements ChatMemory {
        private final String id;
        private final int maxMessages;
        private final Deque<ChatMessage> messages = new ArrayDeque<>();

        MessageWindowChatMemory(String id, int maxMessages) {
            this.id = id;
            this.maxMessages = maxMessages;
        }

        @Override
        public void add(ChatMessage message) {
            messages.addLast(message);
            while (messages.size() > maxMessages) {
                messages.removeFirst();
            }
        }

        @Override
        public List<ChatMessage> messages() {
            return new ArrayList<>(messages);
        }

        String id() {
            return id;
        }
    }

    static class TokenWindowChatMemory implements ChatMemory {
        private final int maxCharacters;
        private final Deque<ChatMessage> messages = new ArrayDeque<>();

        TokenWindowChatMemory(int maxCharacters) {
            this.maxCharacters = maxCharacters;
        }

        @Override
        public void add(ChatMessage message) {
            messages.addLast(message);
            while (messages.stream().mapToInt(item -> item.content().length()).sum() > maxCharacters) {
                messages.removeFirst();
            }
        }

        @Override
        public List<ChatMessage> messages() {
            return new ArrayList<>(messages);
        }
    }

    static class CachingChatMemoryProvider implements ChatMemoryProvider {
        private final Map<String, ChatMemory> memories = new HashMap<>();

        @Override
        public ChatMemory get(String memoryId) {
            return memories.computeIfAbsent(memoryId, id -> new MessageWindowChatMemory(id, 10));
        }
    }

    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    @interface MemoryId {
    }

    record ChatMessage(String role, String content) {
        static ChatMessage user(String content) {
            return new ChatMessage("user", content);
        }

        static ChatMessage assistant(String content) {
            return new ChatMessage("assistant", content);
        }
    }

    record ModelSettings(AiProvider provider, String model, String baseUrl) {
        static ModelSettings fromEnv() {
            AiProvider provider = AiProvider.parseOrDefault(System.getenv("APP_AI_DEFAULT_PROVIDER"));
            return switch (provider) {
                case DEEPSEEK -> new ModelSettings(provider, env("DEEPSEEK_MODEL", "deepseek-v4-flash"), env("DEEPSEEK_BASE_URL", "https://api.deepseek.com"));
                case OLLAMA -> new ModelSettings(provider, env("OLLAMA_MODEL", "qwen2.5:7b"), env("OLLAMA_BASE_URL", "http://localhost:11434"));
                case CHATGPT -> new ModelSettings(provider, env("CHATGPT_MODEL", "gpt-4.1-mini"), env("CHATGPT_BASE_URL", "https://api.openai.com/v1"));
            };
        }

        private static String env(String name, String fallback) {
            String value = System.getenv(name);
            return value == null || value.isBlank() ? fallback : value;
        }
    }

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
