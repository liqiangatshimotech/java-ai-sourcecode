package com.example.langgraph4j.aftersales.service;

/** 图运行时异常的应用层包装，避免 Web 层泄露框架堆栈。 */
public class WorkflowExecutionException extends RuntimeException {
    public WorkflowExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
