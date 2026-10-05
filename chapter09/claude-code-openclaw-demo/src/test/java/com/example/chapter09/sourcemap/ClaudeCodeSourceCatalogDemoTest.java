package com.example.chapter09.sourcemap;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** 单项目包没有外部仓库是正常交付状态，必须跳过且不能打印伪造的索引成功。 */
class ClaudeCodeSourceCatalogDemoTest {
    @TempDir Path temporary;
    private String run(Map<String, String> environment) {
        var output = new ByteArrayOutputStream();
        new ClaudeCodeSourceCatalogDemo().run(environment, new PrintStream(output, true, StandardCharsets.UTF_8));
        return output.toString(StandardCharsets.UTF_8);
    }
    @Test void unconfiguredSourceIsExplicitlySkipped() {
        String output = run(Map.of());
        assertTrue(output.contains("[SKIPPED]"));
        assertFalse(output.contains("[INDEXED]"));
        assertFalse(output.contains("Indexed files:"));
    }
    @Test void missingConfiguredPathDoesNotAbortOtherDemos() {
        String output = assertDoesNotThrow(() -> run(Map.of("CLAUDE_CODE_SOURCEMAP_ROOT", temporary.resolve("missing").toString())));
        assertTrue(output.contains("[SKIPPED]"));
    }
    @Test void emptyDirectoryIsNotAReportedSuccess() {
        String output = run(Map.of("CLAUDE_CODE_SOURCEMAP_ROOT", temporary.toString()));
        assertTrue(output.contains("[NO_SOURCE_FILES]"));
        assertFalse(output.contains("[INDEXED]"));
    }
    @Test void explicitFixtureIsActuallyCounted() throws Exception {
        Path sources = Files.createDirectories(temporary.resolve("restored-src/src/tools"));
        Files.writeString(sources.resolve("Read.ts"), "export const name = 'read';");
        Files.writeString(sources.getParent().resolve("main.tsx"), "export const entry = true;");
        Files.writeString(sources.getParent().resolve("readme.md"), "not source");
        String output = run(Map.of("CLAUDE_CODE_SOURCEMAP_ROOT", temporary.toString()));
        assertTrue(output.contains("[INDEXED]"));
        assertTrue(output.contains("Indexed files: 2"));
        assertTrue(output.contains("main.tsx [ok]"));
        assertTrue(output.contains("commands.ts [missing]"));
    }
}
