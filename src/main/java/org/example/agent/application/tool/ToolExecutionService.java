package org.example.agent.application.tool;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.example.agent.application.event.AgentEvent.ToolExecutionEndEvent;
import org.example.agent.application.event.AgentEvent.ToolExecutionStartEvent;
import org.example.agent.application.event.AgentEventSink;
import org.example.agent.application.hook.ToolHookService;
import org.example.agent.application.runtime.AgentRunContext;
import org.example.agent.domain.session.message.AgentMessage;
import org.example.agent.domain.tool.ToolCall;
import org.example.agent.domain.tool.ToolResult;

/**
 * 执行单个 Tool：hooks、事件、写入 ToolResult。
 * <p>
 * 调用方应已将对应 {@link ToolCall} 写入 Session。
 */
@ApplicationScoped
public class ToolExecutionService {

    private final ToolHookService hooks;
    private final ToolExecutor toolExecutor;

    @Inject
    public ToolExecutionService(ToolHookService hooks, ToolExecutor toolExecutor) {
        this.hooks = hooks;
        this.toolExecutor = toolExecutor;
    }

    public void execute(
            AgentRunContext context,
            ToolCall toolCall,
            AgentEventSink sink) {
        hooks.beforeTool(context, toolCall);
        sink.emit(new ToolExecutionStartEvent(toolCall.callId(), toolCall.toolName()));

        ToolResult result = toolExecutor.execute(toolCall);

        hooks.afterTool(context, toolCall, result);
        context.session().addMessage(new AgentMessage.ToolResultMessage(
                result.callId(),
                result.result()));
        sink.emit(new ToolExecutionEndEvent(result.callId(), result.result()));

    }
}
