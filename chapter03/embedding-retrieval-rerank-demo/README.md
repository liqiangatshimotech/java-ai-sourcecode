# Embedding Retrieval Rerank Demo

本模块对应第 03 章 `3.7｜Embedding 召回和重排优化`。

它演示 RAG 里一个更贴近生产的问题：向量召回 Top1 不一定是最终应该交给模型的材料。正确做法通常是先扩大候选池，再用查询扩展、混合召回和重排来提高上下文质量。

## 运行测试

```bash
mvn test
```

## 运行 Demo

```bash
mvn exec:java
```

默认问题：

```text
报销凭证公司名称不对，能不能重新开？
```

输出会展示四组结果：

- 只取向量 Top1
- 扩大向量候选池 Top3
- 对 Top3 候选重排
- 查询扩展 + 混合召回

## 代码入口

| 文件 | 讲解重点 |
|---|---|
| `RetrievalRerankDemo` | 命令行入口，对比不同检索策略 |
| `VectorRetriever` | 一阶段向量召回 |
| `KeywordRetriever` | 关键词召回 |
| `HybridRetriever` | RRF 混合召回 |
| `QueryExpander` | 查询扩展 |
| `BusinessReranker` | 规则版重排器，模拟 Cross-Encoder Reranker |
| `CourseChatSettings` | 保留默认 DeepSeek Chat 配置，同时支持 Ollama / ChatGPT |
