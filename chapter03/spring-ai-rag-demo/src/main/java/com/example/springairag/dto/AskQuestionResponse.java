package com.example.springairag.dto;

import java.util.List;

public record AskQuestionResponse(
    String answer,
    List<RetrievedSource> sources,
    List<String> retrievedChunks
) {
}
