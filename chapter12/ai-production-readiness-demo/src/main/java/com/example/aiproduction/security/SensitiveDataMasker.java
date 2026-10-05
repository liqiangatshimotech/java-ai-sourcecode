package com.example.aiproduction.security;

/**
 * 敏感信息脱敏工具。
 *
 * <p>日志里不能直接保存手机号、邮箱、身份证号、API Key 等敏感字段。这里演示两个常见规则：手机号和邮箱脱敏。
 */
public class SensitiveDataMasker {

    public String mask(String text) {
        String maskedPhone = text.replaceAll("(1[3-9]\\d)\\d{6}(\\d{2})", "$1*******$2");
        return maskedPhone.replaceAll(
                "([A-Za-z0-9._%+-])[A-Za-z0-9._%+-]*(@[A-Za-z0-9.-]+\\.[A-Za-z]{2,})",
                "$1***$2");
    }
}
