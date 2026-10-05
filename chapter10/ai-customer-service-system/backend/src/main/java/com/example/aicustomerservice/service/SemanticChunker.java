package com.example.aicustomerservice.service;

import com.example.aicustomerservice.common.Hashes;
import com.example.aicustomerservice.entity.KnowledgeChunk;
import com.example.aicustomerservice.entity.KnowledgeDocument;
import java.util.ArrayList;
import java.util.List;

/**
 * 简化版语义切分器。
 *
 * 生产里的 chunking 不应该只按固定字符数截断。这里按段落优先切分，
 * 超长段落再按句号切开，保留了“先保证语义完整”的基本原则。
 */
public class SemanticChunker {

    public List<KnowledgeChunk> split(KnowledgeDocument document) {
        List<KnowledgeChunk> chunks = new ArrayList<>();
        String[] paragraphs = document.content().split("\\R\\s*\\R|\\n");
        int index = 0;
        for (String paragraph : paragraphs) {
            String normalized = paragraph.trim();
            if (normalized.isBlank()) {
                continue;
            }
            for (String text : splitLongParagraph(normalized)) {
                String chunkId = document.documentId() + "-C" + index + "-" + Hashes.sha256Short(text);
                chunks.add(new KnowledgeChunk(chunkId, document.documentId(), document.version(), document.title(), text));
                index++;
            }
        }
        return chunks;
    }

    private List<String> splitLongParagraph(String paragraph) {
        if (paragraph.length() <= 180) {
            return List.of(paragraph);
        }
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String sentence : paragraph.split("。")) {
            if (sentence.isBlank()) {
                continue;
            }
            if (current.length() + sentence.length() > 160 && !current.isEmpty()) {
                chunks.add(current.toString());
                current = new StringBuilder();
            }
            current.append(sentence.trim()).append("。");
        }
        if (!current.isEmpty()) {
            chunks.add(current.toString());
        }
        return chunks;
    }
}
