package org.example.agent.application.hook;

import org.example.agent.application.runtime.AgentRunContext;
import org.example.agent.domain.tool.ToolCall;
import org.example.agent.domain.tool.ToolResult;

public interface AgentHook {

    default void beforeAgent(AgentRunContext context) {}

    default void afterAgent(AgentRunContext context) {}

    default void beforeModel(AgentRunContext context) {}

    default void afterModel(AgentRunContext context) {}

    default void beforeTool(
            AgentRunContext context,
            ToolCall toolCall) {}

    default void afterTool(
            AgentRunContext context,
            ToolCall toolCall,
            ToolResult result) {}
}
