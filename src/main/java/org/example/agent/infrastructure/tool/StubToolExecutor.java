package org.example.agent.infrastructure.tool;

import jakarta.enterprise.context.ApplicationScoped;
import org.example.agent.application.tool.ToolExecutor;
import org.example.agent.domain.tool.ToolCall;
import org.example.agent.domain.tool.ToolResult;

/**
 * 占位 Tool 执行器：返回空结果。
 */
@ApplicationScoped
public class StubToolExecutor implements ToolExecutor {

    @Override
    public ToolResult execute(ToolCall toolCall) {
        return new ToolResult(toolCall.callId(), "");
    }
}
