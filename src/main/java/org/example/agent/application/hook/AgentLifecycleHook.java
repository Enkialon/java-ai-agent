package org.example.agent.application.hook;

import org.example.agent.application.runtime.AgentRunContext;

/**
 * Agent Run 生命周期扩展点。
 */
public interface AgentLifecycleHook {

    /**
     * Agent Loop 开始前。
     */
    default void beforeAgent(AgentRunContext context) {}

    /**
     * Agent Loop 结束后。
     */
    default void afterAgent(AgentRunContext context) {}
}
