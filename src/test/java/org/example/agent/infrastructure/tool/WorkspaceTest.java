package org.example.agent.infrastructure.tool;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkspaceTest {

    @TempDir
    Path tempDir;

    @Test
    void resolvePath_rejectsEscape() {
        Workspace workspace = new Workspace(tempDir);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> workspace.resolvePath("../outside.txt"));

        assertTrue(ex.getMessage().contains("escapes workspace"));
    }

    @Test
    void resolvePath_normalizesRelativePath() {
        Workspace workspace = new Workspace(tempDir);

        Path resolved = workspace.resolvePath("a/./b/../c.txt");

        assertEquals(tempDir.resolve("a/c.txt").normalize(), resolved);
    }
}
