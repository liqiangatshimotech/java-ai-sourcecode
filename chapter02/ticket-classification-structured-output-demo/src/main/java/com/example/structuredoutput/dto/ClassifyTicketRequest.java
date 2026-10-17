package com.example.structuredoutput.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 调用方输入；分类服务会显式校验，普通 Java 方法不会自动执行校验注解。 */
public record ClassifyTicketRequest(
    @NotBlank @Size(max = 2000) String content
) {
}
