package com.example.codingagent;

import com.example.codingagent.workspace.WorkspaceBoundary;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkspaceBoundaryTest {

    @Test
    void shouldRejectPathEscapingWorkspace() {
        WorkspaceBoundary boundary = new WorkspaceBoundary(Path.of("target/workspace-boundary").toAbsolutePath());

        assertThrows(SecurityException.class, () -> boundary.resolve("../secret.txt"));
    }

    @Test
    void shouldResolveRelativePathInsideWorkspace() {
        Path root = Path.of("target/workspace-boundary").toAbsolutePath().normalize();
        WorkspaceBoundary boundary = new WorkspaceBoundary(root);

        assertTrue(boundary.resolve("src/main/java/App.java").startsWith(root));
    }

    @org.junit.jupiter.api.io.TempDir Path temp;

    @Test void rejectsAbsolutePathAndFileSymlink() throws Exception {
        Path root = java.nio.file.Files.createDirectory(temp.resolve("workspace"));
        Path outside = java.nio.file.Files.writeString(temp.resolve("outside.java"), "secret");
        java.nio.file.Files.createSymbolicLink(root.resolve("link.java"), outside);
        WorkspaceBoundary boundary = new WorkspaceBoundary(root);
        assertThrows(SecurityException.class, () -> boundary.resolve(outside.toString()));
        assertThrows(SecurityException.class, () -> boundary.resolve("link.java"));
        assertThrows(SecurityException.class, () -> com.example.codingagent.workspace.WorkspaceSnapshot.capture(root));
    }

    @Test void rejectsDirectoryAndRootSymlinks() throws Exception {
        Path outside = java.nio.file.Files.createDirectory(temp.resolve("outside"));
        Path root = java.nio.file.Files.createDirectory(temp.resolve("workspace"));
        java.nio.file.Files.createSymbolicLink(root.resolve("linked-dir"), outside);
        assertThrows(SecurityException.class, () -> new WorkspaceBoundary(root).resolve("linked-dir/new.java"));
        Path linkedRoot = java.nio.file.Files.createSymbolicLink(temp.resolve("linked-root"), root);
        assertThrows(SecurityException.class, () -> new WorkspaceBoundary(linkedRoot).resolve("new.java"));
    }
}
