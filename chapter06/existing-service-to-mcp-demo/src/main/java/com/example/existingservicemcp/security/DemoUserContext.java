package com.example.existingservicemcp.security;

import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DemoUserContext {

    public CurrentUser currentUser() {
        // 示例中固定当前用户；生产环境应从登录态、JWT 或网关透传身份解析。
        return new CurrentUser("agent-1001", "acme", Set.of("support_agent"));
    }
}
