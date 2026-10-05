package com.example.springaiticketclassifier.controller;

import com.example.springaiticketclassifier.dto.ClassifyTicketRequest;
import com.example.springaiticketclassifier.dto.TicketClassification;
import com.example.springaiticketclassifier.service.SpringAiTicketClassifier;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tickets")
public class TicketController {

    private final SpringAiTicketClassifier classifier;

    public TicketController(SpringAiTicketClassifier classifier) {
        this.classifier = classifier;
    }

    @PostMapping("/classify/text")
    public String classifyAsText(@Valid @RequestBody ClassifyTicketRequest request) {
        return this.classifier.classifyAsText(request.content());
    }

    @PostMapping("/classify/structured")
    public TicketClassification classify(@Valid @RequestBody ClassifyTicketRequest request) {
        return this.classifier.classify(request.content());
    }

    @PostMapping("/classify/converter")
    public TicketClassification classifyWithConverter(@Valid @RequestBody ClassifyTicketRequest request) {
        return this.classifier.classifyWithConverter(request.content());
    }
}
