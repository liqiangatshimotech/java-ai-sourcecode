package com.example.codingagent.workspace;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;

/**
 * 用工作区快照生成简化 diff。
 * 这里不追求完整 git diff 格式，而是清楚展示哪些文件变了、关键行怎么变。
 */
public final class DiffService {

    public String diff(WorkspaceSnapshot baseline, Path workspaceRoot) throws IOException {
        WorkspaceSnapshot current = WorkspaceSnapshot.capture(workspaceRoot);
        return diff(baseline, current);
    }

    /** 生成审批预览时只比较内存快照，不提前把模型代码写到工作区。 */
    public String diff(WorkspaceSnapshot baseline, WorkspaceSnapshot current) {
        Set<String> paths = new TreeSet<>();
        paths.addAll(baseline.files().keySet());
        paths.addAll(current.files().keySet());

        StringBuilder builder = new StringBuilder();
        for (String path : paths) {
            String before = baseline.files().get(path);
            String after = current.files().get(path);
            if (java.util.Objects.equals(before, after)) {
                continue;
            }
            builder.append("diff -- ").append(path).append(System.lineSeparator());
            appendChangedLines(builder, before, after);
        }
        if (builder.length() == 0) {
            return "No changes.";
        }
        return builder.toString();
    }

    private void appendChangedLines(StringBuilder builder, String before, String after) {
        if (before == null) {
            builder.append("+ <new file>").append(System.lineSeparator());
            return;
        }
        if (after == null) {
            builder.append("- <deleted file>").append(System.lineSeparator());
            return;
        }

        String[] beforeLines = before.split("\\R", -1);
        String[] afterLines = after.split("\\R", -1);
        int max = Math.max(beforeLines.length, afterLines.length);
        for (int i = 0; i < max; i++) {
            String oldLine = i < beforeLines.length ? beforeLines[i] : "";
            String newLine = i < afterLines.length ? afterLines[i] : "";
            if (!oldLine.equals(newLine)) {
                builder.append("- ").append(oldLine).append(System.lineSeparator());
                builder.append("+ ").append(newLine).append(System.lineSeparator());
            }
        }
    }
}
