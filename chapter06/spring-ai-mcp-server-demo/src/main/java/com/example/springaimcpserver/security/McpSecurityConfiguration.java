package com.example.springaimcpserver.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.transport.WebMvcStatelessServerTransport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import java.net.URI;
import java.util.*;

/** 本地与远程安全边界：remote 是 OAuth 资源服务器，不是授权服务器。 */
@Configuration
public class McpSecurityConfiguration {
    public static final String VERIFIED_USER="course.verified-user";

    @Bean @Profile("!remote")
    SecurityFilterChain localSecurity(HttpSecurity http,ServerProperties server) throws Exception {
        // 无令牌的演示模式只能在回环地址监听，防止误设0.0.0.0后暴露固定身份。
        if(server.getAddress()==null || !server.getAddress().isLoopbackAddress()) {
            throw new IllegalStateException("local demo 必须绑定 127.0.0.1；远程部署使用 remote profile");
        }
        return http.csrf(c->c.disable()).authorizeHttpRequests(a->a.anyRequest().permitAll()).build();
    }

    /** 三项配置缺一不可，防止远程启动后退回匿名身份。 */
    public record AuthSettings(URI issuer,URI jwkSetUri,URI resource) {
        public AuthSettings {
            for(URI uri:List.of(issuer,jwkSetUri,resource)) {
                if(!uri.isAbsolute() || uri.getHost()==null || uri.getUserInfo()!=null || uri.getFragment()!=null
                        || !(uri.getScheme().equals("https") || (uri.getScheme().equals("http")
                        && Set.of("localhost","127.0.0.1","[::1]").contains(uri.getHost())))) {
                    throw new IllegalArgumentException("OAuth URL 必须为 HTTPS；HTTP 只允许本机联调");
                }
            }
        }
        public String metadataUri(){return resource.resolve("/.well-known/oauth-protected-resource").toString();}
    }
    @Bean @Profile("remote")
    AuthSettings authSettings(@Value("${mcp.auth.issuer:}") String issuer,
                              @Value("${mcp.auth.jwk-set-uri:}") String jwk,
                              @Value("${mcp.auth.resource:}") String resource) {
        if(issuer.isBlank()||jwk.isBlank()||resource.isBlank()) throw new IllegalStateException("remote 需要 MCP_ISSUER、MCP_JWK_SET_URI、MCP_RESOURCE");
        return new AuthSettings(URI.create(issuer),URI.create(jwk),URI.create(resource));
    }
    @Bean @Profile("remote")
    JwtDecoder jwtDecoder(AuthSettings settings) {
        NimbusJwtDecoder decoder=NimbusJwtDecoder.withJwkSetUri(settings.jwkSetUri().toString()).build();
        OAuth2TokenValidator<Jwt> claims=jwt->{
            boolean valid=jwt.getExpiresAt()!=null && jwt.getAudience()!=null && jwt.getAudience().contains(settings.resource().toString())
                    && jwt.getSubject()!=null && !jwt.getSubject().isBlank()
                    && jwt.getClaimAsString("tenant_id")!=null && !jwt.getClaimAsString("tenant_id").isBlank();
            return valid?OAuth2TokenValidatorResult.success():OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token","令牌缺少身份、有效期或正确资源audience",null));
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(settings.issuer().toString()),
                new JwtTimestampValidator(java.time.Duration.ZERO),claims));
        return decoder;
    }
    @Bean @Profile("remote")
    SecurityFilterChain remoteSecurity(HttpSecurity http,AuthSettings settings) throws Exception {
        return http.csrf(c->c.disable()).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a->a.requestMatchers("/.well-known/oauth-protected-resource","/.well-known/oauth-protected-resource/mcp").permitAll()
                        .requestMatchers("/mcp","/mcp/**").hasAuthority("SCOPE_mcp.read").anyRequest().denyAll())
                .oauth2ResourceServer(o->o.jwt(j->{})
                        .authenticationEntryPoint((request,response,error)->{
                            response.setStatus(401);
                            response.setHeader("WWW-Authenticate","Bearer resource_metadata=\""+settings.metadataUri()+"\", scope=\"mcp.read\"");
                        }))
                .exceptionHandling(e->e.authenticationEntryPoint((request,response,error)->{
                    response.setStatus(401);
                    response.setHeader("WWW-Authenticate","Bearer resource_metadata=\""+settings.metadataUri()+"\", scope=\"mcp.read\"");
                }).accessDeniedHandler((request,response,error)->{
                    response.setStatus(403);
                    response.setHeader("WWW-Authenticate","Bearer error=\"insufficient_scope\", scope=\"mcp.read\"");
                })).build();
    }

    /**
     * Servlet入口只读取经过过滤链验证的Principal，复制到MCP传输上下文。
     * 工具可能运行在调度线程上，因此绝不在工具里依赖SecurityContextHolder或Servlet ThreadLocal。
     * remote使用无状态传输，每个请求都验证Bearer，不共享身份相关的MCP session。
     */
    @Bean @Profile("remote")
    WebMvcStatelessServerTransport remoteTransport(ObjectMapper mapper) {
        return WebMvcStatelessServerTransport.builder().jsonMapper(new JacksonMcpJsonMapper(mapper))
                .messageEndpoint("/mcp").contextExtractor(request->{
                    var principal=request.principal().orElseThrow(()->new SecurityException("缺少已验证身份"));
                    if(!(principal instanceof JwtAuthenticationToken auth) || !auth.isAuthenticated()) {
                        throw new SecurityException("只接受已验证JWT身份");
                    }
                    Jwt jwt=auth.getToken();
                    CurrentUser user=new CurrentUser(jwt.getSubject(),jwt.getClaimAsString("tenant_id"),Set.of("support_agent"));
                    return McpTransportContext.create(Map.of(VERIFIED_USER,user));
                }).build();
    }
}
