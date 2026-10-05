package com.example.codingagent.web;

import com.example.codingagent.service.CodingAgentJob;
import com.example.codingagent.service.CodingTaskStatus;

import java.time.Instant;

public record CodingTaskSummaryResponse(
        String taskId,
        String repositoryName,
        String branchName,
        String description,
        String operator,
        CodingTaskStatus status,
        Instant createdAt,
        Instant updatedAt,
        boolean testsPassed
) {

    public static CodingTaskSummaryResponse from(CodingAgentJob job) {
        return new CodingTaskSummaryResponse(
                job.taskId(),
                job.repositoryName(),
                job.branchName(),
                job.description(),
                job.operator(),
                job.status(),
                job.createdAt(),
                job.updatedAt(),
                job.report() != null && job.report().testsPassed()
        );
    }
}
