package com.example.existingservicemcpclient.existing;

import java.util.List;

public record SupportCaseAnswer(
    String orderId,
    String orderStatus,
    String refundDecision,
    String reply,
    List<String> toolsUsed
) {
}
