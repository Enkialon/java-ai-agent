package org.example.agent.domain.tool;

import java.util.Objects;

/**
 * 一次 Tool 执行结果（运行态）。
 * <p>
 * {@link #status()} 区分成功/失败，勿依赖 content 文本前缀判断。
 */
public record ToolResult(
        String callId,
        ToolResultStatus status,
        String content
) {

    public ToolResult {
        Objects.requireNonNull(callId, "callId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(content, "content must not be null");
    }

    public boolean success() {
        return status == ToolResultStatus.SUCCESS;
    }

    public static ToolResult ok(String callId, String content) {
        return new ToolResult(callId, ToolResultStatus.SUCCESS, content);
    }

    public static ToolResult error(String callId, String content) {
        return new ToolResult(callId, ToolResultStatus.ERROR, content);
    }
}
