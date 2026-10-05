# dsh-ideas-java-demo

DeepSeek Harness (dsh) core ideas modeled in plain Java, no external dependencies:

- **Plugin tree with reversible effects** (Cordis style): every service, prompt fragment, tool and listener is registered through `ctx.effect(...)` with a disposer; unloading a plugin rolls back all of its side effects.
- **Typed event bus**: `emit` / `waterfall` / `serial` / `parallel` dispatch modes, including waterfall short-circuit semantics used by permission policies.
- **Append-only session log**: `user/message`, `assistant/message`, `tool/call`, `tool/result`, `session/compacted` events; `deriveMessages()` projects model-visible history; `compact()` folds old events into a summary; `SessionLog.fork()` creates child sessions for delegation.
- **Tool execution pipeline**: pre-execute policy (allow / deny / ask), approval seam, monotonic guards, execute wrappers (timeout / telemetry), post-execute rewrite; errors normalized to `isError` results.
- **Capability seams**: `fs` seam with `fs-local` and `fs-sandbox` providers; swapping the provider does not change consumer tool code. LLM seam with replay provider plus an OpenAI-compatible adapter for DeepSeek / Ollama / ChatGPT.

Run it from the repository root:

```bash
mvn -q -f java-ai-course-examples/chapter09/dsh-ideas-java-demo/pom.xml compile exec:java
```

The demo runs offline with an `llm-replay` provider (the same trick the real dsh project uses for runtime tests), so no API key is required. The provider profile still follows the course convention:

- `APP_AI_DEFAULT_PROVIDER=deepseek` (default)
- `DEEPSEEK_MODEL`, `DEEPSEEK_BASE_URL`, `DEEPSEEK_API_KEY`
- `OLLAMA_MODEL`, `OLLAMA_BASE_URL`
- `CHATGPT_MODEL`, `CHATGPT_BASE_URL`, `OPENAI_API_KEY`
