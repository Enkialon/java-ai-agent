package org.example.agent.application.tool;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.example.agent.application.event.AgentEvent.ToolExecutionEndEvent;
import org.example.agent.application.event.AgentEvent.ToolExecutionStartEvent;
import org.example.agent.application.event.AgentEventSink;
import org.example.agent.application.hook.ToolHookService;
import org.example.agent.application.runtime.AgentRunContext;
import org.example.agent.domain.session.message.AgentMessage;
import org.example.agent.domain.tool.Tool;
import org.example.agent.domain.tool.ToolCall;
import org.example.agent.domain.tool.ToolResult;

/**
 * 执行单个 Tool：hooks、事件、写入 ToolResult。
 * <p>
 * 从 {@link AgentRunContext} 的 Runtime 按名查找可执行 {@link Tool}；
 * 调用方应已将对应 {@link ToolCall} 写入 Session。
 */
@ApplicationScoped
public class ToolExecutionService {

    private final ToolHookService hooks;

    @Inject
    public ToolExecutionService(ToolHookService hooks) {
        this.hooks = hooks;
    }

    public void execute(
            AgentRunContext context,
            ToolCall toolCall,
            AgentEventSink sink) {
        Tool tool = context.runtime().findTool(toolCall.toolName())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown tool: " + toolCall.toolName()));

        hooks.beforeTool(context, toolCall);
        sink.emit(new ToolExecutionStartEvent(toolCall.callId(), toolCall.toolName()));

        ToolResult result = tool.execute(toolCall);

        hooks.afterTool(context, toolCall, result);
        String contentForSession = result.success()
                ? result.content()
                : "Error: " + result.content();
        context.session().addMessage(new AgentMessage.ToolResultMessage(
                result.callId(),
                contentForSession));
        sink.emit(new ToolExecutionEndEvent(result.callId(), contentForSession));
    }
}
