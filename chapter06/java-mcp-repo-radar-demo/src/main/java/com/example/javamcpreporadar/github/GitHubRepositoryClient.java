package com.example.javamcpreporadar.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Map;

public final class GitHubRepositoryClient {

    private static final String GITHUB_API = "https://api.github.com/repos/";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String token;

    public GitHubRepositoryClient(HttpClient httpClient, ObjectMapper objectMapper, String token) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.token = token == null ? "" : token.trim();
    }

    public static GitHubRepositoryClient fromEnv(Map<String, String> env) {
        return new GitHubRepositoryClient(
            HttpClient.newHttpClient(),
            new ObjectMapper(),
            env.getOrDefault("GITHUB_TOKEN", "")
        );
    }

    public GitHubRepositoryProfile fetch(KnownJavaMcpRepository repository) throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder()
            .uri(URI.create(GITHUB_API + repository.fullName()))
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "java-ai-course-mcp-repo-radar")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .GET();
        if (!token.isBlank()) {
            request.header("Authorization", "Bearer " + token);
        }

        HttpResponse<String> response = httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("GitHub API returned " + response.statusCode() + " for " + repository.fullName());
        }
        return parse(repository, response.body(), objectMapper);
    }

    public static GitHubRepositoryProfile parse(KnownJavaMcpRepository repository, String json, ObjectMapper objectMapper)
        throws IOException {
        JsonNode root = objectMapper.readTree(json);
        return new GitHubRepositoryProfile(
            root.path("full_name").asText(repository.fullName()),
            root.path("html_url").asText("https://github.com/" + repository.fullName()),
            root.path("description").asText(""),
            root.path("language").asText(""),
            root.path("stargazers_count").asInt(),
            root.path("forks_count").asInt(),
            Instant.parse(root.path("updated_at").asText()),
            repository.role(),
            repository.courseNote()
        );
    }
}
