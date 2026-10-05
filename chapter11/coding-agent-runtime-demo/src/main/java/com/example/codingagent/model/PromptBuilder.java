package com.example.codingagent.model;

import com.example.codingagent.config.ModelSettings;
import com.example.codingagent.domain.CodingTask;

/**
 * Coding Agent 的 Prompt 不应该只是用户原话。
 * 这里把任务、模型配置、文件路径和当前代码一起放进去，方便模型生成可审查的补丁。
 */
public final class PromptBuilder {

    public String buildPatchPrompt(CodingTask task, String targetPath, String currentContent,
                                   ModelSettings settings) {
        return """
                你是一个 Java Coding Agent。请只修改必要代码，并保持公开方法签名不变。

                运行模型配置：
                %s

                任务：
                %s

                目标文件：
                %s

                当前代码：
                ```java
                %s
                ```
                """.formatted(settings.summary(), task.description(), targetPath, currentContent);
    }
}
