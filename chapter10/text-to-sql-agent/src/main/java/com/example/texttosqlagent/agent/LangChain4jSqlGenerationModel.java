package com.example.texttosqlagent.agent;

import com.example.texttosqlagent.config.ModelSettings;
import com.example.texttosqlagent.schema.MetricDictionary;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;

/**
 * 真实模型路径：用 LangChain4j 调 OpenAI 兼容协议的大模型生成 SQL。
 *
 * DeepSeek、Ollama、ChatGPT 三家都提供 OpenAI 兼容端点，
 * 所以一个 OpenAiChatModel 通过换 baseUrl/apiKey/modelName 就能全部覆盖：
 * - DeepSeek：baseUrl=https://api.deepseek.com，key 来自 DEEPSEEK_API_KEY；
 * - Ollama：baseUrl=http://localhost:11434/v1，本地服务不需要真 key；
 * - ChatGPT：baseUrl=https://api.openai.com/v1，key 来自 OPENAI_API_KEY。
 *
 * 这个类只在没有 API Key 的离线环境下不会被装配（见 AgentConfiguration），
 * 因此测试可以完全离线运行。
 */
public class LangChain4jSqlGenerationModel implements SqlGenerationModel {

    private final ChatModel chatModel;
    private final MetricDictionary metricDictionary;

    public LangChain4jSqlGenerationModel(ModelSettings settings, MetricDictionary metricDictionary) {
        this.metricDictionary = metricDictionary;
        // Ollama 不校验 key，但 OpenAI 客户端要求非空，给个占位值
        String apiKey = settings.apiKey() == null || settings.apiKey().isBlank()
            ? "not-required" : settings.apiKey();
        this.chatModel = OpenAiChatModel.builder()
            .baseUrl(settings.baseUrl())
            .apiKey(apiKey)
            .modelName(settings.modelName())
            .temperature(settings.temperature())
            .build();
    }

    /**
     * 调模型生成 SQL。
     * system prompt 里写死硬性规则（只读、只用给定表、按问题保留 LIMIT、只输出 SQL），
     * user message 带上 Schema Linking 选出来的 schema 上下文和用户问题。
     */
    @Override
    public String generateSql(String question, String schemaContext) {
        String systemPrompt = """
            你是一个严谨的 Text-to-SQL 助手，负责把中文数据分析问题翻译成 H2 数据库的 SQL。
            硬性规则（必须全部遵守）：
            1. 只能生成 SELECT 查询（或以 WITH 开头的 CTE 查询），禁止任何写操作；
            2. 只能使用下面给出的表和字段，禁止编造不存在的表或字段；
            3. 统计金额时必须排除 status = 'REFUNDED' 的退款订单；
            4. 用户明确要求前 N 行时使用 LIMIT N，否则不自行设置行数，执行护栏会统一限制；
            5. 只输出 SQL 本身，不要输出解释，不要用 Markdown 代码围栏；
            6. 如果给定表无法回答这个问题，只输出 CANNOT_ANSWER。

            %s
            """.formatted(metricDictionary.toPromptFragment());

        String userPrompt = schemaContext + "\n【用户问题】\n" + question;

        ChatResponse response = chatModel.chat(ChatRequest.builder()
            .messages(SystemMessage.from(systemPrompt), UserMessage.from(userPrompt))
            .build());
        return extractSql(response.aiMessage().text());
    }

    /**
     * 只剥掉包住整个响应的 Markdown 围栏；完整 SQL 交给 AST 护栏检查。
     * 不能按分号取第一条：那会隐藏第二条危险语句，也会损坏含分号的字符串。
     */
    static String extractSql(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) return CANNOT_ANSWER;
        String text = rawResponse.trim();
        if (text.equals(CANNOT_ANSWER)) return CANNOT_ANSWER;
        if (text.startsWith("```sql\n") && text.endsWith("```")) {
            text = text.substring(7, text.length() - 3).trim();
        } else if (text.startsWith("```\n") && text.endsWith("```")) {
            text = text.substring(4, text.length() - 3).trim();
        }
        return text;
    }
}
