package com.example.skillbasics;

import java.nio.file.Path;
import java.util.List;

public class SkillPackageBasicsDemo {

    public static void main(String[] args) {
        SkillPackage codeReview = new SkillPackage(
                "code-review",
                "审查 Java 代码变更，优先发现行为风险和缺失测试。",
                List.of("code review", "代码审查", "review PR"),
                Path.of("skills/code-review/SKILL.md"),
                List.of(Path.of("skills/code-review/templates/review-report.md")),
                List.of(Path.of("skills/code-review/scripts/scan.sh"))
        );

        String task = "帮我做一次代码审查，重点看回归风险";
        System.out.println("Skill: " + codeReview.name());
        System.out.println("Matched: " + codeReview.match(task));
        System.out.println("Instruction: " + codeReview.instructionFile());
    }

    record SkillPackage(
            String name,
            String description,
            List<String> triggers,
            Path instructionFile,
            List<Path> templates,
            List<Path> scripts
    ) {
        boolean match(String taskText) {
            // 最小版本先用关键词触发。真实系统里可以替换成 embedding 召回或分类模型。
            String normalized = taskText.toLowerCase();
            return triggers.stream()
                    .map(String::toLowerCase)
                    .anyMatch(normalized::contains);
        }
    }
}
