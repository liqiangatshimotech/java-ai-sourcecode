package com.example.springaipersistentmemory.memory;
import java.time.Instant;
/** 长期偏好独立于会话窗口，保留来源与有效期；它不是业务权限。 */
public record UserPreferenceMemory(String userId,String type,String content,String source,Instant observedAt,Instant expiresAt) {
    public boolean reusable(Instant now) { return expiresAt.isAfter(now); }
}
