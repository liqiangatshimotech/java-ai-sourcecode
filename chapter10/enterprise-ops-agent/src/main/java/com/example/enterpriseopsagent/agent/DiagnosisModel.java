package com.example.enterpriseopsagent.agent;

import com.example.enterpriseopsagent.domain.DiagnosisReport;
import com.example.enterpriseopsagent.domain.OpsAlert;

/**
 * 排障模型接口。
 *
 * 真实项目里，这个接口可以由 LangChain4j AI Service、Spring AI ChatClient、
 * DeepSeek HTTP Client 或 OpenAI Client 实现。先抽象接口，是为了避免业务流程
 * 和某个模型 SDK 绑定死。
 */
public interface DiagnosisModel {
    DiagnosisReport generateReport(OpsAlert alert, String runbookContext, String evidence);
}
