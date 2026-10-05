package com.example.aicustomerservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 前后端分离开发时的 CORS 配置。
 *
 * 独立项目的 frontend 可以用本地静态服务器启动，也可以直接打开 HTML。
 * 这里仅开放 /api/**，不会影响 Actuator 或其他管理端点。
 */
@Configuration
public class WebConfiguration implements WebMvcConfigurer {
    private final com.example.aicustomerservice.security.SupportAuthentication authentication;
    public WebConfiguration(
        @org.springframework.beans.factory.annotation.Value("${support.auth.enabled:true}") boolean enabled,
        @org.springframework.beans.factory.annotation.Value("${support.auth.tenant:course}") String tenant,
        @org.springframework.beans.factory.annotation.Value("${support.auth.identities:[]}") String identities) {
        authentication = new com.example.aicustomerservice.security.SupportAuthentication(enabled, tenant, identities);
    }
    @Override public void addInterceptors(org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
        registry.addInterceptor(authentication).addPathPatterns("/api/**");
    }


    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOriginPatterns("*")
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .maxAge(3600);
    }
}
