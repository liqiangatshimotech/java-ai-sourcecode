package com.example.aicustomerservice.service;

import com.example.aicustomerservice.common.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.nio.ByteBuffer;
import java.util.Map;

/**
 * 独立的真实 Embedding 适配器。聊天默认 DeepSeek，向量模型须另外配置：
 * 可接 OpenAI /v1/embeddings，或 Ollama 的 OpenAI 兼容 /v1/embeddings。
 * 不把 deepseek-v4-flash 当作向量模型，也不在 HTTP 失败时悄悄退回 Hashing。
 */
public class OpenAiCompatibleEmbeddingService implements EmbeddingService {
    private final String baseUrl, apiKey, model;
    private final int dimensions;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper json = new ObjectMapper();
    public OpenAiCompatibleEmbeddingService(String baseUrl, String apiKey, String model, int dimensions) {
        if (baseUrl.isBlank() || model.isBlank() || dimensions < 1)
            throw new IllegalArgumentException("真实 Embedding 必须配置地址、模型和预期维度");
        this.baseUrl=baseUrl; this.apiKey=apiKey; this.model=model; this.dimensions=dimensions;
    }
    @Override public String modelName() { return model; }
    @Override public int dimensions() { return dimensions; }
    @Override public float[] embed(String text) {
        try {
            var builder = HttpRequest.newBuilder(URI.create(baseUrl.replaceAll("/+$", "") + "/embeddings"))
                .timeout(Duration.ofSeconds(30)).header("Content-Type", "application/json");
            if (!apiKey.isBlank()) builder.header("Authorization", "Bearer " + apiKey);
            var response = http.send(builder.POST(HttpRequest.BodyPublishers.ofString(
                json.writeValueAsString(Map.of("model", model, "input", text)))).build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw new BusinessException("EMBEDDING_FAILED", "向量服务 HTTP " + response.statusCode());
            var values=json.readTree(response.body()).path("data").path(0).path("embedding");
            if (!values.isArray() || values.size()!=dimensions)
                throw new BusinessException("EMBEDDING_DIMENSION_MISMATCH", "向量维度与配置不一致，禁止混用不同向量空间");
            float[] result=new float[dimensions];
            double norm=0;
            for (int i=0;i<dimensions;i++) {
                if (!values.get(i).isNumber()) throw new BusinessException("EMBEDDING_INVALID", "向量包含非数值");
                result[i]=(float)values.get(i).asDouble();
                if (!Float.isFinite(result[i])) throw new BusinessException("EMBEDDING_INVALID", "向量包含非有限数");
                norm+=result[i]*result[i];
            }
            if (norm==0) throw new BusinessException("EMBEDDING_INVALID", "零向量不能用于余弦检索");
            return result;
        } catch (java.io.IOException e) {
            throw new BusinessException("EMBEDDING_FAILED", "向量服务连接或响应格式错误");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("EMBEDDING_INTERRUPTED", "向量调用已中断");
        }
    }
    @Override public byte[] embedAsBytes(String text) {
        ByteBuffer bytes=ByteBuffer.allocate(dimensions*Float.BYTES);
        for(float value:embed(text)) bytes.putFloat(value);
        return bytes.array();
    }
}
