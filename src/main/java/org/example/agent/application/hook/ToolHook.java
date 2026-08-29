package org.example.agent.application.hook;

import org.example.agent.application.runtime.AgentRunContext;
import org.example.agent.domain.tool.ToolCall;
import org.example.agent.domain.tool.ToolResult;

/**
 * Tool 执行扩展点。
 */
public interface ToolHook {

    /**
     * 执行 Tool 前；可用于校验、审批拦截等。
     */
    default void beforeTool(AgentRunContext context, ToolCall toolCall) {}

    /**
     * Tool 执行完成后。
     */
    default void afterTool(
            AgentRunContext context,
            ToolCall toolCall,
            ToolResult result) {}
}
