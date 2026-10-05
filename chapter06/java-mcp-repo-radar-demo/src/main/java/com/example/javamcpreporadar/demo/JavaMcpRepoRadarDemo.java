package com.example.javamcpreporadar.demo;

import com.example.javamcpreporadar.analysis.JavaMcpRepositoryRadar;
import com.example.javamcpreporadar.analysis.RepoBriefFormatter;
import com.example.javamcpreporadar.config.AiModelProperties;
import com.example.javamcpreporadar.config.ChatModelFactory;
import com.example.javamcpreporadar.github.GitHubRepositoryClient;
import com.example.javamcpreporadar.github.GitHubRepositoryProfile;
import com.example.javamcpreporadar.github.KnownJavaMcpRepository;
import dev.langchain4j.model.chat.ChatModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class JavaMcpRepoRadarDemo {

    private JavaMcpRepoRadarDemo() {
    }

    public static void main(String[] args) throws Exception {
        Map<String, String> env = System.getenv();
        GitHubRepositoryClient client = GitHubRepositoryClient.fromEnv(env);
        List<GitHubRepositoryProfile> profiles = new ArrayList<>();

        for (KnownJavaMcpRepository repository : KnownJavaMcpRepository.defaults()) {
            profiles.add(client.fetch(repository));
        }

        List<GitHubRepositoryProfile> rankedProfiles = JavaMcpRepositoryRadar.rankByAdoptionSignal(profiles);
        System.out.println(RepoBriefFormatter.toMarkdown(rankedProfiles));

        if (Boolean.parseBoolean(env.getOrDefault("APP_AI_SUMMARY", "false"))) {
            AiModelProperties aiProperties = AiModelProperties.fromEnv(env);
            if (!aiProperties.defaultProviderConfigured()) {
                System.err.println("Skip AI summary: default provider is not configured.");
                return;
            }
            ChatModel chatModel = ChatModelFactory.createDefault(aiProperties);
            System.out.println("\n## AI Summary\n");
            System.out.println(chatModel.chat(RepoBriefFormatter.toPrompt(rankedProfiles)));
        }
    }
}
