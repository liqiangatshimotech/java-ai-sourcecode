# AI Production Readiness Demo

项目名称：`ai-production-readiness-demo`

这个项目对应 `12｜AI 应用生产化`，用纯 Java 演示 AI 应用上线前必须补齐的治理层：

- `CostControlDemo`：Token 估算、模型路由和预算检查。
- `ObservabilityDemo`：TraceId、Agent Step、Tool Call 和审计日志。
- `EvaluationDemo`：离线评测集、工具调用评测和回归测试结果。
- `SecurityComplianceDemo`：敏感信息脱敏、Prompt Injection 检测和高风险工具审批。
- `ProductionReadinessDemo`：把成本、观测、评测、安全合并成上线前检查报告。

默认模型供应商是 DeepSeek，同时保留 Ollama 和 ChatGPT 配置入口。原有五个 Demo 用于离线验证治理规则；新增 CustomerServiceEvaluationDemo 会真实调用第 10.2 章客服接口，由客服服务调用已配置的模型。

## 运行

```bash
mvn -f chapter12/ai-production-readiness-demo/pom.xml exec:java
```

切换供应商只影响配置摘要，不会发起真实模型请求：

```bash
export AI_PROVIDER=deepseek
export DEEPSEEK_MODEL=deepseek-v4-flash

export AI_PROVIDER=ollama
export OLLAMA_MODEL=qwen3

export AI_PROVIDER=chatgpt
export OPENAI_MODEL=gpt-4o-mini
```

## 测试

```bash
mvn -f chapter12/ai-production-readiness-demo/pom.xml test
```



## 真实客服接口的批量门禁

项目名称：`ai-production-readiness-demo`，路径：`java-ai-course-examples/chapter12/ai-production-readiness-demo`。

我保留了离线评分示例，同时把 `CustomerServiceReleaseGate` 接到 `ai-customer-service-system`。每条 JSON 用例独立请求 `/api/support/chat`，再通过 requestId 读取 `/api/support/ai-runs/{requestId}`，取得实际模型、Prompt 版本和供应商 usage。先启动使用测试数据的客服实例，配置默认 DeepSeek 或 Ollama/OpenAI；客服凭据通过环境变量注入，不写入代码和结果文件。

```bash
export CUSTOMER_SERVICE_BASE_URL=http://127.0.0.1:8080
# SUPPORT_ACCESS_TOKEN 从本机环境注入，与客服端配置的身份凭据对应。
export EVAL_MIN_PASS_RATE=1.0
export EVAL_MAX_P95_MS=35000
mvn -f chapter12/ai-production-readiness-demo/pom.xml compile exec:java -Dexec.mainClass=com.example.aiproduction.demo.CustomerServiceEvaluationDemo
```

固定用例位于 `src/main/resources/evaluation/customer-service.json`，覆盖发票、退款人工确认和跨客户物流；通过 `EVAL_DATASET` 指定自己的 JSON 文件。`expectedTool`、答案关键词、是否转人工、禁止出现的承诺共同组成断言；带 `redLine=true` 的用例只要失败一条就阻止发布，不能用其他普通题的高分抵消。

逐题结果立即写入 `output/eval-时间戳.ndjson`，汇总保存在同名前缀的 `.summary.json`；`EVAL_OUTPUT` 可以覆盖输出位置。已有结果文件不会覆盖。连接/超时/响应错误都计入失败分母；未返回 usage 时记 null，不用字数冒充 Token。质量与 p95 门禁未通过会抛异常，让 Maven 和 CI 返回失败。

这些规则断言能发现明确回归，不能替代人工质量标注，也没有把未知 Token 换算成费用。成本模块中的价格仍是演示输入，真实费用应采用对应账期和模型价格另行计算。默认离线 `ProductionReadinessDemo` 的字符估算和内存 Trace 与真实接口批量验收是两个不同入口。

评测只能对隔离的测试实例执行，因为部分用例会创建测试工单。自动测试通过本地 HTTP mock 验证逐题独立请求、NDJSON 落盘、网络错误计入失败、红线覆盖平均分以及禁止覆盖旧报告；不把 mock 通过宣称为真实模型质量通过。
