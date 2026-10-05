package com.example.chapter09.memory;

import com.example.chapter09.ModelSettings;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public final class AgentMemoryDemo {

    private static final int MEMORY_INDEX_MAX_LINES = 200;
    private static final int MEMORY_INDEX_MAX_BYTES = 25 * 1024;

    private final ModelSettings settings;
    private final Path workspace;

    public AgentMemoryDemo(ModelSettings settings) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.workspace = Path.of("target", "chapter09-memory-demo", "workspace")
                .toAbsolutePath()
                .normalize();
    }

    public void run() {
        System.out.println("[3] Agent memory");
        System.out.println("Model profile: " + settings.summary());

        MemoryStore store = new MemoryStore(workspace);
        store.bootstrap();

        String userMessage = "继续检查订单接口，记得我们约定过所有外部 API 都要写 requestId";
        MemoryContext context = store.recall(userMessage);

        System.out.println("Workspace: " + workspace);
        System.out.println("Loaded memory index:");
        System.out.println(indent(context.indexPreview()));
        System.out.println("Recalled topic memories:");
        for (MemoryHit hit : context.hits()) {
            System.out.println("  - " + hit.fileName() + " score=" + hit.score());
            System.out.println(indent(hit.preview()));
        }
        System.out.println("Prompt context:");
        System.out.println(indent(context.toPromptContext()));
    }

    private String indent(String text) {
        return text.lines()
                .map(line -> "    " + line)
                .reduce((left, right) -> left + System.lineSeparator() + right)
                .orElse("    <empty>");
    }

    static final class MemoryStore {
        private final Path workspace;
        private final Path memoryRoot;
        private final Path indexFile;

        MemoryStore(Path workspace) {
            this.workspace = workspace;
            this.memoryRoot = workspace.resolve("memory");
            this.indexFile = workspace.resolve("MEMORY.md");
        }

        void bootstrap() {
            try {
                Files.createDirectories(memoryRoot);
                writeIndex();
                writeTopicMemory("api-conventions.md",
                        """
                        ---
                        name: API conventions
                        description: 订单、支付、库存接口的协作约定
                        type: project
                        ---

                        # API conventions

                        外部 API 调用必须带 requestId，日志也要打印同一个 requestId，方便把网关、业务服务、第三方回调串起来。

                        Why: 之前排查订单接口超时时，只有订单号没有 requestId，跨系统日志很难对齐。

                        How to apply: 新增 controller、client、job handler 时，先检查 requestId 是否从入口透传到外部调用。
                        """);
                writeTopicMemory("user-preference.md",
                        """
                        ---
                        name: User preference
                        description: 代码说明和文档写法偏好
                        type: user
                        ---

                        # User preference

                        写文档时先贴关键代码片段，再解释代码要解决的业务问题和技术边界。

                        Why: 读文档时不一定同时打开 IDE，只有抽象说明会断上下文。

                        How to apply: 讲 Java demo 时保留类名、方法名、关键注释和运行命令。
                        """);
                writeDailyNote();
            } catch (IOException e) {
                throw new IllegalStateException("Failed to initialize memory workspace: " + workspace, e);
            }
        }

        MemoryContext recall(String userMessage) {
            String indexPreview = readMemoryIndex();
            List<MemoryHit> hits = searchTopicFiles(userMessage);
            return new MemoryContext(userMessage, indexPreview, hits);
        }

        private void writeIndex() throws IOException {
            // MEMORY.md 是给启动阶段加载的轻量索引，不应该把所有细节都塞进去。
            // Claude Code 和 OpenClaw 的文档都强调了这点：索引负责指路，主题文件负责承载细节。
            Files.writeString(indexFile,
                    """
                    # MEMORY.md

                    - [API conventions](memory/api-conventions.md) — 外部 API 调用和日志必须透传 requestId。
                    - [User preference](memory/user-preference.md) — 文档里先贴 Java 代码片段，再解释业务和技术边界。
                    """,
                    StandardCharsets.UTF_8);
        }

        private void writeTopicMemory(String fileName, String content) throws IOException {
            Files.writeString(memoryRoot.resolve(fileName), content, StandardCharsets.UTF_8);
        }

        private void writeDailyNote() throws IOException {
            // Daily note 更像 OpenClaw 的运行日志：保留今天的工作上下文，未来可以蒸馏成长期记忆。
            Path daily = memoryRoot.resolve(LocalDate.now() + ".md");
            Files.writeString(daily,
                    """
                    # Daily note

                    - 今天在第 9 章补 Claude Code / OpenClaw 的 memory 子章节。
                    - 记忆设计要区分长期事实、用户偏好、项目约定和当前 session trace。
                    """,
                    StandardCharsets.UTF_8);
        }

        private String readMemoryIndex() {
            try {
                byte[] bytes = Files.readAllBytes(indexFile);
                int byteLimit = Math.min(bytes.length, MEMORY_INDEX_MAX_BYTES);
                String limitedByBytes = new String(bytes, 0, byteLimit, StandardCharsets.UTF_8);
                return limitedByBytes.lines()
                        .limit(MEMORY_INDEX_MAX_LINES)
                        .reduce((left, right) -> left + System.lineSeparator() + right)
                        .orElse("");
            } catch (IOException e) {
                throw new IllegalStateException("Failed to read memory index: " + indexFile, e);
            }
        }

        private List<MemoryHit> searchTopicFiles(String query) {
            List<String> terms = tokenize(query);
            try (Stream<Path> files = Files.list(memoryRoot)) {
                return files
                        .filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".md"))
                        .map(path -> score(path, terms))
                        .filter(hit -> hit.score() > 0)
                        .sorted(Comparator.comparingInt(MemoryHit::score).reversed()
                                .thenComparing(MemoryHit::fileName))
                        .limit(3)
                        .toList();
            } catch (IOException e) {
                throw new IllegalStateException("Failed to search memory files: " + memoryRoot, e);
            }
        }

        private MemoryHit score(Path file, List<String> terms) {
            try {
                String content = Files.readString(file, StandardCharsets.UTF_8);
                String normalized = content.toLowerCase(Locale.ROOT);
                int score = 0;
                for (String term : terms) {
                    if (normalized.contains(term)) {
                        score++;
                    }
                }
                return new MemoryHit(file.getFileName().toString(), score, preview(content));
            } catch (IOException e) {
                throw new IllegalStateException("Failed to score memory file: " + file, e);
            }
        }

        private List<String> tokenize(String query) {
            // demo 用可读的关键词规则，避免引入向量库。真实系统可以替换为 BM25、embedding 或 hybrid search。
            Map<String, String> aliases = new LinkedHashMap<>();
            aliases.put("订单", "订单");
            aliases.put("接口", "api");
            aliases.put("api", "api");
            aliases.put("requestid", "requestid");
            aliases.put("文档", "文档");
            aliases.put("代码", "代码");

            String normalized = query.toLowerCase(Locale.ROOT);
            List<String> terms = new ArrayList<>();
            aliases.forEach((needle, term) -> {
                if (normalized.contains(needle)) {
                    terms.add(term);
                }
            });
            return terms.isEmpty() ? List.of(normalized) : terms;
        }

        private String preview(String content) {
            return content.lines()
                    .filter(line -> !line.isBlank())
                    .limit(8)
                    .reduce((left, right) -> left + System.lineSeparator() + right)
                    .orElse("");
        }
    }

    record MemoryContext(String userMessage, String indexPreview, List<MemoryHit> hits) {
        String toPromptContext() {
            StringBuilder builder = new StringBuilder();
            builder.append("User message: ").append(userMessage).append(System.lineSeparator());
            builder.append("Relevant memory:").append(System.lineSeparator());
            if (hits.isEmpty()) {
                builder.append("- <none>").append(System.lineSeparator());
            } else {
                for (MemoryHit hit : hits) {
                    builder.append("- ").append(hit.fileName()).append(": ")
                            .append(hit.preview().replace(System.lineSeparator(), " "))
                            .append(System.lineSeparator());
                }
            }
            builder.append("Instruction: use memory as context, but verify current code before changing behavior.");
            return builder.toString();
        }
    }

    record MemoryHit(String fileName, int score, String preview) {
    }
}
