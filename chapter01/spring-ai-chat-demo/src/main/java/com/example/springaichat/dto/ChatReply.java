package com.example.springaichat.dto;

/**
 * Response body for POST /chat.
 *
 * @param provider actual provider used for this request
 * @param content model-generated answer
 */
public record ChatReply(String provider, String content) {
}
