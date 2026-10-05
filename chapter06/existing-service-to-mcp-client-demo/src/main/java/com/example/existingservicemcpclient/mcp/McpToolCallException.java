package com.example.existingservicemcpclient.mcp;

public class McpToolCallException extends RuntimeException {

    public McpToolCallException(String message) {
        super(message);
    }

    public McpToolCallException(String message, Throwable cause) {
        super(message, cause);
    }
}
