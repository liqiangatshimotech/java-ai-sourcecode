package com.example.texttosqlagent.web;

import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 统一异常处理：参数校验失败（比如空问题）返回 400 和统一错误体。
 *
 * 不让框架默认的 500/堆栈信息直接暴露给调用方——
 * 错误响应也是 API 契约的一部分。
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /** 统一错误体：状态码 + 人类可读的错误说明 */
    public record ErrorBody(int status, String message) {
    }

    /** @Valid 校验失败：把每个字段的校验消息拼成一句话返回 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorBody> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + " " + error.getDefaultMessage())
            .collect(Collectors.joining("；"));
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorBody(HttpStatus.BAD_REQUEST.value(), message));
    }
}
