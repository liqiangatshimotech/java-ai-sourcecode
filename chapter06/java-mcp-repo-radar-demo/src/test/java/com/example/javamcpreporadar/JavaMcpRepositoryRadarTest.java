package com.example.javamcpreporadar;

import com.example.javamcpreporadar.analysis.JavaMcpRepositoryRadar;
import com.example.javamcpreporadar.analysis.RepoBriefFormatter;
import com.example.javamcpreporadar.github.GitHubRepositoryProfile;
import com.example.javamcpreporadar.github.RepositoryRole;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JavaMcpRepositoryRadarTest {

    @Test
    void ranksOfficialInfrastructureBeforeServerCollection() {
        GitHubRepositoryProfile serverCollection = profile(
            "quarkiverse/quarkus-mcp-servers",
            RepositoryRole.SERVER_COLLECTION,
            192
        );
        GitHubRepositoryProfile officialSdk = profile(
            "modelcontextprotocol/java-sdk",
            RepositoryRole.OFFICIAL_SDK,
            3462
        );

        List<GitHubRepositoryProfile> ranked = JavaMcpRepositoryRadar.rankByAdoptionSignal(
            List.of(serverCollection, officialSdk)
        );

        assertThat(ranked).extracting(GitHubRepositoryProfile::fullName)
            .containsExactly("modelcontextprotocol/java-sdk", "quarkiverse/quarkus-mcp-servers");
    }

    @Test
    void formatsMarkdownTable() {
        String markdown = RepoBriefFormatter.toMarkdown(List.of(profile(
            "quarkiverse/quarkus-mcp-servers",
            RepositoryRole.SERVER_COLLECTION,
            192
        )));

        assertThat(markdown).contains("Java MCP GitHub Radar");
        assertThat(markdown).contains("quarkiverse/quarkus-mcp-servers");
        assertThat(markdown).contains("MCP Server 集合");
    }

    private static GitHubRepositoryProfile profile(String fullName, RepositoryRole role, int stars) {
        return new GitHubRepositoryProfile(
            fullName,
            "https://github.com/" + fullName,
            "demo",
            "Java",
            stars,
            10,
            Instant.parse("2026-06-01T00:00:00Z"),
            role,
            "course note"
        );
    }
}
