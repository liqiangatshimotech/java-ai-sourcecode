package com.example.codingagent.domain;

/**
 * 模型给出的代码修改提案。
 * Runtime 不直接相信模型输出，必须先经过审批，再通过受控工具写入工作区。
 */
public record CodeChangeProposal(
        String targetPath,
        String rationale,
        String newContent
) {
}
