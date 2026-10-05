# claude-code-signal-analysis-demo

This demo belongs to Chapter 09. It shows two things in plain Java:

- how a CLI runtime can encode environment-like signals into a normal-looking system prompt line;
- how to inspect Unicode code points so visually similar characters do not hide in plain sight.

Run from `java-ai-course-examples`:

```bash
mvn -q -f chapter09/claude-code-signal-analysis-demo/pom.xml compile exec:java
```

The model profile follows the course default:

- `APP_AI_DEFAULT_PROVIDER=deepseek`
- `DEEPSEEK_MODEL`, `DEEPSEEK_BASE_URL`
- `OLLAMA_MODEL`, `OLLAMA_BASE_URL`
- `CHATGPT_MODEL`, `CHATGPT_BASE_URL`

The demo does not call any real model. The model settings are printed because this is the exact layer where a Coding Agent would normally assemble the system prompt before sending it to DeepSeek, Ollama, or ChatGPT.
