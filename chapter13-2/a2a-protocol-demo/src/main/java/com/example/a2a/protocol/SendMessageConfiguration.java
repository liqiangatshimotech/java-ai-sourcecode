package com.example.a2a.protocol;

import java.util.List;

/**
 * message:send 的配置。
 */
public record SendMessageConfiguration(
        List<String> acceptedOutputModes,
        Integer historyLength,
        Boolean returnImmediately) {

    public boolean returnImmediatelyEnabled() {
        return Boolean.TRUE.equals(returnImmediately);
    }
}
