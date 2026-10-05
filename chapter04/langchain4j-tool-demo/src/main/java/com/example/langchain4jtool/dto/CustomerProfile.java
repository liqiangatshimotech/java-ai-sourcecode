package com.example.langchain4jtool.dto;

public record CustomerProfile(
    String customerId,
    String name,
    String level,
    String owner,
    String latestNote
) {
}
