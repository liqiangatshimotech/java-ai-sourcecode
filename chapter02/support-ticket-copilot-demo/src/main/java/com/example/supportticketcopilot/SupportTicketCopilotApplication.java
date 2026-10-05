package com.example.supportticketcopilot;

import com.example.supportticketcopilot.config.TicketCopilotProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(TicketCopilotProperties.class)
public class SupportTicketCopilotApplication {

    public static void main(String[] args) {
        SpringApplication.run(SupportTicketCopilotApplication.class, args);
    }
}
