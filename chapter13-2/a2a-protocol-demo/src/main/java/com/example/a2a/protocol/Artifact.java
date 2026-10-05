package com.example.a2a.protocol;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Task 产出的结果。
 *
 * <p>A2A 里 Message 偏沟通，Artifact 偏结果。报告、文件、结构化 JSON 都应该以 Artifact 形式返回。
 */
public record Artifact(
        String artifactId,
        String name,
        List<Part> parts,
        Map<String, Object> metadata) {

    public static Artifact text(String name, String text) {
        return new Artifact(UUID.randomUUID().toString(), name, List.of(Part.text(text)), null);
    }

    public String firstText() {
        if (parts == null) {
            return "";
        }
        return parts.stream()
                .map(Part::text)
                .filter(text -> text != null && !text.isBlank())
                .findFirst()
                .orElse("");
    }
}
