package com.example.springaitool.dto;

public record CustomerProfile(
    String customerId,
    String name,
    String level,
    String owner,
    String latestNote
) {
}
