package com.example.langchain4jchat.exception;

import com.example.langchain4jchat.dto.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class, WebExchangeBindException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleBadRequest(Exception ex) {
        return ApiErrorResponse.of("bad_request", ex.getMessage());
    }

    @ExceptionHandler(ProviderNotConfiguredException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ApiErrorResponse handleProviderNotConfigured(ProviderNotConfiguredException ex) {
        return ApiErrorResponse.of("provider_not_configured", ex.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ApiErrorResponse handleProviderFailure(RuntimeException ex) {
        return ApiErrorResponse.of("provider_failure", "AI provider call failed: " + ex.getMessage());
    }
}
