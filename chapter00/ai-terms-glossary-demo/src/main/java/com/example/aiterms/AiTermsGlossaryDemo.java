package com.example.aiterms;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 这个示例不追求连接真实模型，而是把术语表中的概念拆成工程对象。
 *
 * <p>真实项目里，很多问题并不在“模型会不会回答”，而在请求到达模型之前和模型回答之后：
 * 如何选择模型、如何打包上下文、如何检索资料、如何限制工具权限、如何记录 trace、
 * 如何做最小评测门禁。这些对象组合起来，就是一个简化版 AI 应用 harness。
 */
public class AiTermsGlossaryDemo {

    public static void main(String[] args) {
        ProviderCatalog providerCatalog = ProviderCatalog.defaultCatalog();
        ProviderConfig provider = providerCatalog.resolve(System.getProperty("ai.provider", "deepseek"));

        List<KnowledgeDocument> documents = List.of(
                new KnowledgeDocument("doc-001", "Token",
                        "Token 是模型处理文本的基本单位，会影响成本、上下文窗口和响应速度。"),
                new KnowledgeDocument("doc-002", "RAG",
                        "RAG 会先检索知识库，再把相关片段放进上下文，让模型基于资料回答。"),
                new KnowledgeDocument("doc-003", "Guardrails",
                        "Guardrails 用规则、模型或审批流程限制输出和工具动作，避免越权和数据泄露。"),
                new KnowledgeDocument("doc-004", "Observability",
                        "可观测性会记录模型调用、检索、工具执行、延迟、token 和成本，方便排查问题。")
        );

        SimpleRetriever retriever = new SimpleRetriever(documents);
        ContextPacker contextPacker = new ContextPacker(1_000);
        ToolRegistry toolRegistry = ToolRegistry.withDefaultTools();
        Guardrail guardrail = new Guardrail();
        EvalHarness evalHarness = EvalHarness.defaultHarness();
        ChatClient chatClient = new MockChatClient();

        UserRequest request = new UserRequest(
                "req-" + UUID.randomUUID(),
                "我准备做一个企业知识问答系统，怎么理解 RAG、Token 和 Guardrails？",
                "course-demo-user"
        );

        Trace trace = new Trace(request.requestId());
        trace.add("request.accepted", "收到用户问题");

        if (!guardrail.allowRequest(request)) {
            trace.add("guardrail.blocked", "问题里包含敏感信息，停止进入模型");
            trace.print();
            return;
        }

        List<KnowledgeDocument> retrievedDocuments = retriever.search(request.question(), 3);
        trace.add("retriever.search", "检索到 " + retrievedDocuments.size() + " 个片段");

        PromptEnvelope prompt = contextPacker.pack(request, retrievedDocuments, toolRegistry, provider);
        trace.add("context.pack", "估算输入 token: " + prompt.estimatedInputTokens());

        ModelAnswer answer = chatClient.chat(prompt);
        trace.add("model.chat", "provider=" + provider.name() + ", model=" + provider.model());

        EvalResult eval = evalHarness.evaluate(answer);
        trace.add("eval.run", "score=" + eval.score() + ", passed=" + eval.passed());

        System.out.println("=== Provider ===");
        System.out.println(provider);
        System.out.println();
        System.out.println("=== Prompt Envelope ===");
        System.out.println(prompt.renderForDebug());
        System.out.println();
        System.out.println("=== Answer ===");
        System.out.println(answer.text());
        System.out.println();
        System.out.println("=== Eval ===");
        System.out.println(eval);
        System.out.println();
        trace.print();
    }

    /**
     * ProviderConfig 对应“模型供应商配置”。
     *
     * <p>同一套业务代码不应该写死一个模型地址。这里默认 deepseek，
     * 也保留 ollama 和 chatgpt，方便在成本、延迟、隐私和效果之间做模型路由。
     */
    record ProviderConfig(String name, String baseUrl, String model, String apiKeyEnv) {
    }

    static final class ProviderCatalog {
        private final Map<String, ProviderConfig> configs;

        private ProviderCatalog(Map<String, ProviderConfig> configs) {
            this.configs = Map.copyOf(configs);
        }

        static ProviderCatalog defaultCatalog() {
            Map<String, ProviderConfig> configs = new HashMap<>();
            configs.put("deepseek", new ProviderConfig(
                    "deepseek",
                    "https://api.deepseek.com",
                    "deepseek-v4-flash",
                    "DEEPSEEK_API_KEY"
            ));
            configs.put("ollama", new ProviderConfig(
                    "ollama",
                    "http://localhost:11434",
                    "qwen2.5:7b",
                    ""
            ));
            configs.put("chatgpt", new ProviderConfig(
                    "chatgpt",
                    "https://api.openai.com/v1",
                    "gpt-4o-mini",
                    "OPENAI_API_KEY"
            ));
            return new ProviderCatalog(configs);
        }

        ProviderConfig resolve(String providerName) {
            String key = providerName == null ? "deepseek" : providerName.toLowerCase(Locale.ROOT);
            ProviderConfig config = configs.get(key);
            if (config == null) {
                throw new IllegalArgumentException("Unsupported provider: " + providerName + ". Supported: " + configs.keySet());
            }
            return config;
        }
    }

    record UserRequest(String requestId, String question, String userId) {
    }

    record KnowledgeDocument(String id, String title, String content) {
    }

    /**
     * SimpleRetriever 对应“关键词检索 / RAG 里的 retriever”。
     *
     * <p>真实 RAG 会用 embedding、向量数据库和 rerank。这里用关键词打分，
     * 是为了让检索过程足够透明，便于看清“先召回资料，再交给模型”的链路。
     */
    static final class SimpleRetriever {
        private final List<KnowledgeDocument> documents;

        SimpleRetriever(List<KnowledgeDocument> documents) {
            this.documents = List.copyOf(documents);
        }

        List<KnowledgeDocument> search(String query, int topK) {
            Set<String> queryTerms = tokenize(query);
            return documents.stream()
                    .map(document -> new ScoredDocument(document, score(document, queryTerms)))
                    .filter(scored -> scored.score() > 0)
                    .sorted(Comparator.comparingInt(ScoredDocument::score).reversed())
                    .limit(topK)
                    .map(ScoredDocument::document)
                    .toList();
        }

        private int score(KnowledgeDocument document, Set<String> queryTerms) {
            Set<String> documentTerms = tokenize(document.title() + " " + document.content());
            int score = 0;
            for (String term : queryTerms) {
                if (documentTerms.contains(term)) {
                    score++;
                }
            }
            return score;
        }

        private Set<String> tokenize(String text) {
            String normalized = text.toLowerCase(Locale.ROOT)
                    .replaceAll("[^a-z0-9\\u4e00-\\u9fa5]+", " ");
            Set<String> terms = new HashSet<>();
            for (String item : normalized.split("\\s+")) {
                if (!item.isBlank()) {
                    terms.add(item);
                }
            }
            return terms;
        }
    }

    record ScoredDocument(KnowledgeDocument document, int score) {
    }

    /**
     * ContextPacker 对应“上下文工程”。
     *
     * <p>模型真正看到的是一个拼好的上下文包，而不是孤零零的用户问题。
     * 这里会把系统规则、检索片段、工具清单和用户问题组装起来，并做一个粗略 token 估算。
     */
    static final class ContextPacker {
        private final int maxInputTokens;

        ContextPacker(int maxInputTokens) {
            this.maxInputTokens = maxInputTokens;
        }

        PromptEnvelope pack(
                UserRequest request,
                List<KnowledgeDocument> retrievedDocuments,
                ToolRegistry toolRegistry,
                ProviderConfig provider
        ) {
            List<String> contextBlocks = new ArrayList<>();
            for (KnowledgeDocument document : retrievedDocuments) {
                String block = "资料 " + document.id() + "｜" + document.title() + "：" + document.content();
                if (estimateTokens(String.join("\n", contextBlocks) + "\n" + block) <= maxInputTokens) {
                    contextBlocks.add(block);
                }
            }

            String systemPrompt = "你是一个谨慎的 Java AI 项目助手。回答必须基于资料，不能编造。";
            String toolSummary = toolRegistry.describeForPrompt();
            int estimatedTokens = estimateTokens(systemPrompt + toolSummary + contextBlocks + request.question());

            return new PromptEnvelope(provider, systemPrompt, contextBlocks, toolSummary, request.question(), estimatedTokens);
        }

        private int estimateTokens(String text) {
            // 这里只做演示估算：中文和英文混合时，按每 2 个字符约 1 个 token 粗略计算。
            return Math.max(1, text.length() / 2);
        }
    }

    record PromptEnvelope(
            ProviderConfig provider,
            String systemPrompt,
            List<String> contextBlocks,
            String toolSummary,
            String userPrompt,
            int estimatedInputTokens
    ) {
        String renderForDebug() {
            return """
                    system: %s

                    context:
                    %s

                    tools:
                    %s

                    user: %s
                    """.formatted(systemPrompt, String.join("\n", contextBlocks), toolSummary, userPrompt);
        }
    }

    /**
     * ToolRegistry 对应“工具注册表”。
     *
     * <p>工具描述不是写给人看的装饰文本，模型会根据它决定是否调用工具。
     * 所以工具名、参数、权限和危险等级都应该清楚。
     */
    static final class ToolRegistry {
        private final List<ToolSpec> tools;

        private ToolRegistry(List<ToolSpec> tools) {
            this.tools = List.copyOf(tools);
        }

        static ToolRegistry withDefaultTools() {
            return new ToolRegistry(List.of(
                    new ToolSpec("ticket.search", "查询工单摘要", "read", "low"),
                    new ToolSpec("doc.search", "查询知识库文档", "read", "low"),
                    new ToolSpec("email.send", "发送邮件，需要人工确认", "write", "high")
            ));
        }

        String describeForPrompt() {
            StringBuilder builder = new StringBuilder();
            for (ToolSpec tool : tools) {
                builder.append("- ")
                        .append(tool.name())
                        .append(": ")
                        .append(tool.description())
                        .append(", permission=")
                        .append(tool.permission())
                        .append(", risk=")
                        .append(tool.risk())
                        .append("\n");
            }
            return builder.toString();
        }
    }

    record ToolSpec(String name, String description, String permission, String risk) {
    }

    /**
     * Guardrail 对应“安全防护栏”。
     *
     * <p>示例只做了最基础的手机号拦截。生产系统里通常还会有租户权限、
     * 数据分级、敏感动作审批、输出脱敏和审计日志。
     */
    static final class Guardrail {
        private static final Pattern CHINA_MOBILE_PHONE = Pattern.compile("1[3-9]\\d{9}");

        boolean allowRequest(UserRequest request) {
            return !CHINA_MOBILE_PHONE.matcher(request.question()).find();
        }
    }

    interface ChatClient {
        ModelAnswer chat(PromptEnvelope prompt);
    }

    /**
     * MockChatClient 代替真实模型调用。
     *
     * <p>把这里替换成真实 HTTP 客户端时，要注意三个点：
     * 1. 请求和响应都进入 trace；
     * 2. 超时后走 fallback model 或业务兜底；
     * 3. 输出进入 eval 和 guardrail，而不是直接返回给用户。
     */
    static final class MockChatClient implements ChatClient {
        @Override
        public ModelAnswer chat(PromptEnvelope prompt) {
            String text = """
                    RAG 可以理解成“先查资料，再回答”。Token 是模型处理文本和计费的基本单位，决定上下文能放多少内容。
                    Guardrails 则是业务防护栏：模型不确定、资料不足或工具动作有风险时，要回到规则、审批或人工确认。
                    在 Java 工程里，我会把这些拆成 Retriever、ContextPacker、ToolRegistry、Guardrail、EvalHarness 和 Trace。
                    """;
            return new ModelAnswer(text, prompt.estimatedInputTokens(), 128);
        }
    }

    record ModelAnswer(String text, int inputTokens, int outputTokens) {
    }

    /**
     * EvalHarness 对应“评测框架”。
     *
     * <p>这里用关键词检查做最小示例。真实项目会把 golden set、人工评分、
     * LLM-as-Judge、回归阈值和 CI 门禁组合起来。
     */
    static final class EvalHarness {
        private final List<String> requiredKeywords;

        private EvalHarness(List<String> requiredKeywords) {
            this.requiredKeywords = List.copyOf(requiredKeywords);
        }

        static EvalHarness defaultHarness() {
            return new EvalHarness(List.of("RAG", "Token", "Guardrails", "Java"));
        }

        EvalResult evaluate(ModelAnswer answer) {
            long matched = requiredKeywords.stream()
                    .filter(keyword -> answer.text().contains(keyword))
                    .count();
            double score = matched * 1.0 / requiredKeywords.size();
            return new EvalResult(score, score >= 0.75, requiredKeywords);
        }
    }

    record EvalResult(double score, boolean passed, List<String> requiredKeywords) {
    }

    /**
     * Trace 对应一次请求的可观测执行轨迹。
     *
     * <p>线上系统里，每个 span 还会记录耗时、token、错误码、模型名、工具名、用户和租户等字段。
     */
    static final class Trace {
        private final String traceId;
        private final Instant startedAt;
        private final List<TraceSpan> spans = new ArrayList<>();

        Trace(String traceId) {
            this.traceId = Objects.requireNonNull(traceId);
            this.startedAt = Instant.now();
        }

        void add(String name, String detail) {
            spans.add(new TraceSpan(name, detail, Duration.between(startedAt, Instant.now()).toMillis()));
        }

        void print() {
            System.out.println("=== Trace ===");
            System.out.println("traceId=" + traceId);
            for (TraceSpan span : spans) {
                System.out.println(span.offsetMillis() + "ms " + span.name() + " - " + span.detail());
            }
            Optional<TraceSpan> last = spans.stream().reduce((first, second) -> second);
            last.ifPresent(span -> System.out.println("total=" + span.offsetMillis() + "ms"));
        }
    }

    record TraceSpan(String name, String detail, long offsetMillis) {
    }
}
