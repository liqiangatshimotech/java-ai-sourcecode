package com.example.memoryservice;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MemoryServiceFinalDemo {

    public static void main(String[] args) {
        ModelSettings settings = ModelSettings.fromEnv();
        MemoryRepository repository = new InMemoryMemoryRepository();
        MemoryExtractor extractor = new MemoryExtractor();
        MemoryAccessPolicy accessPolicy = new MemoryAccessPolicy();
        MemoryRetentionPolicy retentionPolicy = new MemoryRetentionPolicy(Duration.ofDays(30));
        CustomerSupportMemoryService service = new CustomerSupportMemoryService(repository, extractor, accessPolicy, retentionPolicy);

        MemoryScope scope = new MemoryScope("tenant-1", "user-a", "c-1001");
        service.observe(scope, "以后回答用中文，尽量短一点。");
        service.observe(scope, "帮我查一下订单退款状态。");

        System.out.println("Provider: " + settings.provider().id());
        System.out.println("可召回记忆: " + service.recall(scope));
        service.clear(scope);
        System.out.println("清空后: " + service.recall(scope));
        System.out.println("核心结论: 记忆服务必须有作用域、过期策略、删除能力和污染过滤。");
    }

    static class CustomerSupportMemoryService {
        private final MemoryRepository repository;
        private final MemoryExtractor extractor;
        private final MemoryAccessPolicy accessPolicy;
        private final MemoryRetentionPolicy retentionPolicy;

        CustomerSupportMemoryService(MemoryRepository repository, MemoryExtractor extractor,
                MemoryAccessPolicy accessPolicy, MemoryRetentionPolicy retentionPolicy) {
            this.repository = repository;
            this.extractor = extractor;
            this.accessPolicy = accessPolicy;
            this.retentionPolicy = retentionPolicy;
        }

        void observe(MemoryScope scope, String userMessage) {
            accessPolicy.assertWritable(scope);
            MemoryDecision decision = extractor.extract(userMessage);
            if (!decision.shouldRemember()) {
                System.out.println("跳过记忆: " + decision.reason());
                return;
            }
            repository.save(scope.longTermKey(), new MemoryRecord(
                    decision.memoryType(),
                    decision.normalizedContent(),
                    Instant.now(),
                    retentionPolicy.expiresAt()
            ));
        }

        List<MemoryRecord> recall(MemoryScope scope) {
            accessPolicy.assertReadable(scope);
            return repository.find(scope.longTermKey()).stream()
                    .filter(MemoryRecord::reusable)
                    .toList();
        }

        void clear(MemoryScope scope) {
            accessPolicy.assertWritable(scope);
            repository.delete(scope.longTermKey());
        }
    }

    static class MemoryExtractor {
        MemoryDecision extract(String userMessage) {
            String text = userMessage.trim();
            if (text.contains("忽略之前规则") || text.contains("设为管理员")) {
                return MemoryDecision.skip("疑似 Prompt 注入，不写入长期记忆");
            }
            if (text.contains("订单") || text.contains("退款状态")) {
                return MemoryDecision.skip("订单和退款状态属于实时业务数据");
            }
            if (text.contains("以后") && (text.contains("中文") || text.contains("短"))) {
                return MemoryDecision.remember("preference", "回答用中文，保持简短", "用户表达了稳定偏好");
            }
            return MemoryDecision.skip("没有可复用的长期记忆");
        }
    }

    static class MemoryAccessPolicy {
        void assertReadable(MemoryScope scope) {
            validate(scope);
        }

        void assertWritable(MemoryScope scope) {
            validate(scope);
        }

        private void validate(MemoryScope scope) {
            if (scope.tenantId().isBlank() || scope.userId().isBlank()) {
                throw new IllegalArgumentException("tenantId and userId are required");
            }
        }
    }

    static class MemoryRetentionPolicy {
        private final Duration ttl;

        MemoryRetentionPolicy(Duration ttl) {
            this.ttl = ttl;
        }

        Instant expiresAt() {
            return Instant.now().plus(ttl);
        }
    }

    interface MemoryRepository {
        void save(String key, MemoryRecord record);

        List<MemoryRecord> find(String key);

        void delete(String key);
    }

    static class InMemoryMemoryRepository implements MemoryRepository {
        private final Map<String, List<MemoryRecord>> recordsByKey = new ConcurrentHashMap<>();

        @Override
        public void save(String key, MemoryRecord record) {
            recordsByKey.computeIfAbsent(key, ignored -> new ArrayList<>()).add(record);
        }

        @Override
        public List<MemoryRecord> find(String key) {
            return new ArrayList<>(recordsByKey.getOrDefault(key, List.of()));
        }

        @Override
        public void delete(String key) {
            recordsByKey.remove(key);
        }
    }

    record MemoryScope(String tenantId, String userId, String conversationId) {
        String shortTermKey() {
            return tenantId + ":" + userId + ":" + conversationId;
        }

        String longTermKey() {
            return tenantId + ":" + userId;
        }
    }

    record MemoryDecision(boolean shouldRemember, String memoryType, String normalizedContent, String reason) {
        static MemoryDecision skip(String reason) {
            return new MemoryDecision(false, "none", "", reason);
        }

        static MemoryDecision remember(String type, String content, String reason) {
            return new MemoryDecision(true, type, content, reason);
        }
    }

    record MemoryRecord(String type, String content, Instant createdAt, Instant expiresAt) {
        boolean reusable() {
            return expiresAt == null || expiresAt.isAfter(Instant.now());
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
