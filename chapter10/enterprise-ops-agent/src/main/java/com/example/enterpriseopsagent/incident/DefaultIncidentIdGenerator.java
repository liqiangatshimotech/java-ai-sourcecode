package com.example.enterpriseopsagent.incident;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

public class DefaultIncidentIdGenerator implements IncidentIdGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private final Clock clock;

    public DefaultIncidentIdGenerator(Clock clock) {
        this.clock = clock;
    }

    @Override
    public String nextIncidentId() {
        String date = LocalDate.now(clock).format(DATE_FORMATTER);
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        return "INC-" + date + "-" + suffix;
    }
}
