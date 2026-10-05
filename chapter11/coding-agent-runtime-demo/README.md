# coding-agent-runtime-demo

项目名称：`coding-agent-runtime-demo`

这个项目对应 `11｜自研 Java Coding Agent`。我把一个 Coding Agent 拆成了几块能落地的 Java 代码：模型配置、任务会话、工具注册、文件边界、安全审批、代码写入、测试执行和 diff 交付。现在它不再只是命令行 demo，而是一个可以直接打开的 Spring Boot 管理台，能从页面提交任务、审批、运行 Agent、查看步骤、审计日志、源码和 diff。

默认用 `RuleBasedCodingModel` 离线生成固定业务提案。显式设置 `RUN_MODEL_CALL=true` 后，管理台使用 `HttpCodingModel` 调用 DeepSeek、Ollama 或 ChatGPT 的兼容接口；真实模式失败会报错，不会自动切回规则答案。测试执行始终需要 Docker Engine 与预置的 `eclipse-temurin:21-jdk` 镜像；缺少容器环境时会失败，不会在宿主运行模型代码。

## 业务场景

示例会创建一个临时业务代码仓库：`target/coding-agent-workspace`。里面有一个计费类 `PriceCalculator`，会员折扣上限原来是 20%。提交任务后，Agent 会把折扣上限调整为 30%，并且保持方法签名不变。

完整链路限定为 `PriceCalculator.java` 单文件计费改造，不克隆任意仓库、不让模型自由选择文件或命令。仓库名、分支名只是任务标签。

- 创建任务时读取受控源码与测试快照，生成具体提案；源码尚未写入。
- 页面先展示完整 diff；API 还返回 `proposal.newContent`、`proposalId` 和 `sourceVersion`。
- 审批请求必须带当前提案 ID 与源版本摘要；源码或测试变化后原审批失效。
- 执行时只写入保存的提案，不再次调用模型。相同提案的重复/并发执行最多进行一次。
- 固定 Docker 命令真实执行 `javac -proc:none` 与业务测试入口。容器无网络、只读输入、非 root，限制内存/CPU/PID/45秒时间/16KB输出。
- 测试通过才标记 `COMPLETED`。编译失败、业务断言失败、超时或输出超限都不会标成通过。
- 实际测试失败时最多生成一次修复提案，回到 `WAITING_APPROVAL`；新提案不能继承旧批准。Docker 不可用时先修环境，不用模型改代码掩盖故障。

任务、幂等记录和审计仍保存在进程内存，重启不会恢复；未接入登录授权、真实 Git 分支或 PR。工作区不能由不可信宿主进程并发改动，本示例的路径检查不宣称解决操作系统层面的所有竞争条件。

## 代码结构

- `config`：DeepSeek、Ollama、ChatGPT 三类模型配置。
- `domain`：任务、会话、步骤、审批和改动提案。
- `model`：规则模型、真实 HTTP JSON 适配器、Prompt 和有界响应接收。
- `runtime`：Agent Runtime 主流程、审批服务、审计日志和风险策略。
- `tool`：`list_files`、`search_code`、`read_file`、`write_file`、`run_tests`、`git_diff`。
- `workspace`：工作区边界、快照、diff 和演示仓库生成。
- `service`：任务中心、状态流转和工作区管理。
- `web`：Spring Boot REST API、请求响应对象和异常处理。
- `static`：企业化管理台页面。

## 运行

在 `java-ai-course-examples` 目录下执行：

```bash
mvn -f chapter11/coding-agent-runtime-demo/pom.xml spring-boot:run
```

打开管理台：

```text
http://localhost:18081
```

健康检查：

```bash
curl -s http://localhost:18081/actuator/health
```

如果只想运行原来的命令行链路：

```bash
mvn -q -f chapter11/coding-agent-runtime-demo/pom.xml compile exec:java
```

## HTTP API

查看运行配置：

```bash
curl -s http://localhost:18081/api/coding-agent/runtime
```

提交任务：

```bash
curl -s -X POST http://localhost:18081/api/coding-agent/tasks \
  -H 'Content-Type: application/json' \
  -d '{
    "repositoryName": "billing-service",
    "branchName": "feature/member-discount-limit",
    "operator": "course-demo",
    "description": "把会员折扣上限从 20% 调整为 30%，同时不要修改 calculateFinalPrice 的方法签名。"
  }'
```

审批任务：

```bash
curl -s -X POST http://localhost:18081/api/coding-agent/tasks/CA-XXXXXXXXXXXXXX-1001/approve \
  -H 'Content-Type: application/json' \
  -d '{
    "proposalId": "替换为当前详情返回的proposal.proposalId",
    "sourceVersion": "替换为当前详情返回的proposal.sourceVersion",
    "approver": "tech-lead",
    "comment": "确认需求明确，允许在隔离工作区执行写入和测试。"
  }'
```

运行任务：

```bash
curl -s -X POST http://localhost:18081/api/coding-agent/tasks/CA-XXXXXXXXXXXXXX-1001/run \
  -H 'Content-Type: application/json' \
  -d '{"proposalId":"替换为当前详情返回的proposal.proposalId"}'
```

真实模型调用默认关闭。管理台启用真实 DeepSeek 时，设置 `RUN_MODEL_CALL=true` 并由本机环境注入 `DEEPSEEK_API_KEY`。命令行 demo 固定使用规则模型和自动审批，只用于预置计费示例，不用于真实模型自动批准。

默认供应商是 DeepSeek：

```bash
export AI_PROVIDER=deepseek
export DEEPSEEK_MODEL=deepseek-v4-flash
export DEEPSEEK_BASE_URL=https://api.deepseek.com
```

切换到 Ollama：

```bash
export AI_PROVIDER=ollama
export OLLAMA_MODEL=qwen3
export OLLAMA_BASE_URL=http://localhost:11434/v1
```

切换到 ChatGPT / OpenAI：

```bash
export AI_PROVIDER=chatgpt
export OPENAI_MODEL=gpt-4o-mini
export OPENAI_BASE_URL=https://api.openai.com/v1
```

## 测试

```bash
mvn -f chapter11/coding-agent-runtime-demo/pom.xml test
```

普通测试用本机假 HTTP 与容器测试桩覆盖编排边界，不调用外部模型、不执行模型代码。测试桩通过不等于真实容器通过。

测试覆盖：

- 模型配置默认值和供应商切换。
- 工作区路径穿越拦截。
- 提案先审阅后写入、源版本变化拒绝、重复并发执行幂等、一次修复需要重新审批。
- HTTP JSON、字段类型、拒答、截断、超时、超大响应、路径变更与错误脱敏。
- Docker 固定命令能力边界、环境不可用、超时、输出超限和真实管道捕获上限。

启动 Docker 后先准备镜像，再显式执行真实容器集成验收：

```bash
docker pull eclipse-temurin:21-jdk
RUN_DOCKER_TESTS=true mvn -f chapter11/coding-agent-runtime-demo/pom.xml -Dtest=DockerBillingIntegrationTest test
```

这个测试先要求原始 20% 代码在真实容器中失败，再要求 30% 版本通过。环境不可用时测试失败，不能把它当成预期业务失败。本轮本机 Docker socket 不存在，尚未完成真实容器成功路径和真实模型质量验收。

## 打包

```bash
mvn -f chapter11/coding-agent-runtime-demo/pom.xml clean package
```

运行 JAR：

```bash
SERVER_PORT=18081 java -jar chapter11/coding-agent-runtime-demo/target/coding-agent-runtime-demo-0.0.1-SNAPSHOT.jar
```
