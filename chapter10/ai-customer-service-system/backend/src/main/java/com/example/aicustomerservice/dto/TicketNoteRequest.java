package com.example.aicustomerservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TicketNoteRequest(
    @NotBlank(message = "operator 不能为空")
    @Size(max = 64, message = "operator 长度不能超过 64")
    String operator,

    @NotBlank(message = "message 不能为空")
    @Size(max = 500, message = "message 长度不能超过 500")
    String message
) {
}
