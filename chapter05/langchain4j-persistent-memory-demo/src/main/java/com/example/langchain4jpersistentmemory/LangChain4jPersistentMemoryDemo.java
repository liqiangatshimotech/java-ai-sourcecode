package com.example.langchain4jpersistentmemory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LangChain4jPersistentMemoryDemo {

    public static void main(String[] args) {
        ModelSettings settings = ModelSettings.fromEnv();
        ChatMemoryStore store = new InMemoryChatMemoryStore();
        RetrievalAugmentor augmentor = query -> List.of("退款政策：到账通常需要 1-3 个工作日。");

        PersistentAssistant assistant = new PersistentAssistant(settings, store, augmentor, false);
        assistant.chat("user-a", "我叫老李，回答要简短。");
        String answer = assistant.chat("user-a", "退款三天没到账，应该先查什么？");

        System.out.println(answer);
        System.out.println("持久化消息: " + store.getMessages("user-a"));
        System.out.println("核心结论: RAG 检索内容默认只作为本次上下文，不写入 ChatMemory。");
    }

    static class PersistentAssistant {
        private final ModelSettings settings;
        private final ChatMemoryStore store;
        private final RetrievalAugmentor retrievalAugmentor;
        private final boolean storeRetrievedContentInChatMemory;

        PersistentAssistant(ModelSettings settings, ChatMemoryStore store,
                RetrievalAugmentor retrievalAugmentor, boolean storeRetrievedContentInChatMemory) {
            this.settings = settings;
            this.store = store;
            this.retrievalAugmentor = retrievalAugmentor;
            this.storeRetrievedContentInChatMemory = storeRetrievedContentInChatMemory;
        }

        String chat(String memoryId, String userMessage) {
            MessageWindowChatMemory memory = new MessageWindowChatMemory(memoryId, store, 10);
            List<String> retrieved = retrievalAugmentor.retrieve(userMessage);
            String modelContext = String.join("\n", retrieved);
            String answer = "[" + settings.provider().id() + "] " + answer(userMessage, modelContext);

            memory.add(ChatMessage.user(userMessage));
            if (storeRetrievedContentInChatMemory && !retrieved.isEmpty()) {
                memory.add(ChatMessage.system("retrieved_context: " + modelContext));
            }
            memory.add(ChatMessage.assistant(answer));
            return answer;
        }

        private String answer(String userMessage, String modelContext) {
            if (userMessage.contains("退款") && !modelContext.isBlank()) {
                return "先核对支付渠道流水，再按政策确认到账窗口。";
            }
            return "已处理。";
        }
    }

    interface RetrievalAugmentor {
        List<String> retrieve(String query);
    }

    interface ChatMemoryStore {
        List<ChatMessage> getMessages(Object memoryId);

        void updateMessages(Object memoryId, List<ChatMessage> messages);

        void deleteMessages(Object memoryId);
    }

    static class InMemoryChatMemoryStore implements ChatMemoryStore {
        private final Map<Object, List<ChatMessage>> messagesByMemoryId = new HashMap<>();

        @Override
        public List<ChatMessage> getMessages(Object memoryId) {
            return new ArrayList<>(messagesByMemoryId.getOrDefault(memoryId, List.of()));
        }

        @Override
        public void updateMessages(Object memoryId, List<ChatMessage> messages) {
            messagesByMemoryId.put(memoryId, new ArrayList<>(messages));
        }

        @Override
        public void deleteMessages(Object memoryId) {
            messagesByMemoryId.remove(memoryId);
        }
    }

    static class MessageWindowChatMemory {
        private final Object memoryId;
        private final ChatMemoryStore store;
        private final int maxMessages;

        MessageWindowChatMemory(Object memoryId, ChatMemoryStore store, int maxMessages) {
            this.memoryId = memoryId;
            this.store = store;
            this.maxMessages = maxMessages;
        }

        void add(ChatMessage message) {
            ArrayDeque<ChatMessage> messages = new ArrayDeque<>(store.getMessages(memoryId));
            messages.addLast(message);
            while (messages.size() > maxMessages) {
                messages.removeFirst();
            }
            store.updateMessages(memoryId, new ArrayList<>(messages));
        }
    }

    record ChatMessage(String role, String content) {
        static ChatMessage user(String content) {
            return new ChatMessage("user", content);
        }

        static ChatMessage assistant(String content) {
            return new ChatMessage("assistant", content);
        }

        static ChatMessage system(String content) {
            return new ChatMessage("system", content);
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
