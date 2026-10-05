package com.example.springairag.rag;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class MarkdownKnowledgeDocumentLoader {

    private static final String LOCATION = "classpath*:knowledge-base/*.md";

    private final ResourcePatternResolver resolver;

    public MarkdownKnowledgeDocumentLoader() {
        this(new PathMatchingResourcePatternResolver());
    }

    MarkdownKnowledgeDocumentLoader(ResourcePatternResolver resolver) {
        this.resolver = resolver;
    }

    public List<KnowledgeDocument> loadDocuments() {
        try {
            Resource[] resources = resolver.getResources(LOCATION);
            Arrays.sort(resources, Comparator.comparing(resource -> safeFilename(resource.getFilename())));

            List<KnowledgeDocument> documents = new ArrayList<>();
            for (Resource resource : resources) {
                documents.add(parse(resource));
            }
            return List.copyOf(documents);
        }
        catch (IOException ex) {
            throw new UncheckedIOException("Failed to load knowledge documents from " + LOCATION, ex);
        }
    }

    private KnowledgeDocument parse(Resource resource) {
        try {
            String filename = safeFilename(resource.getFilename());
            String raw = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8)
                .replace("\r\n", "\n");

            ParsedMarkdown parsed = splitFrontMatter(raw);
            Map<String, String> metadata = parsed.metadata();
            String id = metadata.getOrDefault("id", filename.replaceFirst("\\.md$", ""));
            String title = metadata.getOrDefault("title", id);
            String tenantId = metadata.getOrDefault("tenantId", "default");
            String source = metadata.getOrDefault("source", filename);
            List<String> tags = parseTags(metadata.getOrDefault("tags", ""));

            return new KnowledgeDocument(
                id,
                title,
                tenantId,
                source,
                tags,
                parsed.body(),
                metadata
            );
        }
        catch (IOException ex) {
            throw new UncheckedIOException("Failed to parse " + resource.getFilename(), ex);
        }
    }

    private ParsedMarkdown splitFrontMatter(String raw) {
        if (!raw.startsWith("---\n")) {
            return new ParsedMarkdown(Map.of(), raw.strip());
        }

        int end = raw.indexOf("\n---\n", 4);
        if (end < 0) {
            return new ParsedMarkdown(Map.of(), raw.strip());
        }

        String header = raw.substring(4, end);
        String body = raw.substring(end + "\n---\n".length()).strip();
        Map<String, String> metadata = new LinkedHashMap<>();

        for (String line : header.split("\n")) {
            int colon = line.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            String key = line.substring(0, colon).trim();
            String value = line.substring(colon + 1).trim();
            metadata.put(key, value);
        }

        return new ParsedMarkdown(Map.copyOf(metadata), body);
    }

    private List<String> parseTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }
        return Arrays.stream(tags.split(","))
            .map(String::trim)
            .filter(tag -> !tag.isBlank())
            .toList();
    }

    private String safeFilename(String filename) {
        return filename == null ? "unknown.md" : filename;
    }

    private record ParsedMarkdown(Map<String, String> metadata, String body) {
    }
}
