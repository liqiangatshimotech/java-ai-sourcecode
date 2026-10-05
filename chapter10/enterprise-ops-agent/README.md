# enterprise-ops-agent

企业级智能运维排障 Agent 服务。

这个项目模拟一个企业内部的告警排障系统：告警平台把线上告警推给 Agent，Agent 自动检索 Runbook、查询日志指标和发布记录，生成结构化排障报告；如果建议涉及回滚这类高风险动作，系统只创建审批单，不直接执行生产变更。排障事件默认写入 MySQL，日志、监控、发布和审批暂时使用内存实现，后续可以替换成企业内部平台。

## 业务需求

我按真实项目补了这些需求：

- 告警接入：外部告警平台通过 HTTP 提交告警。
- 幂等处理：同一个 `alertId` 重复上报时返回同一个排障事件，不重复创建工单。
- Agent 诊断：自动查询 Runbook、日志、指标和最近发布记录，生成结构化报告。
- 审批收口：P1 回滚建议只创建审批单，等待人工决策。
- 事件查询：按状态、服务、环境筛选排障事件。
- 生命周期推进：支持审批通过、审批拒绝、事件关闭。
- SLA 管理：按告警等级计算响应截止时间，并返回是否超时。
- 审计回放：每个事件保存时间线，记录告警接入、诊断、审批和关闭动作。

## 代码结构

- `domain`：告警、诊断报告、排障事件、状态和时间线。
- `agent`：Runbook 检索、工具调用、模型生成和审计日志。
- `incident`：排障事件工作流、MySQL 仓储、SLA 和编号生成。
- `tool`：日志、指标、发布记录和审批系统的工具接口。
- `web`：REST API、请求响应对象和异常处理。
- `config`：Spring Bean 装配和模型配置。

## 本地 MySQL 环境

项目默认连接本机 Docker MySQL：

- 容器名：`enterprise-ops-mysql`
- 宿主机端口：`3307`
- 数据库：`enterprise_ops_agent`
- 用户名：`ops_agent`
- 密码：`ops_agent_123`

启动 MySQL：

```bash
docker compose -f chapter10/enterprise-ops-agent/docker/mysql/docker-compose.yml up -d
```

等待 MySQL 就绪：

```bash
docker exec enterprise-ops-mysql mysqladmin ping -h 127.0.0.1 -uroot -proot123456 --silent
```

执行建库建表脚本：

```bash
docker exec -i enterprise-ops-mysql mysql -uroot -proot123456 \
  < chapter10/enterprise-ops-agent/src/main/resources/db/mysql/01_schema.sql
```

确认表结构：

```bash
docker exec enterprise-ops-mysql mysql -uops_agent -pops_agent_123 enterprise_ops_agent \
  -e 'SHOW TABLES;'
```

停止 MySQL：

```bash
docker compose -f chapter10/enterprise-ops-agent/docker/mysql/docker-compose.yml down
```

如果要同时删除本地数据卷：

```bash
docker compose -f chapter10/enterprise-ops-agent/docker/mysql/docker-compose.yml down -v
```

## 本地启动服务

在 `java-ai-course-examples` 目录下执行：

```bash
SERVER_PORT=18080 AI_PROVIDER=deepseek \
mvn -f chapter10/enterprise-ops-agent/pom.xml -am spring-boot:run
```

默认数据库配置来自 `application.yml`：

- `MYSQL_URL=jdbc:mysql://127.0.0.1:3307/enterprise_ops_agent?...`
- `MYSQL_USERNAME=ops_agent`
- `MYSQL_PASSWORD=ops_agent_123`
- `OPS_STORAGE=mysql`

如果只是想离线演示，不写 MySQL，可以临时切回内存模式：

```bash
OPS_STORAGE=memory SERVER_PORT=18080 \
mvn -f chapter10/enterprise-ops-agent/pom.xml -am spring-boot:run
```

健康检查：

```bash
curl -s http://localhost:18080/actuator/health
```

查看当前模型运行配置：

```bash
curl -s http://localhost:18080/api/ops/runtime
```

## 接入告警并创建排障事件

```bash
curl -s -X POST http://localhost:18080/api/ops/alerts \
  -H 'Content-Type: application/json' \
  -d '{
    "alertId": "ALERT-HTTP-001",
    "serviceName": "payment-service",
    "environment": "prod",
    "severity": "P1",
    "title": "支付服务错误率升高",
    "description": "支付回调接口出现大量 timeout，P95 延迟明显升高。"
  }'
```

响应里会返回：

- `incidentId`：排障事件编号。
- `status`：当前状态，P1 回滚建议会进入 `WAITING_APPROVAL`。
- `approvalTicketId`：审批单号。
- `slaDeadline`：响应截止时间。
- `report`：Agent 生成的结构化排障报告。
- `timeline`：事件时间线。

确认数据已写入 MySQL：

```bash
docker exec enterprise-ops-mysql mysql -uops_agent -pops_agent_123 enterprise_ops_agent \
  -e "SELECT incident_id, alert_id, status, duplicate_count FROM ops_incident ORDER BY created_at DESC LIMIT 5;"
```

## 查询排障事件

查询单个事件：

```bash
curl -s http://localhost:18080/api/ops/incidents/INC-20260611-XXXXXXXX
```

按状态、服务、环境筛选：

```bash
curl -s 'http://localhost:18080/api/ops/incidents?status=WAITING_APPROVAL&serviceName=payment-service&environment=prod'
```

## 审批和关闭

审批通过：

```bash
curl -s -X POST http://localhost:18080/api/ops/incidents/INC-20260611-XXXXXXXX/decision \
  -H 'Content-Type: application/json' \
  -d '{
    "decision": "APPROVE",
    "operator": "sre-zhangsan",
    "comment": "同意回滚到上一稳定版本"
  }'
```

关闭事件：

```bash
curl -s -X POST http://localhost:18080/api/ops/incidents/INC-20260611-XXXXXXXX/resolve \
  -H 'Content-Type: application/json' \
  -d '{
    "operator": "sre-zhangsan",
    "summary": "回滚后错误率恢复到 0.3%"
  }'
```

## 同步诊断入口

如果只想快速看 Agent 报告，可以调用同步诊断接口。这个接口不会创建 incident，也不会推进审批状态。

```bash
curl -s -X POST http://localhost:18080/api/ops/diagnose \
  -H 'Content-Type: application/json' \
  -d '{
    "alertId": "ALERT-DIAGNOSE-001",
    "serviceName": "payment-service",
    "environment": "prod",
    "severity": "P1",
    "title": "支付服务错误率升高",
    "description": "支付回调接口出现大量 timeout，P95 延迟明显升高。"
  }'
```

## 切换模型配置

默认使用 DeepSeek：

```bash
AI_PROVIDER=deepseek SERVER_PORT=18080 mvn -f chapter10/enterprise-ops-agent/pom.xml -am spring-boot:run
```

切换到 Ollama：

```bash
AI_PROVIDER=ollama SERVER_PORT=18080 mvn -f chapter10/enterprise-ops-agent/pom.xml -am spring-boot:run
```

切换到 ChatGPT / OpenAI：

```bash
AI_PROVIDER=chatgpt SERVER_PORT=18080 mvn -f chapter10/enterprise-ops-agent/pom.xml -am spring-boot:run
```

当前版本先用规则模型模拟报告生成，不会真实请求外部模型。后续接入 DeepSeek、Ollama 或 ChatGPT 时，只需要替换 `DiagnosisModel` 的实现。

## 打包部署

打包为可执行 JAR：

```bash
mvn -f chapter10/enterprise-ops-agent/pom.xml -am clean package
```

运行 JAR：

```bash
SERVER_PORT=18080 AI_PROVIDER=deepseek \
java -jar chapter10/enterprise-ops-agent/target/enterprise-ops-agent-0.0.1-SNAPSHOT.jar
```

## 容器运行

先打包：

```bash
mvn -f chapter10/enterprise-ops-agent/pom.xml -am clean package
```

构建镜像：

```bash
docker build -t enterprise-ops-agent:0.0.1 chapter10/enterprise-ops-agent
```

运行容器：

```bash
docker run --rm -p 18080:8080 \
  -e AI_PROVIDER=deepseek \
  -e MYSQL_URL='jdbc:mysql://host.docker.internal:3307/enterprise_ops_agent?useUnicode=true&characterEncoding=utf8&connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true' \
  -e MYSQL_USERNAME=ops_agent \
  -e MYSQL_PASSWORD=ops_agent_123 \
  enterprise-ops-agent:0.0.1
```

## 命令行入口

如果只想看控制台输出，也可以运行命令行入口：

```bash
mvn -f chapter10/enterprise-ops-agent/pom.xml exec:java
```

## 测试

```bash
mvn -f chapter10/enterprise-ops-agent/pom.xml -am test
```

当前测试覆盖 19 个用例：

- 告警入参校验。
- DeepSeek、Ollama、ChatGPT / OpenAI 模型配置解析。
- 工具调用的查询时间窗口限制。
- Agent 排障闭环和审计日志写入。
- 排障事件创建、幂等、审批、关闭和 SLA。
- HTTP API、健康检查、查询筛选、请求参数校验和错误响应。

测试默认使用 `OPS_STORAGE=memory`，不会依赖本地 MySQL。真实 MySQL 链路用上面的 Docker MySQL 和接口命令验证。


## 2026-09-09：真实诊断模型

默认 RUN_MODEL_CALL=false 使用规则模型；设置 RUN_MODEL_CALL=true 后由 HttpDiagnosisModel 发起真实模型请求，失败不会回退规则文本。默认 AI_PROVIDER=deepseek，也支持 ollama、openai/chatgpt。密钥只从 DEEPSEEK_API_KEY / OPENAI_API_KEY 读取；模型与地址可分别通过对应 MODEL、BASE_URL 环境变量覆盖。

```bash
cd java-ai-course-examples
OPS_STORAGE=memory AI_PROVIDER=deepseek RUN_MODEL_CALL=true mvn -f chapter10/enterprise-ops-agent/pom.xml spring-boot:run
mvn -f chapter10/enterprise-ops-agent/pom.xml test
```

真实模型只归纳证据；告警ID、原始工具证据、需要人工审批由后端确定。证据为空直接明确缺少证据，不让模型编造根因。HttpDiagnosisModelTest 用本地HTTP替身验证JSON契约、HTTP429、截断、无效响应和日志隐藏密钥。外部日志/指标工具仍是示例数据，不能当成已经接入企业监控。
