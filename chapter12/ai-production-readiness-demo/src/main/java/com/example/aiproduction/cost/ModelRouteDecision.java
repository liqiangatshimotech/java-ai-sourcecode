package com.example.aiproduction.cost;

/**
 * 模型路由决策。
 *
 * <p>reason 字段是为了让线上排查更容易。只知道“选了哪个模型”还不够，还要知道为什么选它。
 */
public record ModelRouteDecision(ModelProfile selectedModel, String reason) {}
