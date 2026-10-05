package com.example.codingagent.model;

import com.example.codingagent.config.ModelSettings;
import com.example.codingagent.domain.CodeChangeProposal;
import com.example.codingagent.domain.CodingTask;

/**
 * 为了让示例在没有 API Key 的情况下也能跑通，这里用规则模型模拟一次大模型代码生成。
 * 真实项目中可以保留同样的入参和返回值，把内部替换成 DeepSeek/Ollama/ChatGPT 调用。
 */
public final class RuleBasedCodingModel implements CodingModel {

    private final PromptBuilder promptBuilder = new PromptBuilder();

    @Override
    public CodeChangeProposal proposeChange(CodingTask task, String targetPath, String currentContent,
                                            ModelSettings settings) {
        String prompt = promptBuilder.buildPatchPrompt(task, targetPath, currentContent, settings);
        String newContent = currentContent;

        if (task.description().contains("30") && currentContent.contains("new BigDecimal(\"0.20\")")) {
            newContent = currentContent.replace("new BigDecimal(\"0.20\")", "new BigDecimal(\"0.30\")");
        }

        String rationale = "根据任务要求调整会员折扣上限；Prompt 长度=" + prompt.length()
                + "，供应商=" + settings.provider().id();
        return new CodeChangeProposal(targetPath, rationale, newContent);
    }
}
