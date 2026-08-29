package org.example.agent.domain.tool;

import java.util.Objects;

/**
 * 一次 Tool 调用（运行态）。
 */
public record ToolCall(
        String callId,
        String toolName,
        String arguments
) {

    public ToolCall {
        Objects.requireNonNull(callId, "callId must not be null");
        Objects.requireNonNull(toolName, "toolName must not be null");
        Objects.requireNonNull(arguments, "arguments must not be null");
    }
}
