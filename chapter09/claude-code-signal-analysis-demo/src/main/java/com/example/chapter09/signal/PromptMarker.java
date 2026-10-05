package com.example.chapter09.signal;

public record PromptMarker(
        boolean chinaTimezone,
        boolean chinaLikeProxyHost,
        boolean aiLabLikeProxyHost,
        int apostropheCodePoint,
        String dateSeparator
) {

    public String explain() {
        return "timezone=" + (chinaTimezone ? "china" : "other")
                + ", proxyHost=" + (chinaLikeProxyHost ? "china-like" : "other")
                + ", aiLab=" + (aiLabLikeProxyHost ? "yes" : "no")
                + ", apostrophe=" + UnicodePrinter.describe(apostropheCodePoint)
                + ", dateSeparator=\"" + dateSeparator + "\"";
    }
}
