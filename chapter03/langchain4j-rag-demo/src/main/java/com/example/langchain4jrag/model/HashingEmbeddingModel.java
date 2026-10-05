package com.example.langchain4jrag.model;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HashingEmbeddingModel implements EmbeddingModel {

    private final int dimensions;

    public HashingEmbeddingModel() {
        this(384);
    }

    public HashingEmbeddingModel(int dimensions) {
        this.dimensions = Math.max(64, dimensions);
    }

    @Override
    public Response<List<Embedding>> embedAll(List<TextSegment> textSegments) {
        return Response.from(textSegments.stream()
            .map(segment -> Embedding.from(embedText(segment.text())))
            .toList());
    }

    @Override
    public int dimension() {
        return dimensions;
    }

    @Override
    public String modelName() {
        return "local-hashing-embedding";
    }

    private float[] embedText(String text) {
        float[] vector = new float[dimensions];
        for (String token : tokens(text)) {
            int hash = stableHash(token);
            int index = Math.floorMod(hash, dimensions);
            vector[index] += hash >= 0 ? 1.0f : -1.0f;
        }
        normalize(vector);
        return vector;
    }

    private List<String> tokens(String text) {
        String normalized = text.toLowerCase(Locale.ROOT);
        List<String> tokens = new ArrayList<>();
        StringBuilder ascii = new StringBuilder();

        normalized.codePoints().forEach(codePoint -> {
            if (isAsciiWord(codePoint)) {
                ascii.appendCodePoint(codePoint);
                return;
            }

            flushAsciiToken(ascii, tokens);
            if (Character.isWhitespace(codePoint) || Character.isISOControl(codePoint)) {
                return;
            }
            tokens.add(new String(Character.toChars(codePoint)));
        });
        flushAsciiToken(ascii, tokens);

        for (int i = 0; i < normalized.length() - 1; i++) {
            char left = normalized.charAt(i);
            char right = normalized.charAt(i + 1);
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

    private boolean isAsciiWord(int codePoint) {
        return codePoint < 128 && Character.isLetterOrDigit(codePoint);
    }

    private boolean isCjk(char ch) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(ch);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
            || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
            || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A;
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
}
