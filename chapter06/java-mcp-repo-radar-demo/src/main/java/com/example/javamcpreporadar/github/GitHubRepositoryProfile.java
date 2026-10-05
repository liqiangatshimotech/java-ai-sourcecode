package com.example.javamcpreporadar.github;

import java.time.Instant;

public record GitHubRepositoryProfile(
    String fullName,
    String htmlUrl,
    String description,
    String language,
    int stars,
    int forks,
    Instant updatedAt,
    RepositoryRole role,
    String courseNote
) {

    public boolean primaryLanguageIsJava() {
        return "Java".equalsIgnoreCase(language);
    }
}
