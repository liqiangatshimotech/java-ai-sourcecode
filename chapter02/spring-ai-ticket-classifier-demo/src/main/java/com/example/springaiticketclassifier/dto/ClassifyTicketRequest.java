package com.example.springaiticketclassifier.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClassifyTicketRequest(
    @NotBlank @Size(max = 2000) String content
) {
}
