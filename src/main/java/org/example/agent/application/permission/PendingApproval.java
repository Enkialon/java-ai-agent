package org.example.agent.application.permission;

import org.example.agent.domain.tool.ToolCall;

import java.util.Objects;

/**
 * 一次待审批的工具调用快照。
 */
public record PendingApproval(
        String sessionId,
        String callId,
        String toolName,
        String arguments,
        String permission
) {

    public PendingApproval {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(callId, "callId must not be null");
        Objects.requireNonNull(toolName, "toolName must not be null");
        Objects.requireNonNull(arguments, "arguments must not be null");
        Objects.requireNonNull(permission, "permission must not be null");
    }

    public static PendingApproval from(String sessionId, ToolCall toolCall, String permission) {
        return new PendingApproval(
                sessionId,
                toolCall.callId(),
                toolCall.toolName(),
                toolCall.arguments(),
                permission);
    }
}
