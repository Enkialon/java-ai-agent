package org.example.agent.application.tool;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.example.agent.application.event.AgentEvent.ToolExecutionEndEvent;
import org.example.agent.application.event.AgentEvent.ToolExecutionStartEvent;
import org.example.agent.application.event.AgentEventSink;
import org.example.agent.application.hook.ToolHookService;
import org.example.agent.application.permission.ToolPermissionService;
import org.example.agent.application.runtime.AgentRunContext;
import org.example.agent.domain.session.message.AgentMessage;
import org.example.agent.domain.tool.Tool;
import org.example.agent.domain.tool.ToolCall;
import org.example.agent.domain.tool.ToolResult;

import java.util.Objects;
import java.util.Optional;

/**
 * 执行单个 Tool：权限裁决、hooks、事件、写入 ToolResult。
 * <p>
 * 从 {@link AgentRunContext} 的 Runtime 按名查找可执行 {@link Tool}；
 * 调用方应已将对应 {@link ToolCall} 写入 Session。
 */
@ApplicationScoped
public class ToolExecutionService {

    private final ToolHookService hooks;
    private final ToolPermissionService permissions;

    @Inject
    public ToolExecutionService(ToolHookService hooks, ToolPermissionService permissions) {
        this.hooks = Objects.requireNonNull(hooks, "hooks must not be null");
        this.permissions = Objects.requireNonNull(permissions, "permissions must not be null");
    }

    /**
     * 测试用：跳过权限闸门（全部放行）。
     */
    public ToolExecutionService(ToolHookService hooks) {
        this.hooks = Objects.requireNonNull(hooks, "hooks must not be null");
        this.permissions = null;
    }

    public void execute(
            AgentRunContext context,
            ToolCall toolCall,
            AgentEventSink sink) {
        Tool tool = context.runtime().findTool(toolCall.toolName())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown tool: " + toolCall.toolName()));

        if (permissions != null) {
            Optional<ToolResult> denied = permissions.authorize(context, toolCall, sink);
            if (denied.isPresent()) {
                finish(context, denied.get(), sink);
                return;
            }
        }

        hooks.beforeTool(context, toolCall);
        sink.emit(new ToolExecutionStartEvent(
                toolCall.callId(),
                toolCall.toolName(),
                toolCall.arguments()));

        ToolResult result = tool.execute(toolCall, context.runtime().workspace());

        hooks.afterTool(context, toolCall, result);
        finish(context, result, sink);
    }

    private static void finish(
            AgentRunContext context,
            ToolResult result,
            AgentEventSink sink) {
        String contentForSession = result.success()
                ? result.content()
                : "Error: " + result.content();
        context.session().addMessage(new AgentMessage.ToolResultMessage(
                result.callId(),
                contentForSession));
        sink.emit(new ToolExecutionEndEvent(
                result.callId(),
                result.success(),
                contentForSession));
    }
}
