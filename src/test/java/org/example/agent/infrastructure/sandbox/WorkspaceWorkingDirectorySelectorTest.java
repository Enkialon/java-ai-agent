package org.example.agent.infrastructure.sandbox;

import org.example.agent.domain.workspace.Workspace;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkspaceWorkingDirectorySelectorTest {

    @TempDir
    Path tempDir;

    private final WorkingDirectorySelector selector = new WorkspaceWorkingDirectorySelector();

    @Test
    void select_blank_returnsWorkspaceRoot() {
        Workspace workspace = new Workspace(tempDir);

        assertEquals(tempDir.toAbsolutePath().normalize(), selector.select(workspace, null));
        assertEquals(tempDir.toAbsolutePath().normalize(), selector.select(workspace, " "));
    }

    @Test
    void select_relativeDirectory() throws Exception {
        Path sub = tempDir.resolve("pkg");
        Files.createDirectories(sub);
        Workspace workspace = new Workspace(tempDir);

        assertEquals(sub.toAbsolutePath().normalize(), selector.select(workspace, "pkg"));
    }

    @Test
    void select_rejectsEscape() {
        Workspace workspace = new Workspace(tempDir);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> selector.select(workspace, "../outside"));

        assertTrue(ex.getMessage().contains("escapes workspace"));
    }
}
