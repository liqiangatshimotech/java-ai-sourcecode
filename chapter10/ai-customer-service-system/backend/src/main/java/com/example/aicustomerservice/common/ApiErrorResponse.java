package com.example.aicustomerservice.common;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
    String code,
    String message,
    List<String> details,
    Instant timestamp
) {
    public static ApiErrorResponse of(String code, String message, List<String> details) {
        return new ApiErrorResponse(code, message, details, Instant.now());
    }
}
