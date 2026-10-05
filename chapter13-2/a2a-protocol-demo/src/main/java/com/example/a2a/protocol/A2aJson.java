package com.example.a2a.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * A2A JSON 序列化工具。
 *
 * <p>规范里的 JSON 字段使用 lowerCamelCase，Java record 字段也按同样方式命名，
 * 这样可以减少映射配置和协议误差。
 */
public final class A2aJson {

    public static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private A2aJson() {}
}
