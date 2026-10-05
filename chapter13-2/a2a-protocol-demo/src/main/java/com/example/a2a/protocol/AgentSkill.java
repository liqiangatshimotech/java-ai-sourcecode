package com.example.a2a.protocol;

import java.util.List;

/**
 * Agent 对外声明的一项能力。
 */
public record AgentSkill(
        String id,
        String name,
        String description,
        List<String> inputModes,
        List<String> outputModes,
        List<String> examples) {}
