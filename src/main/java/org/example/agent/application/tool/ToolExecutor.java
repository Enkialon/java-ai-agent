package org.example.agent.application.tool;

import org.example.agent.domain.tool.ToolCall;
import org.example.agent.domain.tool.ToolResult;

/**
 * Tool 执行器（可阻塞等待 I/O）。
 */
public interface ToolExecutor {

    ToolResult execute(ToolCall toolCall);
}
