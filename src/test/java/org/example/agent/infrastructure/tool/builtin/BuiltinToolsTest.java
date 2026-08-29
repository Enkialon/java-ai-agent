package org.example.agent.infrastructure.tool.builtin;

import org.example.agent.domain.tool.ToolCall;
import org.example.agent.domain.tool.ToolResult;
import org.example.agent.domain.workspace.Workspace;
import org.example.agent.infrastructure.sandbox.LocalSandboxRuntime;
import org.example.agent.infrastructure.sandbox.ProcessRunner;
import org.example.agent.infrastructure.sandbox.SandboxCommandExecutor;
import org.example.agent.infrastructure.sandbox.WorkspaceWorkingDirectorySelector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuiltinToolsTest {

    @TempDir
    Path tempDir;

    private Workspace workspace;
    private ReadFileTool readTool;
    private WriteFileTool writeTool;
    private EditFileTool editTool;
    private BashTool bashTool;

    @BeforeEach
    void setUp() {
        workspace = new Workspace(tempDir);
        readTool = new ReadFileTool();
        writeTool = new WriteFileTool();
        editTool = new EditFileTool();
        bashTool = newBashTool(30, 100_000);
    }

    @Test
    void write_then_read_roundTrip() {
        ToolResult writeResult = writeTool.execute(call(
                "write",
                "{\"path\":\"notes/hello.txt\",\"content\":\"你好\"}"), workspace);
        assertTrue(writeResult.success());
        assertTrue(writeResult.content().startsWith("created "));

        ToolResult readResult = readTool.execute(call(
                "read",
                "{\"path\":\"notes/hello.txt\"}"), workspace);
        assertTrue(readResult.success());
        assertEquals("你好", readResult.content());
    }

    @Test
    void edit_replacesUniqueOccurrence() throws Exception {
        Path file = tempDir.resolve("app.txt");
        Files.writeString(file, "alpha\nbeta\nalpha-tail\n");

        ToolResult result = editTool.execute(call(
                "edit",
                "{\"path\":\"app.txt\",\"old_string\":\"beta\",\"new_string\":\"BETA\"}"), workspace);

        assertTrue(result.success());
        assertEquals("edited app.txt", result.content());
        assertEquals("alpha\nBETA\nalpha-tail\n", Files.readString(file));
    }

    @Test
    void edit_failsWhenOldStringNotUnique() throws Exception {
        Files.writeString(tempDir.resolve("dup.txt"), "xx\nxx\n");

        ToolResult result = editTool.execute(call(
                "edit",
                "{\"path\":\"dup.txt\",\"old_string\":\"xx\",\"new_string\":\"yy\"}"), workspace);

        assertFalse(result.success());
        assertTrue(result.content().contains("not unique"));
    }

    @Test
    void read_missingFile_returnsError() {
        ToolResult result = readTool.execute(call("read", "{\"path\":\"missing.txt\"}"), workspace);

        assertFalse(result.success());
        assertTrue(result.content().contains("not found"));
    }

    @Test
    void bash_runsInWorkspace() throws Exception {
        ToolResult result = bashTool.execute(call(
                "bash",
                "{\"command\":\"pwd && echo hi\"}"), workspace);

        assertTrue(result.success());
        assertTrue(result.content().startsWith("exit_code=0"));
        assertTrue(result.content().contains(tempDir.toRealPath().toString())
                || result.content().contains(tempDir.toAbsolutePath().normalize().toString()));
        assertTrue(result.content().contains("hi"));
    }

    @Test
    void bash_usesWorkingDirectory() throws Exception {
        Path sub = tempDir.resolve("sub");
        Files.createDirectories(sub);

        ToolResult result = bashTool.execute(call(
                "bash",
                "{\"command\":\"pwd\",\"working_directory\":\"sub\"}"), workspace);

        assertTrue(result.success());
        assertTrue(result.content().contains(sub.toRealPath().toString())
                || result.content().contains(sub.toAbsolutePath().normalize().toString()));
    }

    @Test
    void bash_timeout_killsLongRunningCommand() {
        BashTool shortTimeout = newBashTool(1, 100_000);

        long started = System.nanoTime();
        ToolResult result = shortTimeout.execute(call(
                "bash",
                "{\"command\":\"sleep 30\"}"), workspace);
        long elapsedMs = (System.nanoTime() - started) / 1_000_000;

        assertFalse(result.success());
        assertTrue(result.content().contains("timed out"));
        assertTrue(elapsedMs < 10_000, "timeout should fire well before sleep finishes, was " + elapsedMs + "ms");
    }

    @Test
    void bash_truncatesLargeOutputWithoutLoadingAll() {
        BashTool capped = newBashTool(30, 1_000);

        ToolResult result = capped.execute(call(
                "bash",
                "{\"command\":\"python3 -c \\\"print('x'*200000)\\\"\"}"), workspace);

        assertTrue(result.success());
        assertTrue(result.content().contains("truncated"));
        assertTrue(result.content().length() < 5_000);
    }

    @Test
    void toolNames_matchContract() {
        assertEquals("read", readTool.name());
        assertEquals("write", writeTool.name());
        assertEquals("edit", editTool.name());
        assertEquals("bash", bashTool.name());
    }

    private BashTool newBashTool(long timeoutSeconds, int maxOutputChars) {
        return new BashTool(
                new WorkspaceWorkingDirectorySelector(),
                new SandboxCommandExecutor(new ProcessRunner(), new LocalSandboxRuntime()),
                timeoutSeconds,
                maxOutputChars);
    }

    private static ToolCall call(String name, String arguments) {
        return new ToolCall("call_1", name, arguments);
    }
}
