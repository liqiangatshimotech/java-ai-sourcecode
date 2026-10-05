package com.example.aicustomerservice.security;

import com.example.aicustomerservice.common.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 小型服务端凭据库：凭据来自环境变量，客户不能自报角色或租户。
 * 每个实例只服务一个固定租户，跨租户凭据一律拒绝。多租户共库需要另加行级隔离，不能靠此配置宣称支持。
 * 真实企业可将这个入口替换为验证 OIDC 的 issuer/audience/签名/有效期，Controller 保持不变。
 */
public class SupportAuthentication implements HandlerInterceptor {
    private record Credential(byte[] token, SupportIdentity identity) { }
    private final boolean enabled;
    private final String deploymentTenant;
    private final List<Credential> credentials = new ArrayList<>();
    public SupportAuthentication(boolean enabled, String deploymentTenant, String identitiesJson) {
        this.enabled=enabled; this.deploymentTenant=deploymentTenant;
        try {
            var root = new ObjectMapper().readTree(identitiesJson);
            if (!root.isArray()) throw new IllegalArgumentException("身份配置必须是数组");
            for (var entry:root) {
                String token=entry.path("token").asText(), user=entry.path("user").asText();
                String tenant=entry.path("tenant").asText(), role=entry.path("role").asText();
                if (token.length()<24 || user.isBlank() || tenant.isBlank() || !List.of("viewer","agent","admin").contains(role))
                    throw new IllegalArgumentException("身份配置字段不完整或令牌短于 24 位");
                credentials.add(new Credential(token.getBytes(StandardCharsets.UTF_8), new SupportIdentity(user,tenant,role)));
            }
        } catch (java.io.IOException e) { throw new IllegalArgumentException("身份配置不是有效 JSON"); }
    }
    @Override public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!enabled || "OPTIONS".equals(request.getMethod())) return true;
        String auth=request.getHeader("Authorization");
        if (auth==null || !auth.startsWith("Bearer ")) throw new BusinessException("AUTH_REQUIRED","请提供客服访问凭据");
        byte[] supplied=auth.substring(7).getBytes(StandardCharsets.UTF_8);
        SupportIdentity identity=credentials.stream()
            .filter(c -> MessageDigest.isEqual(c.token(), supplied)).map(Credential::identity).findFirst()
            .orElseThrow(() -> new BusinessException("AUTH_REQUIRED","访问凭据无效"));
        if (!deploymentTenant.equals(identity.tenant())) throw new BusinessException("ACCESS_DENIED","该凭据不能访问此租户实例");
        String path=request.getRequestURI();
        boolean write=!List.of("GET","HEAD","OPTIONS").contains(request.getMethod());
        boolean adminAction=path.contains("/knowledge/") || path.contains("/orders/") || path.endsWith("/assign");
        if (write && (identity.role().equals("viewer") || adminAction && !identity.role().equals("admin")))
            throw new BusinessException("ACCESS_DENIED","当前角色无权执行该操作");
        request.setAttribute(SupportIdentity.ATTRIBUTE, identity);
        return true;
    }
}
