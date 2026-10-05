package com.example.aicustomerservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TicketAssignRequest(
    @NotBlank(message = "owner 不能为空")
    @Size(max = 64, message = "owner 长度不能超过 64")
    String owner
) {
}
