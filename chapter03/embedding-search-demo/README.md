# Embedding Search Demo

本模块对应第 03 章 Embedding 补充文章。

它演示一件很具体的事：业务问题经常不会复用知识库里的原词。文档写的是“发票抬头错误”，用户可能问“报销凭证公司名称不对”。关键词检索只看字面，Embedding 检索会把这类表达映射成相近向量，再用相似度找资料。

## 运行测试

```bash
mvn test
```

## 本地运行

默认使用 `local-concept-embedding`，不需要 API Key：

```bash
mvn exec:java
```

指定问题：

```bash
mvn exec:java -Dexec.args="退费的钱什么时候到账？"
```

## 使用 OpenAI Embedding

```bash
export EMBEDDING_PROVIDER=openai
export OPENAI_API_KEY=sk-...
export OPENAI_EMBEDDING_MODEL=text-embedding-3-small
mvn exec:java
```

## 使用 Ollama 开源 Embedding 模型

```bash
ollama pull nomic-embed-text
export EMBEDDING_PROVIDER=ollama
export OLLAMA_BASE_URL=http://localhost:11434
export OLLAMA_EMBEDDING_MODEL=nomic-embed-text
mvn exec:java
```

## 代码入口

| 文件 | 讲解重点 |
|---|---|
| `EmbeddingSearchDemo` | 命令行入口，对比关键词检索和语义检索 |
| `KeywordSearchEngine` | 朴素关键词检索，对照组 |
| `EmbeddingSearchEngine` | 文档向量化、问题向量化、余弦相似度排序 |
| `LocalConceptEmbeddingModel` | 无外部依赖的概念 Embedding 模型 |
| `CourseEmbeddingModels` | 切换 local / OpenAI / Ollama Embedding provider |
| `CourseChatSettings` | 保留课程默认 DeepSeek Chat 配置，同时支持 Ollama / ChatGPT |
