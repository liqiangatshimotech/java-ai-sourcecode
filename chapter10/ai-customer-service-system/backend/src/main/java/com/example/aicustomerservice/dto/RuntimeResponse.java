package com.example.aicustomerservice.dto;

import com.example.aicustomerservice.ai.ModelSettings;

public record RuntimeResponse(
    String provider,
    String model,
    String baseUrl,
    String storage,
    boolean configured,
    String configurationMessage
) {
    public static RuntimeResponse from(ModelSettings settings, String storage) {
        return new RuntimeResponse(
            settings.provider().name(),
            settings.modelName(),
            settings.baseUrl(),
            storage,
            settings.configuredForGeneration(),
            settings.configurationMessage()
        );
    }
}
