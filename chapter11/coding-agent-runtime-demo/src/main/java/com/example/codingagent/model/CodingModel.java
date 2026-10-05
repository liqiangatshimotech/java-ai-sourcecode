package com.example.codingagent.model;

import com.example.codingagent.config.ModelSettings;
import com.example.codingagent.domain.CodeChangeProposal;
import com.example.codingagent.domain.CodingTask;

/**
 * 大模型抽象。
 * 接入 DeepSeek、Ollama 或 ChatGPT 时，只需要把这个接口换成真实 HTTP 调用实现。
 */
public interface CodingModel {

    CodeChangeProposal proposeChange(CodingTask task, String targetPath, String currentContent,
                                     ModelSettings settings);
}
