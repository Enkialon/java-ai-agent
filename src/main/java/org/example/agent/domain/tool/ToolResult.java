package org.example.agent.domain.tool;

import java.util.Objects;

/**
 * 一次 Tool 执行结果（运行态）。
 */
public record ToolResult(
        String callId,
        String result
) {

    public ToolResult {
        Objects.requireNonNull(callId, "callId must not be null");
        Objects.requireNonNull(result, "result must not be null");
    }
}
