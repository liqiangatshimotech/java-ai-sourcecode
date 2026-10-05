package com.example.springaimcpclient;

import com.example.springaimcpclient.config.AiClientProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AiClientProperties.class)
public class SpringAiMcpClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringAiMcpClientApplication.class, args);
    }
}
