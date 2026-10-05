package com.example.javaskillruntime;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class JavaSkillRuntimeDemo {

    public static void main(String[] args) {
        SkillDefinition skill = new SkillDefinition(
                "spring-boot-api-review",
                "1.0.0",
                true,
                false,
                false,
                "tenant-1",
                "skills/spring-boot-api-review/SKILL.md",
                "先确认接口契约，再检查兼容性、权限、异常处理和测试覆盖。"
        );

        SkillRegistry registry = new SkillRegistry();
        registry.register(skill);

        SkillRuntime runtime = new SkillRuntime(
                registry,
                new SkillRouter(registry),
                new SkillContextBuilder(200),
                new SkillExecutionPolicy()
        );

        SkillRequest request = new SkillRequest("tenant-1", "帮我评审订单接口变更，重点看权限和兼容性", false, false);
        Optional<SkillExecutionPlan> plan = runtime.plan(request);
        System.out.println(plan.orElseThrow());
    }

    static class SkillRuntime {
        private final SkillRouter router;
        private final SkillContextBuilder contextBuilder;
        private final SkillExecutionPolicy policy;

        SkillRuntime(SkillRegistry registry, SkillRouter router, SkillContextBuilder contextBuilder, SkillExecutionPolicy policy) {
            this.router = router;
            this.contextBuilder = contextBuilder;
            this.policy = policy;
        }

        Optional<SkillExecutionPlan> plan(SkillRequest request) {
            return router.route(request.taskText())
                    .filter(skill -> policy.canUse(request, skill))
                    .map(skill -> contextBuilder.build(request, skill));
        }
    }

    static class SkillRegistry {
        private final Map<String, List<SkillDefinition>> skillsByName = new ConcurrentHashMap<>();

        void register(SkillDefinition skill) {
            skillsByName.merge(skill.name(), List.of(skill), (oldValue, newValue) -> {
                var merged = new java.util.ArrayList<SkillDefinition>(oldValue);
                merged.addAll(newValue);
                return List.copyOf(merged);
            });
        }

        List<SkillDefinition> allEnabled() {
            return skillsByName.values().stream()
                    .flatMap(List::stream)
                    .filter(SkillDefinition::enabled)
                    .toList();
        }

        Optional<SkillDefinition> latestEnabled(String name) {
            return skillsByName.getOrDefault(name, List.of()).stream()
                    .filter(SkillDefinition::enabled)
                    .max(Comparator.comparing(SkillDefinition::version));
        }
    }

    static class SkillRouter {
        private final SkillRegistry registry;

        SkillRouter(SkillRegistry registry) {
            this.registry = registry;
        }

        Optional<SkillDefinition> route(String taskText) {
            return registry.allEnabled().stream()
                    .filter(skill -> taskText.contains("接口") || taskText.toLowerCase().contains(skill.name().replace("-", " ")))
                    .findFirst()
                    .or(() -> registry.latestEnabled("spring-boot-api-review"));
        }
    }

    static class SkillExecutionPolicy {
        boolean canUse(SkillRequest request, SkillDefinition skill) {
            if (!skill.enabled()) {
                return false;
            }
            if (skill.requiresWriteAccess() && !request.allowWrite()) {
                return false;
            }
            if (skill.requiresNetwork() && !request.allowNetwork()) {
                return false;
            }
            // Skill 可能包含企业内部规范，不能跨租户复用。
            return request.tenantId().equals(skill.tenantId());
        }
    }

    static class SkillContextBuilder {
        private final int maxInstructionCharacters;

        SkillContextBuilder(int maxInstructionCharacters) {
            this.maxInstructionCharacters = maxInstructionCharacters;
        }

        SkillExecutionPlan build(SkillRequest request, SkillDefinition skill) {
            String instruction = skill.instruction();
            if (instruction.length() > maxInstructionCharacters) {
                instruction = instruction.substring(0, maxInstructionCharacters);
            }
            return new SkillExecutionPlan(skill.name(), skill.version(), skill.source(), instruction, request.taskText());
        }
    }

    record SkillDefinition(
            String name,
            String version,
            boolean enabled,
            boolean requiresWriteAccess,
            boolean requiresNetwork,
            String tenantId,
            String source,
            String instruction
    ) {
    }

    record SkillRequest(String tenantId, String taskText, boolean allowWrite, boolean allowNetwork) {
    }

    record SkillExecutionPlan(String skillName, String version, String source, String instruction, String taskText) {
    }
}
