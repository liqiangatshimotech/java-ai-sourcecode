package com.example.langchain4jchat;

import com.example.langchain4jchat.config.AiChatProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AiChatProperties.class)
public class LangChain4jChatApplication {

    public static void main(String[] args) {
        SpringApplication.run(LangChain4jChatApplication.class, args);
    }
}
