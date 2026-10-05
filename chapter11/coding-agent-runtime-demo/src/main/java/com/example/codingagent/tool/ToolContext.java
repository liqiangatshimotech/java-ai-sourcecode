package com.example.codingagent.tool;

import com.example.codingagent.workspace.WorkspaceBoundary;
import com.example.codingagent.workspace.WorkspaceSnapshot;

import java.nio.file.Path;

public record ToolContext(
        Path workspaceRoot,
        WorkspaceBoundary boundary,
        WorkspaceSnapshot baseline
) {
}
