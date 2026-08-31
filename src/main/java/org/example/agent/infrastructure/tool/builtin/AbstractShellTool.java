package org.example.agent.infrastructure.tool.builtin;

import org.example.agent.domain.tool.Tool;
import org.example.agent.domain.tool.ToolCall;
import org.example.agent.domain.tool.ToolResult;
import org.example.agent.domain.workspace.Workspace;
import org.example.agent.infrastructure.sandbox.CommandResult;
import org.example.agent.infrastructure.sandbox.SandboxCommandExecutor;
import org.example.agent.infrastructure.sandbox.WorkingDirectorySelector;
import org.example.agent.infrastructure.tool.ToolArguments;

import java.nio.file.Path;

/**
 * Shell 类工具的共用实现：参数解析、cwd 选择、超时/截断结果映射。
 */
abstract class AbstractShellTool implements Tool {

    static final long DEFAULT_TIMEOUT_SECONDS = 30;
    static final int DEFAULT_MAX_OUTPUT_CHARS = 100_000;

    static final String INPUT_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "command": {
                  "type": "string",
                  "description": "Shell command to run"
                },
                "working_directory": {
                  "type": "string",
                  "description": "Optional working directory relative to the workspace root"
                }
              },
              "required": ["command"]
            }
            """;

    private final WorkingDirectorySelector workingDirectorySelector;
    private final SandboxCommandExecutor commandExecutor;
    private final long timeoutSeconds;
    private final int maxOutputChars;

    AbstractShellTool(
            WorkingDirectorySelector workingDirectorySelector,
            SandboxCommandExecutor commandExecutor) {
        this(workingDirectorySelector, commandExecutor, DEFAULT_TIMEOUT_SECONDS, DEFAULT_MAX_OUTPUT_CHARS);
    }

    AbstractShellTool(
            WorkingDirectorySelector workingDirectorySelector,
            SandboxCommandExecutor commandExecutor,
            long timeoutSeconds,
            int maxOutputChars) {
        this.workingDirectorySelector = workingDirectorySelector;
        this.commandExecutor = commandExecutor;
        this.timeoutSeconds = timeoutSeconds;
        this.maxOutputChars = maxOutputChars;
    }

    @Override
    public String inputSchema() {
        return INPUT_SCHEMA;
    }

    protected long timeoutSeconds() {
        return timeoutSeconds;
    }

    protected int maxOutputChars() {
        return maxOutputChars;
    }

    @Override
    public ToolResult execute(ToolCall call, Workspace workspace) {
        try {
            ToolArguments args = ToolArguments.parse(call.arguments());
            String command = args.requireString("command");
            if (command.isBlank()) {
                return ToolResult.error(call.callId(), "command must not be blank");
            }

            String requestedCwd = args.optionalString("working_directory", null);
            Path cwd = workingDirectorySelector.select(workspace, requestedCwd);

            CommandResult result = commandExecutor.execute(
                    cwd,
                    command,
                    timeoutSeconds,
                    maxOutputChars);

            return toToolResult(call.callId(), result);
        } catch (Exception e) {
            return ToolResult.error(
                    call.callId(),
                    e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }

    private ToolResult toToolResult(String callId, CommandResult result) {
        if (result.timedOut()) {
            String partial = result.output();
            String suffix = partial.isEmpty() ? "" : "\nPartial output:\n" + partial;
            return ToolResult.error(
                    callId,
                    "command timed out after " + timeoutSeconds + "s" + suffix);
        }

        String body = result.output();
        if (body.isEmpty()) {
            body = "(no output)";
        } else if (result.truncated()) {
            body = body + "\n...[truncated]";
        }
        return ToolResult.ok(callId, "exit_code=" + result.exitCode() + "\n" + body);
    }
}
