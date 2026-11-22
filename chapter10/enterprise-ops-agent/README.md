# enterprise-ops-agent

企业级智能运维排障 Agent 服务。

这个项目模拟一个企业内部的告警排障系统：告警平台把线上告警推给 Agent，Agent 自动检索 Runbook、查询日志指标和发布记录，生成结构化排障报告；如果建议涉及回滚这类高风险动作，系统只创建审批单，不直接执行生产变更。排障事件默认写入 MySQL，日志、监控、发布和审批暂时使用内存实现，后续可以替换成企业内部平台。

运行环境：JDK 21、Maven 3.9+，本机 MySQL 用 Docker Compose 启动。下面所有命令都在 `java-ai-course-examples` 目录下执行，服务端口统一用 `18080`。

## 业务需求

我按真实项目补了这些需求：

- 告警接入：外部告警平台通过 HTTP 提交告警。
- 幂等处理：同一个 `alertId` 重复上报时返回同一个排障事件，不重复创建工单。
- Agent 诊断：自动查询 Runbook、日志、指标和最近发布记录，生成结构化报告。
- 审批收口：要不要审批由 `ApprovalPolicy` 在 Java 里判定，不听模型的。处理建议里出现回滚、重启、扩缩容、切流、限流这类变更动作，或者告警来自生产环境且级别是 P0 / P1，就只创建审批单，等待人工决策；都不命中时事件停在 `DIAGNOSED`。
- 事件查询：按状态、服务、环境筛选排障事件。
- 生命周期推进：支持审批通过、审批拒绝、事件关闭。
- SLA 管理：按告警等级计算响应截止时间（P0 5 分钟、P1 15 分钟、P2 30 分钟、P3 2 小时，其他 4 小时），并返回是否超时。
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

内存模式只是把事件仓储换成内存实现，重启后数据就没了。数据源配置还在，所以没启动 MySQL 时业务接口可以正常调用，但 `/actuator/health` 会因为连不上数据库返回 `DOWN`。

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
- `status`：当前状态。这条告警来自生产环境且级别是 P1，会进入 `WAITING_APPROVAL`；不需要审批的告警是 `DIAGNOSED`。
- `approvalTicketId`：审批单号。
- `slaDeadline`：响应截止时间。
- `report`：Agent 生成的结构化排障报告。
- `timeline`：事件时间线。

确认数据已写入 MySQL：

```bash
docker exec enterprise-ops-mysql mysql -uops_agent -pops_agent_123 enterprise_ops_agent \
  -e "SELECT incident_id, alert_id, status, duplicate_count FROM ops_incident ORDER BY created_at DESC LIMIT 5;"
```

查看时间线：

```bash
docker exec enterprise-ops-mysql mysql -uops_agent -pops_agent_123 enterprise_ops_agent \
  -e "SELECT event_type, event_operator, message FROM ops_incident_timeline ORDER BY id DESC LIMIT 10;"
```

再提交一条不需要审批的告警（预发环境、P3），可以看到另一条分支：响应里的 `status` 是 `DIAGNOSED`，`approvalTicketId` 为 `null`，时间线只有告警接入和诊断两条。

```bash
curl -s -X POST http://localhost:18080/api/ops/alerts \
  -H 'Content-Type: application/json' \
  -d '{
    "alertId": "ALERT-LOW-001",
    "serviceName": "payment-service",
    "environment": "staging",
    "severity": "P3",
    "title": "支付服务错误率小幅波动",
    "description": "预发环境支付回调偶发 timeout。"
  }'
```

## 查询排障事件

下面命令里的 `INC-20260611-XXXXXXXX` 是占位，要换成接入告警时返回的 `incidentId`（格式是 `INC-当天日期-8 位随机串`，日期按 UTC 计算），照抄会返回 404。

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

状态流转规则写在 `DiagnosisIncident` 里：`/decision` 只对 `WAITING_APPROVAL` 的事件有效，通过变成 `APPROVED`，拒绝变成 `REJECTED`；`/resolve` 可以关闭 `DIAGNOSED`、`WAITING_APPROVAL`、`APPROVED` 的事件，`REJECTED` 的事件不能再关闭。不符合规则的操作返回 409。

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

上面三条命令只切换供应商配置。默认 `RUN_MODEL_CALL=false`，出报告的还是规则模型，`/api/ops/runtime` 可以看到当前的 provider 和 model。要真的请求模型，再加上 `RUN_MODEL_CALL=true`，见文末“真实诊断模型”一节。

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

运行容器。容器里的 `127.0.0.1` 是容器自己，所以要把数据库地址指到宿主机的 3307 端口（Linux 上还要加 `--add-host=host.docker.internal:host-gateway`）：

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

当前测试覆盖 35 个用例：

- 告警入参校验（告警 ID、服务名、环境不能为空，触发时间默认值）。
- DeepSeek、Ollama、ChatGPT / OpenAI 模型配置解析。
- 工具调用的查询时间窗口限制。
- Agent 排障闭环和审计日志写入。
- 审批判定规则：需要审批和不需要审批两条分支。
- SLA 响应时限：P0 比 P1 更紧，未知级别按 4 小时。
- 排障事件创建、幂等、审批、关闭，以及不需要审批时停在 `DIAGNOSED`。
- 真实模型适配：JSON 请求格式、HTTP 429、响应截断、无效 JSON、缺少 API Key，以及审批结论由后端判定。
- HTTP API、查询筛选、请求参数校验和错误响应。

测试用的是内存仓储（`OpsDiagnosisControllerTest` 把 `ops.storage.type` 设成 `memory`，并排除了数据源自动配置），不会依赖本地 MySQL。真实 MySQL 链路用上面的 Docker MySQL 和接口命令验证，`MySqlIncidentRepository` 目前没有自动化测试。


## 2026-09-09：真实诊断模型

默认 RUN_MODEL_CALL=false 使用规则模型；设置 RUN_MODEL_CALL=true 后由 HttpDiagnosisModel 发起真实模型请求，失败不会回退规则文本。默认 AI_PROVIDER=deepseek，也支持 ollama、openai/chatgpt。密钥只从 DEEPSEEK_API_KEY / OPENAI_API_KEY 读取（Ollama 默认连本机 `http://localhost:11434/v1` 的 `qwen2.5:7b`，不需要密钥）；模型与地址可分别通过 `DEEPSEEK_MODEL` / `DEEPSEEK_BASE_URL` 这类环境变量覆盖，Ollama、OpenAI 换成对应前缀。

规则模型，离线演示：

```bash
OPS_STORAGE=memory RUN_MODEL_CALL=false SERVER_PORT=18080 \
mvn -f chapter10/enterprise-ops-agent/pom.xml -am spring-boot:run
```

真实模型（先在本机环境变量里配置 `DEEPSEEK_API_KEY`；和上一条二选一，占用同一个端口）：

```bash
OPS_STORAGE=memory AI_PROVIDER=deepseek RUN_MODEL_CALL=true SERVER_PORT=18080 \
mvn -f chapter10/enterprise-ops-agent/pom.xml -am spring-boot:run
```

云端模型没配密钥时服务启动就会失败；请求过程中模型出错，接口返回带 `MODEL_` 前缀错误码的失败响应（例如 `MODEL_HTTP_429`、`MODEL_TIMEOUT`），不会回退规则答案。

真实模型只归纳证据；告警 ID、原始工具证据、是否需要人工审批由后端确定（`ApprovalPolicy`），模型返回里即使带了审批字段也不读取。证据为空直接明确缺少证据，不让模型编造根因。HttpDiagnosisModelTest 用本地 HTTP 替身验证 JSON 契约、HTTP 429、截断、无效响应、审批判定和日志隐藏密钥。外部日志/指标工具仍是示例数据，不能当成已经接入企业监控。
