package com.example.springaichat.exception;

import com.example.springaichat.config.AiProvider;

/**
 * Raised when a request selects a provider that is supported by code but not configured.
 */
public class ProviderNotConfiguredException extends RuntimeException {

    public ProviderNotConfiguredException(AiProvider provider) {
        super("provider " + provider.name().toLowerCase()
                + " is not configured. For OpenAI, set OPENAI_API_KEY first.");
    }
}
