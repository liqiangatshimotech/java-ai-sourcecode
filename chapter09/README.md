# Chapter 09 Coding Agent Teardown Examples

This chapter contains Java examples for:

- `dsh-ideas-java-demo`: models the core ideas of DeepSeek Harness (dsh) in Java, including a plugin tree with reversible effects (Cordis style ctx / effect / disposer), a typed event bus with emit / waterfall / serial / parallel dispatch, an append-only session event log with `deriveMessages()` projection, compaction and fork, a tool execution pipeline (pre-execute approval, monotonic guards, execute wrappers, post-execute rewrite), and capability seams that swap `fs-local` for `fs-sandbox` without touching consumer tools.

Default model profile follows the rest of the course:

- `APP_AI_DEFAULT_PROVIDER=deepseek`
- `DEEPSEEK_MODEL`, `DEEPSEEK_BASE_URL`
- `OLLAMA_MODEL`, `OLLAMA_BASE_URL`
- `CHATGPT_MODEL`, `CHATGPT_BASE_URL`

Run the dsh ideas demo from the chapter module:

```bash
mvn -q -f chapter09/dsh-ideas-java-demo/pom.xml compile exec:java
```

The demo runs fully offline with an `llm-replay` provider, matching the approach the real dsh project uses for runtime testing.
