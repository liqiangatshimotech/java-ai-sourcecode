package com.example.javamcpreporadar;

import com.example.javamcpreporadar.github.GitHubRepositoryClient;
import com.example.javamcpreporadar.github.KnownJavaMcpRepository;
import com.example.javamcpreporadar.github.RepositoryRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GitHubRepositoryClientTest {

    @Test
    void parsesRepositoryProfile() throws Exception {
        KnownJavaMcpRepository source = new KnownJavaMcpRepository(
            "quarkiverse/quarkus-mcp-servers",
            RepositoryRole.SERVER_COLLECTION,
            "Java MCP servers"
        );
        String json = """
            {
              "full_name": "quarkiverse/quarkus-mcp-servers",
              "html_url": "https://github.com/quarkiverse/quarkus-mcp-servers",
              "description": "Model Context Protocol Servers in Quarkus",
              "language": "Java",
              "stargazers_count": 192,
              "forks_count": 54,
              "updated_at": "2026-06-01T21:47:01Z"
            }
            """;

        var profile = GitHubRepositoryClient.parse(source, json, new ObjectMapper());

        assertThat(profile.fullName()).isEqualTo(source.fullName());
        assertThat(profile.primaryLanguageIsJava()).isTrue();
        assertThat(profile.stars()).isEqualTo(192);
        assertThat(profile.role()).isEqualTo(RepositoryRole.SERVER_COLLECTION);
    }
}
