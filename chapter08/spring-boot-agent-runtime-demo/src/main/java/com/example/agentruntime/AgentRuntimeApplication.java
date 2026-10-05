package com.example.agentruntime;

import com.example.agentruntime.config.AgentProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AgentProperties.class)
public class AgentRuntimeApplication {
    public static void main(String[] args) { SpringApplication.run(AgentRuntimeApplication.class, args); }
}
