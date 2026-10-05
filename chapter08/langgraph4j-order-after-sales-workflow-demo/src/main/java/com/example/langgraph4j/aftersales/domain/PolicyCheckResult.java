package com.example.langgraph4j.aftersales.domain;

/** 售后政策检查结果。 */
public record PolicyCheckResult(boolean allowed, String reason) {
}
