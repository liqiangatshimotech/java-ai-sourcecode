package com.example.existingservicemcpclient;

import com.example.existingservicemcpclient.config.AiClientProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AiClientProperties.class)
public class ExistingServiceToMcpClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExistingServiceToMcpClientApplication.class, args);
    }
}
