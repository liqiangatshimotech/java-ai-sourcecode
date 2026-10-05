package com.example.aicustomerservice.service;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.CRC32;

/**
 * 本地 deterministic embedding。
 *
 * 生产环境可以替换为真实 embedding 模型；这里先保证 pgvector 检索链路完整可运行。
 */
public class HashingEmbeddingService implements EmbeddingService {

    private static final List<String> DOMAIN_KEYWORDS = List.of(
        "退款", "退货", "物流", "发票", "订单", "售后", "企业", "vip", "超时", "工单",
        "审核", "拦截", "补发", "客户", "专票", "税号", "仓库"
    );

    private final int dimensions;

    public HashingEmbeddingService(int dimensions) {
        if (dimensions < 16) {
            throw new IllegalArgumentException("embedding dimensions must be at least 16");
        }
        this.dimensions = dimensions;
    }

    @Override
    public String modelName() {
        return "local-hashing-embedding-" + dimensions;
    }

    @Override
    public int dimensions() {
        return dimensions;
    }

    @Override
    public float[] embed(String text) {
        float[] vector = new float[dimensions];
        for (String token : tokenize(text)) {
            int index = Math.floorMod(hash(token), dimensions);
            vector[index] += weight(token);
        }
        normalize(vector);
        return vector;
    }

    @Override
    public byte[] embedAsBytes(String text) {
        ByteBuffer buffer = ByteBuffer.allocate(dimensions * Float.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        for (float value : embed(text)) {
            buffer.putFloat(value);
        }
        return buffer.array();
    }

    private Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }

        String normalized = text.toLowerCase(Locale.ROOT)
            .replaceAll("[，。！？；：、,.!?;:()（）\\[\\]{}]", " ");
        Set<String> tokens = new LinkedHashSet<>();
        for (String token : normalized.split("\\s+")) {
            if (!token.isBlank()) {
                tokens.add(token);
            }
        }

        String compact = normalized.replaceAll("\\s+", "");
        for (int size : List.of(2, 3)) {
            for (int i = 0; i + size <= compact.length(); i++) {
                tokens.add(compact.substring(i, i + size));
            }
        }

        List<String> lowerKeywords = new ArrayList<>(DOMAIN_KEYWORDS);
        for (String keyword : lowerKeywords) {
            if (normalized.contains(keyword)) {
                tokens.add(keyword);
            }
        }
        tokens.remove("");
        return tokens;
    }

    private int hash(String token) {
        CRC32 crc32 = new CRC32();
        crc32.update(token.getBytes(StandardCharsets.UTF_8));
        return (int) crc32.getValue();
    }

    private float weight(String token) {
        return DOMAIN_KEYWORDS.contains(token) ? 2.5f : 1.0f;
    }

    private void normalize(float[] vector) {
        double sum = 0;
        for (float value : vector) {
            sum += value * value;
        }
        if (sum == 0) {
            return;
        }
        float length = (float) Math.sqrt(sum);
        for (int i = 0; i < vector.length; i++) {
            vector[i] = vector[i] / length;
        }
    }
}
