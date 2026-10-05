package com.example.codingagent.web;

import com.example.codingagent.config.ModelSettings;
import com.example.codingagent.service.CodingAgentJob;
import com.example.codingagent.service.CodingAgentJobService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

/**
 * Coding Agent 管理台 API。
 *
 * 这些接口模拟企业内部平台的真实工作流：创建任务、审批任务、执行 Agent、
 * 查看步骤、查看 diff、读取工作区文件。
 */
@RestController
@RequestMapping("/api/coding-agent")
public class CodingAgentController {

    private final CodingAgentJobService jobService;

    public CodingAgentController(CodingAgentJobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping("/runtime")
    public RuntimeInfoResponse runtime() {
        ModelSettings settings = ModelSettings.fromEnvironment();
        return new RuntimeInfoResponse(
                settings.provider().id(),
                settings.model(),
                settings.baseUrl(),
                settings.apiKeyConfigured(),
                jobService.registeredToolNames(),
                jobService.workspaceBase().toString()
        );
    }

    @PostMapping("/tasks")
    public CodingTaskDetailResponse createTask(@Valid @RequestBody CreateCodingTaskRequest request)
            throws IOException {
        CodingAgentJob job = jobService.createJob(
                request.repositoryName(),
                request.branchName(),
                request.description(),
                request.operator()
        );
        return CodingTaskDetailResponse.from(job);
    }

    @GetMapping("/tasks")
    public List<CodingTaskSummaryResponse> listTasks() {
        return jobService.listJobs().stream()
                .map(CodingTaskSummaryResponse::from)
                .toList();
    }

    @GetMapping("/tasks/{taskId}")
    public CodingTaskDetailResponse getTask(@PathVariable("taskId") String taskId) {
        return CodingTaskDetailResponse.from(jobService.requireJob(taskId));
    }

    @PostMapping("/tasks/{taskId}/approve")
    public CodingTaskDetailResponse approveTask(@PathVariable("taskId") String taskId,
                                                @Valid @RequestBody ApproveCodingTaskRequest request) throws IOException {
        return CodingTaskDetailResponse.from(jobService.approve(taskId, request.proposalId(), request.sourceVersion(), request.approver(), request.comment()));
    }

    @PostMapping("/tasks/{taskId}/run")
    public CodingTaskDetailResponse runTask(@PathVariable("taskId") String taskId,
            @Valid @RequestBody RunCodingTaskRequest request) {
        return CodingTaskDetailResponse.from(jobService.run(taskId, request.proposalId()));
    }

    @PostMapping("/tasks/{taskId}/repair")
    public CodingTaskDetailResponse repairTask(@PathVariable("taskId") String taskId,
            @Valid @RequestBody RunCodingTaskRequest request) {
        return CodingTaskDetailResponse.from(jobService.repair(taskId, request.proposalId()));
    }

    @GetMapping("/tasks/{taskId}/files")
    public WorkspaceFileResponse readFile(@PathVariable("taskId") String taskId,
                                          @RequestParam(name = "path",
                                                  defaultValue = "src/main/java/com/acme/billing/PriceCalculator.java")
                                          String path) throws IOException {
        return new WorkspaceFileResponse(path, jobService.readWorkspaceFile(taskId, path));
    }
}
