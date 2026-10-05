package com.example.texttosqlagent.web;

import com.example.texttosqlagent.agent.AnalysisReport;
import com.example.texttosqlagent.agent.DataAnalysisAgentService;
import com.example.texttosqlagent.agent.DataAnalysisAgentService.AnalysisOutcome;
import com.example.texttosqlagent.schema.TableSchema;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据分析 HTTP 入口。
 *
 * POST /api/analysis/ask    自然语言提问，走完整的 Agent 链路
 * GET  /api/analysis/schema 返回当前库表结构 + 中文注释，
 *                            调试 Schema Linking 时直接看模型「眼里」的 schema
 */
@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    private final DataAnalysisAgentService agentService;
    private final List<TableSchema> tableSchemas;

    public AnalysisController(DataAnalysisAgentService agentService, List<TableSchema> tableSchemas) {
        this.agentService = agentService;
        this.tableSchemas = tableSchemas;
    }

    @PostMapping("/ask")
    public AskResponse ask(@Valid @RequestBody AskRequest request) {
        AnalysisOutcome outcome = agentService.askWithResult(request.question());
        AnalysisReport report = outcome.report();
        return AskResponse.from(report, outcome.result());
    }

    @GetMapping("/schema")
    public List<TableSchema> schema() {
        return tableSchemas;
    }
}
