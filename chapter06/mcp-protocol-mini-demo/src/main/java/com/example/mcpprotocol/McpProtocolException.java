package com.example.mcpprotocol;

final class McpProtocolException extends RuntimeException {

    private final int code;

    McpProtocolException(int code, String message) {
        super(message);
        this.code = code;
    }

    int code() {
        return code;
    }
}
