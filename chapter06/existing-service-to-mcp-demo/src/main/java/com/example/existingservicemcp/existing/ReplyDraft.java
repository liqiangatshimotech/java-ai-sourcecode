package com.example.existingservicemcp.existing;

public record ReplyDraft(
    String ticketId,
    String tone,
    String reply,
    boolean needsHumanApproval
) {
}
