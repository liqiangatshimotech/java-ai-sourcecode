package com.example.memoryservice.memory;
import java.time.Instant;
/** 来源和有效期随记忆保存；到期过滤与物理删除是两个独立动作。 */
public record MemoryRecord(String type,String content,String source,Instant createdAt,Instant expiresAt) {
    public boolean reusable(Instant now) { return expiresAt.isAfter(now); }
}
