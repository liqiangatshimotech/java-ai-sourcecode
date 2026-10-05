package com.example.javamcpreporadar.analysis;

import com.example.javamcpreporadar.github.GitHubRepositoryProfile;
import com.example.javamcpreporadar.github.RepositoryRole;

import java.util.Comparator;
import java.util.List;

public final class JavaMcpRepositoryRadar {

    private JavaMcpRepositoryRadar() {
    }

    public static List<GitHubRepositoryProfile> rankByAdoptionSignal(List<GitHubRepositoryProfile> profiles) {
        return profiles.stream()
            .sorted(
                Comparator.comparingInt(JavaMcpRepositoryRadar::roleWeight).reversed()
                    .thenComparing(GitHubRepositoryProfile::stars, Comparator.reverseOrder())
                    .thenComparing(GitHubRepositoryProfile::forks, Comparator.reverseOrder())
                    .thenComparing(GitHubRepositoryProfile::fullName)
            )
            .toList();
    }

    public static String shortVerdict(List<GitHubRepositoryProfile> rankedProfiles) {
        boolean hasServerCollection = rankedProfiles.stream()
            .anyMatch(profile -> profile.role() == RepositoryRole.SERVER_COLLECTION && profile.primaryLanguageIsJava());
        if (hasServerCollection) {
            return "有 Java 实现的知名 MCP 项目，但真正接近现成 server 集合的主要看 Quarkus MCP Servers；"
                + "官方 Java SDK 和 Spring AI 更适合作为企业项目落地 MCP 的基础设施。";
        }
        return "当前更知名的是 Java MCP 基础设施项目，现成 server 的数量和成熟度还在增长中。";
    }

    private static int roleWeight(GitHubRepositoryProfile profile) {
        return switch (profile.role()) {
            case OFFICIAL_SDK -> 5;
            case FRAMEWORK -> 4;
            case SERVER_COLLECTION -> 3;
            case SPECIFIC_SERVER -> 2;
            case ANNOTATION_MODEL -> 1;
        };
    }
}
