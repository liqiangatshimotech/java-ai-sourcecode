package com.example.aicustomerservice.entity;

public enum EnterpriseProblemType {
    KNOWLEDGE_RECALL_LOW("8.1", "知识库召回不准"),
    HALLUCINATION_RISK("8.2", "AI 胡说"),
    CONTEXT_TOO_LONG("8.3", "上下文过长"),
    HIGH_CONCURRENCY_LATENCY("8.4", "高并发延迟"),
    KNOWLEDGE_VERSION_DRIFT("8.5", "知识库更新不一致"),
    DUPLICATE_TICKET("8.6", "工单重复生成");

    private final String section;
    private final String title;

    EnterpriseProblemType(String section, String title) {
        this.section = section;
        this.title = title;
    }

    public String section() {
        return section;
    }

    public String title() {
        return title;
    }
}
