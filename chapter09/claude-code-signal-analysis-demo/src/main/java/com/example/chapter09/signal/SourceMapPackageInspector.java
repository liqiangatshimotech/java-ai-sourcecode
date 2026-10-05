package com.example.chapter09.signal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SourceMapPackageInspector {

    private static final Pattern VERSION_PATTERN = Pattern.compile("\"version\"\\s*:\\s*\"([^\"]+)\"");

    public Optional<PackageFinding> inspectIfPresent() {
        return locateRoot().flatMap(this::inspect);
    }

    private Optional<Path> locateRoot() {
        String configured = System.getenv("CLAUDE_CODE_SOURCEMAP_ROOT");
        if (configured != null && !configured.isBlank()) {
            return Optional.of(Path.of(configured));
        }

        // 兼容从课程根目录或 java-ai-course-examples 目录运行 demo 的两种方式。
        List<Path> candidates = List.of(
                Path.of("claude-code-sourcemap"),
                Path.of("../claude-code-sourcemap"),
                Path.of("../../claude-code-sourcemap")
        );
        for (Path candidate : candidates) {
            if (Files.exists(candidate.resolve("package/cli.js.map"))) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private Optional<PackageFinding> inspect(Path root) {
        Path packageJson = root.resolve("package/package.json");
        Path cli = root.resolve("package/cli.js");
        Path sourceMap = root.resolve("package/cli.js.map");
        if (!Files.exists(packageJson) || !Files.exists(cli) || !Files.exists(sourceMap)) {
            return Optional.empty();
        }

        try {
            String manifest = Files.readString(packageJson);
            Matcher matcher = VERSION_PATTERN.matcher(manifest);
            String version = matcher.find() ? matcher.group(1) : "unknown";
            return Optional.of(new PackageFinding(
                    root.normalize().toString(),
                    version,
                    Files.size(cli),
                    Files.size(sourceMap),
                    sourceMapContainsSourcesContent(sourceMap)
            ));
        } catch (IOException ex) {
            return Optional.empty();
        }
    }

    private boolean sourceMapContainsSourcesContent(Path sourceMap) throws IOException {
        // 不把 57MB source map 整体读进内存，只读文件开头已经足够确认字段存在。
        try (var input = Files.newInputStream(sourceMap)) {
            byte[] buffer = input.readNBytes(1024 * 1024);
            String head = new String(buffer);
            return head.contains("\"sourcesContent\"");
        }
    }

    public record PackageFinding(
            String root,
            String version,
            long cliBytes,
            long sourceMapBytes,
            boolean hasSourcesContent
    ) {

        public String summary() {
            return "root=" + root
                    + ", version=" + version
                    + ", cli.js=" + formatMb(cliBytes)
                    + ", cli.js.map=" + formatMb(sourceMapBytes)
                    + ", sourcesContent=" + hasSourcesContent;
        }

        private String formatMb(long bytes) {
            return String.format("%.1fMB", bytes / 1024.0 / 1024.0);
        }
    }
}
