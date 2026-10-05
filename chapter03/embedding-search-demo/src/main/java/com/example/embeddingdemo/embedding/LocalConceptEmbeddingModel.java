package com.example.embeddingdemo.embedding;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 本地概念版 EmbeddingModel。
 *
 * <p>它不是生产模型，而是一个教学用的确定性模型：把少量业务同义词映射到同一个概念维度。
 * 这样即使没有 API Key、没有 Ollama，也能看清 Embedding 检索为什么能跨过关键词字面差异。
 */
public final class LocalConceptEmbeddingModel implements EmbeddingModel {

    private static final int DIMENSIONS = 64;

    private static final List<Concept> CONCEPTS = List.of(
        new Concept(0, List.of("发票", "开票", "票据", "凭证", "报销凭证")),
        new Concept(1, List.of("抬头", "公司名称", "纳税人识别号")),
        new Concept(2, List.of("错误", "填错", "写错", "不对", "更正", "重开", "重新开")),
        new Concept(3, List.of("退款", "退费", "原路退回", "到账", "钱退回")),
        new Concept(4, List.of("物流", "快递", "收货", "地址", "出库", "改派")),
        new Concept(5, List.of("登录", "登陆", "进不去", "密码", "验证码", "账号"))
    );

    @Override
    public Response<List<Embedding>> embedAll(List<TextSegment> textSegments) {
        return Response.from(textSegments.stream()
            .map(segment -> Embedding.from(embedText(segment.text())))
            .toList());
    }

    @Override
    public int dimension() {
        return DIMENSIONS;
    }

    @Override
    public String modelName() {
        return "local-concept-embedding";
    }

    private float[] embedText(String text) {
        String normalized = normalize(text);
        float[] vector = new float[DIMENSIONS];

        /*
         * 概念维度：把“发票、开票、票据、报销凭证”压到同一个方向。
         * 真实 Embedding 模型会从大量语料里学到这种关系，这里用词表手动模拟。
         */
        for (Concept concept : CONCEPTS) {
            for (String phrase : concept.phrases()) {
                if (normalized.contains(phrase)) {
                    vector[concept.dimension()] += 2.0f;
                }
            }
        }

        /*
         * 残差维度：保留少量字面信息。
         * 如果两个文本没有命中同义词词表，仍然可以靠共同词语获得一点分数。
         */
        for (String token : lexicalTokens(normalized)) {
            int index = 8 + Math.floorMod(stableHash(token), DIMENSIONS - 8);
            vector[index] += 0.2f;
        }

        normalize(vector);
        return vector;
    }

    private List<String> lexicalTokens(String text) {
        List<String> tokens = new ArrayList<>();
        StringBuilder ascii = new StringBuilder();

        text.codePoints().forEach(codePoint -> {
            if (codePoint < 128 && Character.isLetterOrDigit(codePoint)) {
                ascii.appendCodePoint(codePoint);
                return;
            }

            flushAsciiToken(ascii, tokens);
            if (!Character.isWhitespace(codePoint) && !Character.isISOControl(codePoint)) {
                tokens.add(new String(Character.toChars(codePoint)));
            }
        });
        flushAsciiToken(ascii, tokens);

        for (int i = 0; i < text.length() - 1; i++) {
            char left = text.charAt(i);
            char right = text.charAt(i + 1);
            if (isCjk(left) && isCjk(right)) {
                tokens.add("" + left + right);
            }
        }
        return tokens;
    }

    private void flushAsciiToken(StringBuilder ascii, List<String> tokens) {
        if (!ascii.isEmpty()) {
            tokens.add(ascii.toString());
            ascii.setLength(0);
        }
    }

    private boolean isCjk(char ch) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(ch);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
            || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
            || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A;
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT);
    }

    private int stableHash(String token) {
        int hash = 0x811c9dc5;
        for (byte b : token.getBytes(StandardCharsets.UTF_8)) {
            hash ^= b;
            hash *= 0x01000193;
        }
        return hash;
    }

    private void normalize(float[] vector) {
        double norm = 0.0;
        for (float value : vector) {
            norm += value * value;
        }
        if (norm == 0.0) {
            return;
        }

        float scale = (float) (1.0 / Math.sqrt(norm));
        for (int i = 0; i < vector.length; i++) {
            vector[i] *= scale;
        }
    }

    private record Concept(int dimension, List<String> phrases) {
    }
}
