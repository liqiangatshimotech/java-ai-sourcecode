package com.example.javaskillloader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class JavaSkillLoaderDemo {

    public static void main(String[] args) throws IOException {
        Path root = findSkillsRoot();
        List<SkillDefinition> skills = new FileSystemSkillLoader().load(root);
        SkillRouter router = new SkillRouter(skills);

        String task = "帮我评审订单 Spring Boot 接口变更，重点看兼容性、权限和测试";
        Optional<SkillDefinition> matched = router.route(task);

        System.out.println("Loaded skills: " + skills.size());
        System.out.println("Matched skill: " + matched.map(SkillDefinition::name).orElse("none"));
    }

    private static Path findSkillsRoot() {
        Path moduleRoot = Path.of("skills");
        if (Files.isDirectory(moduleRoot)) {
            return moduleRoot;
        }

        Path courseRoot = Path.of("chapter07/java-skill-loader-demo/skills");
        if (Files.isDirectory(courseRoot)) {
            return courseRoot;
        }

        throw new IllegalStateException("skills directory not found");
    }

    record SkillDefinition(
            String name,
            String description,
            List<String> triggers,
            String instruction,
            Path sourcePath
    ) {
        boolean canHandle(String taskText) {
            String normalized = taskText.toLowerCase();
            return triggers.stream()
                    .map(String::toLowerCase)
                    .anyMatch(normalized::contains);
        }
    }

    static class FileSystemSkillLoader {
        List<SkillDefinition> load(Path skillsRoot) throws IOException {
            try (var stream = Files.walk(skillsRoot)) {
                return stream
                        // 一个目录下有 SKILL.md，就把它当成一个 Skill 包。
                        .filter(path -> path.getFileName().toString().equals("SKILL.md"))
                        .map(SkillMarkdownParser::parse)
                        .toList();
            }
        }
    }

    static class SkillRouter {
        private final List<SkillDefinition> skills;

        SkillRouter(List<SkillDefinition> skills) {
            this.skills = List.copyOf(skills);
        }

        Optional<SkillDefinition> route(String taskText) {
            return skills.stream()
                    .filter(skill -> skill.canHandle(taskText))
                    // 示例先用描述长度做稳定排序，真实项目可替换成相似度分数。
                    .max(Comparator.comparingInt(skill -> skill.description().length()));
        }
    }

    static class SkillMarkdownParser {
        static SkillDefinition parse(Path skillFile) {
            try {
                List<String> lines = Files.readAllLines(skillFile);
                String name = valueAfter(lines, "name:");
                String description = valueAfter(lines, "description:");
                int metadataEnd = secondDelimiterIndex(lines);
                List<String> metadataLines = lines.subList(0, metadataEnd);
                List<String> triggers = metadataLines.stream()
                        .filter(line -> line.trim().startsWith("- "))
                        .map(line -> line.trim().substring(2))
                        .toList();
                int bodyStart = metadataEnd + 1;
                String instruction = String.join("\n", lines.subList(bodyStart, lines.size())).trim();
                return new SkillDefinition(name, description, triggers, instruction, skillFile);
            } catch (IOException e) {
                throw new IllegalStateException("failed to parse " + skillFile, e);
            }
        }

        private static String valueAfter(List<String> lines, String prefix) {
            return lines.stream()
                    .filter(line -> line.startsWith(prefix))
                    .map(line -> line.substring(prefix.length()).trim())
                    .findFirst()
                    .orElse("");
        }

        private static int secondDelimiterIndex(List<String> lines) {
            int count = 0;
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).equals("---") && ++count == 2) {
                    return i;
                }
            }
            return lines.size() - 1;
        }
    }
}
