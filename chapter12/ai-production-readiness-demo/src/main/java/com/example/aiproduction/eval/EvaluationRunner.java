package com.example.aiproduction.eval;

import java.util.List;

/**
 * 简化版离线评测器。
 *
 * <p>这里用字符串命中模拟评测，是为了让示例可以离线运行。真实项目会把实际模型输出、工具调用记录和人工标注结果放进评测器。
 */
public class EvaluationRunner {

    public EvaluationSummary run(List<EvaluationCase> cases, DemoAgentAnswer answer) {
        return run(cases, ignored -> answer);
    }

    /** 每个问题独立调用答题函数；旧重载只用于展示评分规则，不代表批量模型验收。 */
    public EvaluationSummary run(List<EvaluationCase> cases,
                                 java.util.function.Function<EvaluationCase, DemoAgentAnswer> answerer) {
        List<EvaluationResult> results = cases.stream().map(testCase -> {
            try { return evaluate(testCase, answerer.apply(testCase)); }
            catch (RuntimeException failure) { return new EvaluationResult(testCase.id(), false, "调用失败"); }
        }).toList();
        long passed = results.stream().filter(EvaluationResult::passed).count();
        double passRate = cases.isEmpty() ? 0.0 : passed * 1.0 / cases.size();
        return new EvaluationSummary(results, passRate);
    }

    private EvaluationResult evaluate(EvaluationCase testCase, DemoAgentAnswer answer) {
        boolean toolMatched = testCase.expectedTool().equals(answer.selectedTool());
        boolean answerMatched = answer.text().contains(testCase.expectedAnswerKeyword());
        boolean escalationMatched = testCase.shouldEscalate() == answer.escalated();

        if (toolMatched && answerMatched && escalationMatched) {
            return new EvaluationResult(testCase.id(), true, "工具、答案关键词和升级判断都命中");
        }
        return new EvaluationResult(
                testCase.id(),
                false,
                "toolMatched=%s, answerMatched=%s, escalationMatched=%s"
                        .formatted(toolMatched, answerMatched, escalationMatched));
    }

    public record DemoAgentAnswer(String selectedTool, String text, boolean escalated) {}
}
