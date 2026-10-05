package com.example.springairag.rag;

import com.example.springairag.config.RagProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class HashingEmbeddingModel {

    private final RagProperties properties;

    public HashingEmbeddingModel(RagProperties properties) {
        this.properties = properties;
    }

    public double[] embed(String text) {
        int dimensions = Math.max(64, properties.getEmbeddingDimensions());
        double[] vector = new double[dimensions];
        List<String> tokens = tokenize(text);

        for (String token : tokens) {
            vector[Math.floorMod(token.hashCode(), dimensions)] += 1.0;
        }

        for (int i = 0; i + 1 < tokens.size(); i++) {
            String bigram = tokens.get(i) + "_" + tokens.get(i + 1);
            vector[Math.floorMod(bigram.hashCode(), dimensions)] += 0.7;
        }

        normalize(vector);
        return vector;
    }

    private List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        StringBuilder ascii = new StringBuilder();

        for (int offset = 0; offset < text.length();) {
            int codePoint = text.codePointAt(offset);
            offset += Character.charCount(codePoint);

            if (isAsciiLetterOrDigit(codePoint)) {
                ascii.appendCodePoint(Character.toLowerCase(codePoint));
                continue;
            }

            flushAsciiToken(tokens, ascii);

            if (Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN) {
                tokens.add(new String(Character.toChars(codePoint)));
            }
        }

        flushAsciiToken(tokens, ascii);
        return tokens;
    }

    private boolean isAsciiLetterOrDigit(int codePoint) {
        return codePoint < 128 && Character.isLetterOrDigit(codePoint);
    }

    private void flushAsciiToken(List<String> tokens, StringBuilder ascii) {
        if (!ascii.isEmpty()) {
            tokens.add(ascii.toString().toLowerCase(Locale.ROOT));
            ascii.setLength(0);
        }
    }

    private void normalize(double[] vector) {
        double sum = 0.0;
        for (double value : vector) {
            sum += value * value;
        }
        if (sum == 0.0) {
            return;
        }
        double norm = Math.sqrt(sum);
        for (int i = 0; i < vector.length; i++) {
            vector[i] = vector[i] / norm;
        }
    }
}
