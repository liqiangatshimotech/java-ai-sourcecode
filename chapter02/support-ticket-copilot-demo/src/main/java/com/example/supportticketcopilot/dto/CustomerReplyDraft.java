package com.example.supportticketcopilot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CustomerReplyDraft(
    @NotBlank @Size(max = 80) String subject,
    @NotBlank @Size(max = 600) String body,
    @NotNull ReplyTone tone
) {
}
