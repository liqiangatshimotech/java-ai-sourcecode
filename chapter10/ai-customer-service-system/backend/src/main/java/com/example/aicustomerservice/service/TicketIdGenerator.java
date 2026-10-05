package com.example.aicustomerservice.service;

import com.example.aicustomerservice.common.Hashes;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class TicketIdGenerator {

    private final Clock clock;

    public TicketIdGenerator(Clock clock) {
        this.clock = clock;
    }

    public String nextTicketId() {
        String date = LocalDate.now(clock).format(DateTimeFormatter.BASIC_ISO_DATE);
        return "CS-" + date + "-" + Hashes.sha256Short(UUID.randomUUID().toString()).toUpperCase();
    }
}
