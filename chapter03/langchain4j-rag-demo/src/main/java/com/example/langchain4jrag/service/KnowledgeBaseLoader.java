package com.example.langchain4jrag.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.loader.ClassPathDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class KnowledgeBaseLoader {

    public List<Document> load() {
        List<Document> rawDocuments = ClassPathDocumentLoader.loadDocumentsRecursively(
            "knowledge-base",
            new TextDocumentParser()
        );

        List<Document> documents = new ArrayList<>();
        for (Document rawDocument : rawDocuments) {
            ParsedMarkdown parsed = parseFrontMatter(rawDocument.text());
            Metadata metadata = rawDocument.metadata().copy();
            parsed.metadata().forEach(metadata::put);
            documents.add(Document.from(parsed.body(), metadata));
        }
        return List.copyOf(documents);
    }

    private ParsedMarkdown parseFrontMatter(String raw) {
        if (!raw.startsWith("---")) {
            return new ParsedMarkdown(Map.of(), raw.strip());
        }

        String[] lines = raw.split("\\R");
        Map<String, String> metadata = new LinkedHashMap<>();
        int bodyStart = 0;

        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].strip();
            if ("---".equals(line)) {
                bodyStart = i + 1;
                break;
            }
            int separator = line.indexOf(':');
            if (separator > 0) {
                metadata.put(
                    line.substring(0, separator).strip(),
                    line.substring(separator + 1).strip()
                );
            }
        }

        StringBuilder body = new StringBuilder();
        for (int i = bodyStart; i < lines.length; i++) {
            body.append(lines[i]).append('\n');
        }
        return new ParsedMarkdown(Map.copyOf(metadata), body.toString().strip());
    }

    private record ParsedMarkdown(Map<String, String> metadata, String body) {
    }
}
