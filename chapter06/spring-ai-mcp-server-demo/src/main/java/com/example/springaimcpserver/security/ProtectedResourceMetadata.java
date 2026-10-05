package com.example.springaimcpserver.security;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;
import java.util.*;
/** RFC9728发现入口；签发令牌和PKCE授权流程由外部IdP与客户端完成。 */
@RestController @Profile("remote")
public class ProtectedResourceMetadata {
    private final McpSecurityConfiguration.AuthSettings settings;
    public ProtectedResourceMetadata(McpSecurityConfiguration.AuthSettings settings){this.settings=settings;}
    @GetMapping({"/.well-known/oauth-protected-resource","/.well-known/oauth-protected-resource/mcp"})
    public Map<String,Object> metadata(){return Map.of("resource",settings.resource().toString(),
            "authorization_servers",List.of(settings.issuer().toString()),"scopes_supported",List.of("mcp.read"),
            "bearer_methods_supported",List.of("header"));}
}
