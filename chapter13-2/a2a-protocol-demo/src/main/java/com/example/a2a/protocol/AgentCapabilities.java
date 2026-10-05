package com.example.a2a.protocol;

/**
 * Agent Card 中声明的能力。
 */
public record AgentCapabilities(
        boolean streaming,
        boolean pushNotifications,
        boolean stateTransitionHistory,
        boolean extendedAgentCard) {}
