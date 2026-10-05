package com.example.springairag.rag;

import com.example.springairag.config.RagProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class SimpleTextSplitter {

    private final RagProperties properties;

    public SimpleTextSplitter(RagProperties properties) {
        this.properties = properties;
    }

    public List<DocumentChunk> split(KnowledgeDocument document) {
        int maxChars = Math.max(200, properties.getMaxChunkChars());
        int overlapChars = Math.max(0, Math.min(properties.getChunkOverlapChars(), maxChars / 2));

        List<DocumentChunk> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int chunkNo = 1;

        for (String paragraph : document.content().split("\\n\\s*\\n")) {
            String normalized = paragraph.strip();
            if (normalized.isBlank()) {
                continue;
            }

            if (!current.isEmpty() && current.length() + normalized.length() + 2 > maxChars) {
                chunks.add(toChunk(document, chunkNo++, current.toString()));
                String overlap = tailOverlap(current.toString(), overlapChars);
                current.setLength(0);
                if (!overlap.isBlank()) {
                    current.append(overlap).append("\n\n");
                }
            }

            if (normalized.length() > maxChars) {
                chunkNo = splitLargeParagraph(document, chunks, chunkNo, normalized, maxChars, overlapChars);
                current.setLength(0);
            }
            else {
                current.append(normalized).append("\n\n");
            }
        }

        if (!current.toString().strip().isBlank()) {
            chunks.add(toChunk(document, chunkNo, current.toString()));
        }

        return List.copyOf(chunks);
    }

    private int splitLargeParagraph(
        KnowledgeDocument document,
        List<DocumentChunk> chunks,
        int chunkNo,
        String paragraph,
        int maxChars,
        int overlapChars
    ) {
        int start = 0;
        while (start < paragraph.length()) {
            int end = Math.min(paragraph.length(), start + maxChars);
            chunks.add(toChunk(document, chunkNo++, paragraph.substring(start, end)));
            if (end == paragraph.length()) {
                break;
            }
            start = Math.max(end - overlapChars, start + 1);
        }
        return chunkNo;
    }

    private DocumentChunk toChunk(KnowledgeDocument document, int chunkNo, String content) {
        Map<String, String> metadata = new LinkedHashMap<>(document.metadata());
        metadata.put("chunkNo", Integer.toString(chunkNo));

        return new DocumentChunk(
            document.id() + "#chunk-" + chunkNo,
            document.id(),
            document.title(),
            document.tenantId(),
            document.source(),
            document.tags(),
            content.strip(),
            Map.copyOf(metadata)
        );
    }

    private String tailOverlap(String text, int overlapChars) {
        if (overlapChars <= 0 || text.length() <= overlapChars) {
            return "";
        }
        return text.substring(text.length() - overlapChars).strip();
    }
}
