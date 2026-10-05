# text-to-sql-agent（Text-to-SQL 数据分析 Agent）

电商运营数据分析场景的教学项目。运营同学用自然语言提问（比如「2026年6月销售额最高的五个商品分类」），Agent 负责把问题变成可信的数字结论，完整闭环是：

**Schema Linking 选表 → 生成 SQL → 安全护栏校验 → 只读执行 → 结果翻译成中文结论 → 写审计日志。**

数据库是内嵌 H2（`schema.sql` + `data.sql` 启动自动初始化，12 个客户 / 16 个商品 / 40 张订单），不需要装 MySQL。没有任何 API Key 也能跑：缺 key 时自动落回内置的离线规则模型，测试和演示全部离线通过。

## 快速开始

环境要求：JDK 21+，Maven 3.6+。以下命令在解压后的 `java-ai-course-examples` 目录执行；首次构建需要从 Maven 仓库下载依赖。

```bash
# 跑全部测试（全部用例，完全离线，不需要任何 API Key）
mvn -f chapter10/text-to-sql-agent/pom.xml test

# 命令行离线演示：不启动 Web 服务，跑 4 个问题并打印每一步中间产物
mvn -f chapter10/text-to-sql-agent/pom.xml compile exec:java \
  -Dexec.mainClass=com.example.texttosqlagent.demo.TextToSqlAgentDemo

# 启动 Web 服务（默认 8080 端口）
mvn -f chapter10/text-to-sql-agent/pom.xml spring-boot:run
```

启动后可以这样提问：

```bash
curl -X POST http://localhost:8080/api/analysis/ask \
  -H 'Content-Type: application/json' \
  -d '{"question": "2026年6月销售额最高的五个商品分类"}'

# 查看 Agent「眼里」的库表结构和中文注释（调试选表用）
curl http://localhost:8080/api/analysis/schema
```

## 模型配置

课程默认用 DeepSeek 举例，同一套代码通过配置切换三家模型。密钥只从环境变量读，绝不写进代码。

| 供应商 | 启用方式 | 环境变量 | 默认模型 | 默认端点 |
| --- | --- | --- | --- | --- |
| DeepSeek（默认） | `AI_PROVIDER=deepseek`（不配就是它） | `DEEPSEEK_API_KEY`（必需）、`DEEPSEEK_BASE_URL`、`DEEPSEEK_MODEL` | `deepseek-v4-flash` | `https://api.deepseek.com` |
| Ollama（本机） | `AI_PROVIDER=ollama` | `OLLAMA_BASE_URL`、`OLLAMA_MODEL` | `qwen2.5:7b` | `http://localhost:11434/v1` |
| ChatGPT / OpenAI | `AI_PROVIDER=openai` 或 `chatgpt` | `OPENAI_API_KEY`（必需）、`OPENAI_BASE_URL`、`OPENAI_MODEL` | `gpt-4.1-mini` | `https://api.openai.com/v1` |

**没配 key 时会怎样？** 装配规则写在 `AgentConfiguration` 里：`text2sql.ai.offline=true`，或者选了需要密钥的供应商但环境变量没配 key，就自动落回 `RuleBasedSqlGenerationModel`（离线规则模型，内置四道黄金问题的标准 SQL）。启动日志会打一行说明走了哪条路径（只打 `safeSummary`，不会打密钥）。想强制离线，设 `TEXT2SQL_OFFLINE=true` 即可。

真实模型路径里，DeepSeek 和 ChatGPT 都走 OpenAI 兼容协议，所以一个 `OpenAiChatModel` 换 `baseUrl/apiKey/modelName` 就全覆盖了。

## 目录结构

```
src/main/java/com/example/texttosqlagent/
├── TextToSqlAgentApplication     # Spring Boot 入口
├── config/
│   ├── ModelProvider             # 模型供应商枚举
│   ├── ModelSettings             # 模型连接配置（key 只从环境变量读）
│   └── AgentConfiguration        # Bean 装配：离线/真实模型的选择规则在这里
├── schema/
│   ├── ColumnSchema / TableSchema        # 表结构元数据（含中文注释）
│   ├── SchemaInspector           # 通过 JDBC DatabaseMetaData 读表结构和 REMARKS
│   ├── MetricDictionary          # 业务指标词典：销售额/客单价/订单量 等口径
│   ├── SchemaLinkingService      # 选表：按表注释/列注释/指标关键词打分
│   └── SchemaLinkingResult       # 选表结果（表 + 命中关键词 + schema 上下文）
├── sql/
│   ├── SqlSafetyGuard            # 安全护栏（规则见下）
│   ├── SqlGuardResult            # 护栏校验结果
│   ├── ReadOnlySqlExecutor       # 只读执行器（超时 + 限量 + 截断标记）
│   └── QueryResult               # 结果集快照
├── agent/
│   ├── SqlGenerationModel        # SQL 生成模型抽象
│   ├── LangChain4jSqlGenerationModel  # 真实模型路径（OpenAI 兼容协议）
│   ├── RuleBasedSqlGenerationModel    # 离线规则模型（黄金问题替身）
│   ├── AnswerNarrator / TemplateAnswerNarrator  # 结果解说
│   ├── DataAnalysisAgentService  # 编排入口：把各环节串成完整链路
│   ├── AnalysisReport / AnalysisStatus          # 审计报告和状态机
│   └── AuditLogService / ConsoleAuditLogService # 审计日志
├── web/
│   ├── AnalysisController        # POST /api/analysis/ask、GET /api/analysis/schema
│   ├── AskRequest / AskResponse  # 请求/响应 DTO
│   └── ApiExceptionHandler       # 参数校验失败统一 400
└── demo/
    └── TextToSqlAgentDemo        # 命令行离线演示（手动装配，不走 Spring 容器）
```

## 护栏规则（SqlSafetyGuard）

我把 SQL 文本交给固定版本的 JSQLParser 4.9 生成 AST，再逐节点检查。解析器认识的语法比本项目开放的范围大，所以未知表达式和未验证的方言扩展一律拒绝。

1. 完整响应必须只包含一条查询；不再剥注释后按分号切分，也不在模型响应提取阶段偷偷丢弃第二条语句。
2. 普通 SELECT、非递归 CTE、子查询和集合查询递归检查；逗号连接与 JOIN 一样逐表授权。
3. 白名单是 `BusinessTablePolicy` 五张表与 PUBLIC 实际元数据的交集。发现表不代表获得权限。物理表写成 `PUBLIC.table`，拒绝其他 schema 同名表、跨库名称；双引号名称严格遵守 H2 大小写规则。
4. CTE 名称在其定义体检查通过后才在当前查询中可见，不全局豁免同名物理表；拒绝递归、前向引用和重复名称。
5. 聚合、数值与字符串函数使用显式白名单。文件函数、表函数、序列变更、锁定查询、SELECT INTO 和未验证的窗口扩展都拒绝。
6. 只修改最外层 LIMIT，保留子查询的 LIMIT 和用户 offset。无 LIMIT 或过大 LIMIT 时最多探测 `maxRows + 1` 行，执行器只返回 `maxRows` 行；第 201 行用来证明默认 200 行结果确实被截断。用户主动 LIMIT 5 则保留 5，这个查询本身只要求五行。

`ReadOnlyDatabaseFactory` 在局部管理员连接中建库，随后只向应用提供 `course_reader` 数据源，权限仅为五张表的 SELECT。Web 服务、命令行和集成测试都走这一条路径。`setReadOnly(true)` 是驱动提示，`executeQuery` 是结果类型约定，二者都不保证没有写入副作用。回归测试会绕过护栏用 `Statement.execute` 尝试 DELETE、UPDATE、INSERT、DROP、CREATE，然后确认 12 个客户和首行姓名未变。

行数上限不等于扫描成本上限。当前实现限制 SQL 长度、AST 嵌套、返回行数及 JDBC 查询超时；生产数据库还需要服务端 statement timeout、连接池/并发预算和资源隔离。完整授权与部署说明见 [生产数据库授权.md](生产数据库授权.md)。本项目尚未实现用户/租户行列权限；不能把五表授权当成多租户隔离。

## 黄金问题（离线规则模型内置）

| 问题 | 关键词 | 查询要点 |
| --- | --- | --- |
| 2026年6月销售额最高的五个商品分类 | 6月 + 销售额 + 分类/品类 | orders ⋈ order_items ⋈ products，排除退款，按分类汇总 |
| 累计消费金额最高的十个客户 | 客户 + 消费/累计 | customers ⋈ orders，排除退款，按客户汇总 |
| 2026年6月各支付渠道的金额占比 | 支付/渠道 + 占比/6月 | payments ⋈ orders，窗口函数算占比 |
| 每个商品分类的平均客单价 | 客单价 + 分类 | orders ⋈ order_items ⋈ products，AVG 订单总额 |

规则模型的关键词匹配只是离线替身——真实系统里这层语义理解是模型做的。规则模型答不上会返回 `CANNOT_ANSWER`，编排层据此诚实拒答，绝不硬编 SQL（幻觉兜底）。

## 状态机

| 状态 | 含义 |
| --- | --- |
| `ANSWERED` | 正常出数并生成中文解说 |
| `NO_MATCHING_TABLE` | 选表为空，诚实回复找不到相关表 |
| `CANNOT_ANSWER` | 模型/规则答不上，拒答而不是编 SQL |
| `BLOCKED_BY_GUARD` | SQL 被护栏拦截，从未执行（带违规明细） |
| `EXECUTION_FAILED` | SQL 合法但执行出错，返回脱敏说明 |

每一次请求（无论成败）都会写一行审计日志：requestId、问题、SQL、状态、行数、耗时、模型摘要。

## 测试

测试全部离线运行（`AnalysisControllerTest` 用 `text2sql.ai.offline=true` 钉死规则模型，就算 CI 机器配了 key 也不会真的调外部 API）：

- `ModelSettingsTest`：供应商解析各分支、默认值、非法值、密钥不泄露
- `SchemaInspectorTest`：5 张表、表/列注释都能通过 REMARKS 读回
- `SchemaLinkingServiceTest`：销售额问题选订单三件套不选客户表、客户消费选 customers/orders 等
- `SqlSafetyGuardTest`：每条护栏规则至少一个用例（DROP/DELETE/INSERT、多语句、非白名单表、最外层 LIMIT、CTE、注释/字符串保持原意）
- `ReadOnlySqlExecutorTest`：正常取数、截断标记、SELECT 账号执行器报错路径
- `DataAnalysisAgentServiceTest`：规则模型端到端、无关问题拒答、危险 SQL 被拦且未执行
- `AnalysisControllerTest`：合法问题 200、空问题 400、schema 接口返回 5 张表

- `SqlSecurityRegressionTest`：逗号表、quoted identifier、schema 同名、CTE 作用域、深层子查询、危险函数、多语句、LIMIT/offset 与真实截断；绕过护栏后数据库仍拒绝业务写入。
- `SqlResponseExtractionTest`：响应提取保留第二条语句和字符串里的分号，交给护栏完整判定。

本轮验收执行离线测试与四题演示；真实 DeepSeek、Ollama、ChatGPT 端点需在各自配置可用时另外验收，离线通过不等于真实模型质量通过。
