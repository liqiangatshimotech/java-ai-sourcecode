# document-extraction-agent

我把票据抽取分成三步：模型提出候选 JSON，Java 校验业务约束与证据，人工核对后再由独立业务系统处理。这个项目没有记账、报销或付款接口。

项目路径：`java-ai-course-examples/chapter10/document-extraction-agent`。要求 JDK 21+、Maven 3.9+，命令在解压后的 `java-ai-course-examples` 目录执行，使用单项目 POM。

## 默认离线演示

```bash
mvn -q -f chapter10/document-extraction-agent/pom.xml test
mvn -q -f chapter10/document-extraction-agent/pom.xml compile exec:java
```

默认启动本机临时 HTTP 夹具，用合成票据文本和预设 JSON 验证 `/v1/chat/completions` 请求、响应与 Java 校验。没有调用外部模型，没有进行真实 OCR，也不会读取或上传本地票据。成功结果 `READY_FOR_HUMAN_REVIEW` 表示技术校验通过、等待人工复核；有问题时为 `NEEDS_HUMAN_REVIEW`，逐项返回 field/code/message。

## 使用 OCR 文本

先由可信 OCR 系统或人工获得文本。项目不包含 OCR 引擎，输入必须是 UTF-8 文件，最多256KB且不超过100000字符。

```bash
mvn -q -f chapter10/document-extraction-agent/pom.xml compile exec:java -Dexec.args="--text chapter10/document-extraction-agent/samples/demo-ocr.txt"
```

未设置 `RUN_MODEL_CALL=true` 时返回 `MODEL_CALL_DISABLED`，文件不会发送。要真实请求，先在本机设置 `DEEPSEEK_API_KEY`，再运行：

```bash
RUN_MODEL_CALL=true AI_PROVIDER=deepseek mvn -q -f chapter10/document-extraction-agent/pom.xml compile exec:java -Dexec.args="--text chapter10/document-extraction-agent/samples/demo-ocr.txt"
```

| provider | 模型变量与默认值 | BASE_URL变量与默认值 | 密钥 |
|---|---|---|---|
| deepseek（默认） | DEEPSEEK_MODEL=deepseek-v4-flash | DEEPSEEK_BASE_URL=https://api.deepseek.com/v1 | DEEPSEEK_API_KEY |
| ollama | OLLAMA_MODEL=qwen2.5:7b | OLLAMA_BASE_URL=http://localhost:11434/v1 | 默认占位ollama，本地无需云端密钥 |
| openai / chatgpt | OPENAI_MODEL=gpt-4.1-mini | OPENAI_BASE_URL=https://api.openai.com/v1 | OPENAI_API_KEY |

三家均走兼容 Chat Completions 协议；DeepSeek 分支显式关闭 thinking，使用 JSON 输出。接口兼容不保证每个模型支持相同参数，服务端拒绝时返回复核状态，不自动切换服务或重试。旧模型名 deepseek-v4-flash / deepseek-v4-pro 已被官方停用；模型目录仍会变化，真实运行前核对账户可用模型。

## 使用图片

只接受单张 JPEG/PNG：不超过5MB，边长32至8192像素，总像素不超过2000万。先验证文件和图像尺寸，再组装 data URI。没有PDF、多页、URL下载和远程文件抓取。

图片发送须同时满足：`RUN_MODEL_CALL=true`、`AI_VISION_ENABLED=true`、非空 `AI_VISION_MODEL`，并选择 OpenAI 或 Ollama。本课程的 DeepSeek 分支仅处理 OCR 文本，不把默认文本模型当成视觉模型；官方已有实验性 deepseek-v4-flash-vision-exp，本项目未接入该实验分支。即使设置了标记，也仍需自己确认所选模型真实支持图像和 JSON 输出。

```bash
RUN_MODEL_CALL=true AI_PROVIDER=openai AI_VISION_ENABLED=true AI_VISION_MODEL=gpt-4.1-mini mvn -q -f chapter10/document-extraction-agent/pom.xml compile exec:java -Dexec.args="--image /path/to/invoice.png"
```

该命令会把指定图片作为 `image_url` 的 Base64 data URI 发送给配置的模型地址。默认不开启；本次验收没有发送真实票据。Ollama 同样要显式指定具备视觉能力的 `AI_VISION_MODEL`，普通文字模型不能因为支持兼容接口就识图。

## Java 校验

- `Invoice` 是严格 DTO，拒绝未知字段、重复 JSON key、非 JSON 解释文字与截断响应。
- 金额为 `BigDecimal`，非负、最多2位小数、不超过10亿元；当前业务仅支持CNY。
- 未税金额+税额=合计；每行数量×单价按分 `HALF_UP` 后须等于行金额，行金额之和须等于未税金额。
- 日期须真实存在，范围为2000年至业务时钟的今天；命令行使用系统时区，测试固定时钟。
- 文本字段的 quote 须逐字存在于输入且包含抽取值；数字按完整词元匹配，数量1不能匹配金额100.00。每个明细的名称、数量、单价、金额分别检查引用。
- 图片证据 page=1，bbox 为原图像素 `[x1,y1,x2,y2]`，须在图片内且面积为正。程序只能验证几何范围，不能证明字段真的位于该区域，因此图片结果始终带 `IMAGE_CONTENT_UNVERIFIED`，要求人工对照原图。

原文引用出现并不等于字段语义正确，合法票据格式也不证明发票真实或未重复报销。人工仍要核对字段对应关系、原始票据真伪、重复单据与业务归属。项目不授予付款权限。

`ExtractionClient` 不跟随重定向，接收过程中限制模型响应1MB，完整请求45秒超时，失败不重试。认证错误、限流、响应截断、无效JSON、证据缺失都进入复核；原始认证令牌不输出到日志。

## 已验证与尚未验证

15项测试覆盖本地HTTP请求契约、Decimal金额、开关防外传、三provider、缺字段/未知字段/重复key、日期、总额与明细计算、负数/精度/币种、伪造引用、缺证据/页码、文件大小/像素、图片开关、data URI、bbox、HTTP异常/截断/响应大小和超时。

图片测试只使用程序生成的纯色合成PNG，返回预设候选JSON，不能作为图像识别效果证据。本轮没有真实OCR、外部模型或真实票据识别验收。上线前须用经过授权的脱敏样本按字段准确率、证据定位、异常召回与人工修改量做独立评测。

参考：[DeepSeek JSON输出](https://api-docs.deepseek.com/guides/json_mode/)、[DeepSeek模型更新](https://api-docs.deepseek.com/updates/)、[Ollama视觉能力](https://docs.ollama.com/capabilities/vision)、[兼容接口](https://docs.ollama.com/api/openai-compatibility)。
