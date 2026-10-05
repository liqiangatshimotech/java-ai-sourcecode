package com.example.supportticketcopilot.exception;

import com.example.supportticketcopilot.dto.ApiErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(WebExchangeBindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiErrorResponse handleBodyValidation(WebExchangeBindException ex, ServerWebExchange exchange) {
        String message = ex.getFieldErrors()
            .stream()
            .map(error -> error.getField() + " " + error.getDefaultMessage())
            .collect(Collectors.joining("; "));

        return error(HttpStatus.BAD_REQUEST, message, exchange);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiErrorResponse handleConstraintViolation(ConstraintViolationException ex, ServerWebExchange exchange) {
        String message = ex.getConstraintViolations()
            .stream()
            .map(this::formatViolation)
            .collect(Collectors.joining("; "));

        return error(HttpStatus.BAD_REQUEST, message, exchange);
    }

    @ExceptionHandler({IllegalArgumentException.class, ServerWebInputException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiErrorResponse handleBadRequest(Exception ex, ServerWebExchange exchange) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage(), exchange);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    ApiErrorResponse handleUnexpected(Exception ex, ServerWebExchange exchange) {
        log.error("Unexpected API error", ex);
        return error(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal server error. Check server logs for details.",
            exchange
        );
    }

    private String formatViolation(ConstraintViolation<?> violation) {
        return violation.getPropertyPath() + " " + violation.getMessage();
    }

    private ApiErrorResponse error(HttpStatus status, String message, ServerWebExchange exchange) {
        return new ApiErrorResponse(
            Instant.now(),
            status.value(),
            status.getReasonPhrase(),
            message,
            exchange.getRequest().getPath().value()
        );
    }
}
