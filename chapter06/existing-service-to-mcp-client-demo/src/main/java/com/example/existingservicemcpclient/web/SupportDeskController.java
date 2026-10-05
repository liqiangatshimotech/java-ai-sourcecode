package com.example.existingservicemcpclient.web;

import com.example.existingservicemcpclient.existing.SupportCaseAnswer;
import com.example.existingservicemcpclient.existing.SupportCaseRequest;
import com.example.existingservicemcpclient.existing.SupportDeskService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/existing-support")
public class SupportDeskController {

    private final SupportDeskService supportDeskService;

    public SupportDeskController(SupportDeskService supportDeskService) {
        this.supportDeskService = supportDeskService;
    }

    @PostMapping("/refund-advice")
    public SupportCaseAnswer refundAdvice(@Valid @RequestBody SupportCaseRequest request) {
        return supportDeskService.answerRefundQuestion(request);
    }
}
