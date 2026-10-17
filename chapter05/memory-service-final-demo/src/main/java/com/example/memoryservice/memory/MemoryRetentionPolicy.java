package com.example.memoryservice.memory;
import java.time.*;
/** 使用注入的时钟，使测试能够精确验证到期时刻。 */
public class MemoryRetentionPolicy {
    private final Duration ttl;
    private final Clock clock;
    public MemoryRetentionPolicy(Duration ttl,Clock clock) {
        if(ttl==null||ttl.isZero()||ttl.isNegative()) throw new IllegalArgumentException("TTL 必须为正数");
        this.ttl=ttl;this.clock=clock;
    }
    public Instant now() { return clock.instant(); }
    public Instant expiresAt() { return now().plus(ttl); }
}
