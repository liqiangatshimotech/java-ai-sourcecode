package com.example.memoryconcepts;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MemoryConceptDemo {

    public static void main(String[] args) {
        ModelSettings settings = ModelSettings.fromEnv();

        List<ChatTurn> history = new ArrayList<>();
        history.add(ChatTurn.user("我是华东区售后负责人，今天主要处理退款工单。"));
        history.add(ChatTurn.assistant("收到，后续建议会优先围绕华东区售后和退款场景。"));
        history.add(ChatTurn.user("客户说退款三天还没到账，我应该先查什么？"));

        List<MemoryEntry> memory = List.of(
                new MemoryEntry("profile", "用户负责华东区售后"),
                new MemoryEntry("task_context", "当前对话围绕退款工单处理")
        );

        SupportSessionState state = new SupportSessionState("u-1001", "华东区", "T-20260609-001");

        System.out.println("Provider: " + settings.provider().id());
        System.out.println("History 条数: " + history.size());
        System.out.println("Memory 条数: " + memory.size());
        System.out.println("业务状态: " + state);
        System.out.println("核心结论: History 用于审计，Memory 用于模型上下文，业务状态回到业务系统。");
    }

    record ChatTurn(String role, String content, Instant createdAt) {
        static ChatTurn user(String content) {
            return new ChatTurn("user", content, Instant.now());
        }

        static ChatTurn assistant(String content) {
            return new ChatTurn("assistant", content, Instant.now());
        }
    }

    record MemoryEntry(String type, String content) {
    }

    record SupportSessionState(String userId, String region, String activeTicketId) {
    }

    record ModelSettings(AiProvider provider, String model, String baseUrl) {
        static ModelSettings fromEnv() {
            AiProvider provider = AiProvider.parseOrDefault(System.getenv("APP_AI_DEFAULT_PROVIDER"));
            return switch (provider) {
                case DEEPSEEK -> new ModelSettings(provider,
                        env("DEEPSEEK_MODEL", "deepseek-v4-flash"),
                        env("DEEPSEEK_BASE_URL", "https://api.deepseek.com"));
                case OLLAMA -> new ModelSettings(provider,
                        env("OLLAMA_MODEL", "qwen2.5:7b"),
                        env("OLLAMA_BASE_URL", "http://localhost:11434"));
                case CHATGPT -> new ModelSettings(provider,
                        env("CHATGPT_MODEL", "gpt-4.1-mini"),
                        env("CHATGPT_BASE_URL", "https://api.openai.com/v1"));
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
