package com.example.memoryservice.memory;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
/** 作用域只是地址；授权必须另外比较可信身份。各段编码以避免冒号拼接碰撞。 */
public record MemoryScope(String tenantId,String userId,String conversationId) {
    public MemoryScope {
        for(String v:new String[]{tenantId,userId,conversationId})
            if(v==null||v.isBlank()||v.length()>128) throw new IllegalArgumentException("作用域字段必须非空且不超过128字符");
    }
    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
    public String shortTermKey() { return longTermKey()+":"+encode(conversationId); }
    public String longTermKey() { return encode(tenantId)+":"+encode(userId); }
}
