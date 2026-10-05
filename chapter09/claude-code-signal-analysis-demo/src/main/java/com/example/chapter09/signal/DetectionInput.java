package com.example.chapter09.signal;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

public record DetectionInput(
        ZoneId systemZone,
        Optional<String> proxyUrl,
        LocalDate today
) {

    public static DetectionInput withoutProxy(String zoneId, LocalDate today) {
        return new DetectionInput(ZoneId.of(zoneId), Optional.empty(), today);
    }

    public static DetectionInput withProxy(String zoneId, String proxyUrl, LocalDate today) {
        return new DetectionInput(ZoneId.of(zoneId), Optional.of(proxyUrl), today);
    }
}
