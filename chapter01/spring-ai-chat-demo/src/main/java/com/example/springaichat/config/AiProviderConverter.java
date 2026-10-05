package com.example.springaichat.config;

import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/** 配置文件与 HTTP 参数共用供应商解析规则，chatgpt 统一映射到 OPENAI。 */
@Component
@ConfigurationPropertiesBinding
public final class AiProviderConverter implements Converter<String, AiProvider> {
    @Override public AiProvider convert(String value) { return AiProvider.parse(value); }
}
