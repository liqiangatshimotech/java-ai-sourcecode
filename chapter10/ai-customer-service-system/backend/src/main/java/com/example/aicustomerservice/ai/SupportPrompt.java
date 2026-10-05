package com.example.aicustomerservice.ai;

import com.example.aicustomerservice.entity.CustomerProfile;
import com.example.aicustomerservice.entity.RetrievedKnowledge;
import com.example.aicustomerservice.entity.SupportIntent;
import com.example.aicustomerservice.entity.ToolTrace;
import java.util.List;

public record SupportPrompt(
    String question,
    CustomerProfile customer,
    SupportIntent intent,
    List<RetrievedKnowledge> knowledge,
    List<ToolTrace> toolTraces,
    boolean handoffRequired,
    List<String> history
) {
    public static final String VERSION = "support-prompt-20260909-v2";
    public SupportPrompt(String question, CustomerProfile customer, SupportIntent intent,
                         List<RetrievedKnowledge> knowledge, List<ToolTrace> toolTraces, boolean handoffRequired) {
        this(question, customer, intent, knowledge, toolTraces, handoffRequired, List.of());
    }
    public SupportPrompt { history = List.copyOf(history); }
}
