package com.example.aicustomerservice.entity;

public record ModelCallResult(
    String answer,
    ModelTier tier,
    boolean queued,
    int queueDepth,
    com.example.aicustomerservice.ai.ModelCompletion completion
) {
}
