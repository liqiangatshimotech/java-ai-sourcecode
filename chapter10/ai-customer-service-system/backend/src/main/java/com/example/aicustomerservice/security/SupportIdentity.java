package com.example.aicustomerservice.security;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** 身份由服务端验证 Bearer 凭据后写入，Controller 不信任 JSON 中自报的 operator。 */
public record SupportIdentity(String user, String tenant, String role) {
    public static final String ATTRIBUTE = SupportIdentity.class.getName();
    public static String operator(String legacyOperator) {
        var context = RequestContextHolder.getRequestAttributes();
        if (context instanceof ServletRequestAttributes attributes) {
            Object identity = attributes.getRequest().getAttribute(ATTRIBUTE);
            if (identity instanceof SupportIdentity verified) return verified.user();
        }
        // 只在明确关闭鉴权的本机演示或直接调用服务的单元测试中使用旧字段。
        return legacyOperator;
    }
}
