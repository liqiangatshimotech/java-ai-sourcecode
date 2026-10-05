package com.example.springairag.service;

import com.example.springairag.rag.RagPrompt;

public interface AiGateway {

    String answer(RagPrompt prompt);
}
