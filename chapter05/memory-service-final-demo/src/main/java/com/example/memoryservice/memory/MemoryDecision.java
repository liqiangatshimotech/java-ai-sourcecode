package com.example.memoryservice.memory;
/** 归一化结果和拒绝原因；不能把用户原文直接当成系统规则。 */
public record MemoryDecision(boolean shouldRemember,String memoryType,String normalizedContent,String reason) {
    public static MemoryDecision skip(String reason) { return new MemoryDecision(false,"none","",reason); }
    public static MemoryDecision remember(String type,String content,String reason) { return new MemoryDecision(true,type,content,reason); }
}
