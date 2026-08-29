package org.example.agent.application.hook;

import org.example.agent.application.runtime.AgentRunContext;
import org.example.agent.domain.tool.ToolCall;
import org.example.agent.domain.tool.ToolResult;

/**
 * Agent 生命周期扩展点。
 * <p>
 * 实现方可覆盖感兴趣的回调；未覆盖的方法默认空实现。
 * 多个 Hook 由 {@link AgentHookService} 按注册顺序统一调度。
 */
public interface AgentHook {

    /**
     * Agent Loop 开始前调用。
     */
    default void beforeAgent(AgentRunContext context) {}

    /**
     * Agent Loop 结束后调用。
     */
    default void afterAgent(AgentRunContext context) {}

    /**
     * 调用模型前调用，可在此向 Runtime Context 注入额外材料。
     */
    default void beforeModel(AgentRunContext context) {}

    /**
     * 模型返回后调用。
     */
    default void afterModel(AgentRunContext context) {}

    /**
     * 执行 Tool 前调用，可用于校验、审批拦截等。
     */
    default void beforeTool(
            AgentRunContext context,
            ToolCall toolCall) {}

    /**
     * Tool 执行完成后调用。
     */
    default void afterTool(
            AgentRunContext context,
            ToolCall toolCall,
            ToolResult result) {}
}
