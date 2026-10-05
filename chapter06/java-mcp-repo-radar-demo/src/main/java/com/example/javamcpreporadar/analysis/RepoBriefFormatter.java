package com.example.javamcpreporadar.analysis;

import com.example.javamcpreporadar.github.GitHubRepositoryProfile;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class RepoBriefFormatter {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneId.of("UTC"));

    private RepoBriefFormatter() {
    }

    public static String toMarkdown(List<GitHubRepositoryProfile> profiles) {
        StringBuilder markdown = new StringBuilder();
        markdown.append("# Java MCP GitHub Radar\n\n");
        markdown.append(JavaMcpRepositoryRadar.shortVerdict(profiles)).append("\n\n");
        markdown.append("| Repository | Role | Stars | Forks | Updated | Note |\n");
        markdown.append("|---|---:|---:|---:|---|---|\n");
        for (GitHubRepositoryProfile profile : profiles) {
            markdown.append("| ")
                .append(profile.fullName())
                .append(" | ")
                .append(profile.role().label())
                .append(" | ")
                .append(profile.stars())
                .append(" | ")
                .append(profile.forks())
                .append(" | ")
                .append(DATE.format(profile.updatedAt()))
                .append(" | ")
                .append(profile.courseNote())
                .append(" |\n");
        }
        return markdown.toString();
    }

    public static String toPrompt(List<GitHubRepositoryProfile> profiles) {
        return """
            你是 Java AI 工程课程的讲师。请基于下面这些 GitHub 仓库指标，
            用中文写一段 150 字以内的技术判断：Java 生态里有没有值得关注的 MCP 项目，
            哪些适合学源码，哪些适合企业项目落地。不要夸张，不要营销口吻。

            %s
            """.formatted(toMarkdown(profiles));
    }
}
