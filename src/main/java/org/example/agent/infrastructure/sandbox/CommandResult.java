package org.example.agent.infrastructure.sandbox;

/**
 * 一次命令执行的原始结果（与 ToolResult 解耦）。
 */
public record CommandResult(
        boolean timedOut,
        int exitCode,
        String output,
        boolean truncated
) {

    public static CommandResult timedOut(String partialOutput, boolean truncated) {
        return new CommandResult(true, -1, partialOutput == null ? "" : partialOutput, truncated);
    }

    public static CommandResult completed(int exitCode, String output, boolean truncated) {
        return new CommandResult(false, exitCode, output == null ? "" : output, truncated);
    }
}
