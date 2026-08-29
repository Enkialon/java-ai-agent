package org.example.agent.infrastructure.tool.builtin;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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
 * 通过 {@link SandboxCommandExecutor} 执行 shell 命令。
 * <p>
 * 本 Tool 只负责参数解析与结果映射；隔离策略由 {@link org.example.agent.infrastructure.sandbox.SandboxRuntime} 决定。
 */
@ApplicationScoped
public class BashTool implements Tool {

    private static final long DEFAULT_TIMEOUT_SECONDS = 30;
    private static final int DEFAULT_MAX_OUTPUT_CHARS = 100_000;

    private static final String INPUT_SCHEMA = """
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

    @Inject
    public BashTool(
            WorkingDirectorySelector workingDirectorySelector,
            SandboxCommandExecutor commandExecutor) {
        this(workingDirectorySelector, commandExecutor, DEFAULT_TIMEOUT_SECONDS, DEFAULT_MAX_OUTPUT_CHARS);
    }

    BashTool(
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
    public String name() {
        return "bash";
    }

    @Override
    public String description() {
        return "Run a bash command via the configured sandbox runtime. "
                + "Optional working_directory must stay inside the workspace. "
                + "Times out after " + timeoutSeconds + "s; output capped at "
                + maxOutputChars + " chars.";
    }

    @Override
    public String inputSchema() {
        return INPUT_SCHEMA;
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
