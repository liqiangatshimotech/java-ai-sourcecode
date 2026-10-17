package com.example.memoryservice.memory;
import org.springframework.stereotype.Component;
/** 不仅校验非空，还要比较已经认证的租户和用户与目标作用域。 */
@Component
public class MemoryAccessPolicy {
    public void assertAllowed(AuthenticatedUser principal,MemoryScope scope) {
        if(principal==null||!scope.tenantId().equals(principal.tenantId())||!scope.userId().equals(principal.userId()))
            throw new SecurityException("不能访问其他租户或用户的记忆");
    }
}
