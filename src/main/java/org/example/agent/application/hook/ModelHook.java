package org.example.agent.application.hook;

import org.example.agent.application.runtime.AgentRunContext;

/**
 * 模型调用扩展点。
 */
public interface ModelHook {

    /**
     * 调用模型前；可向 Runtime Context 注入材料。
     */
    default void beforeModel(AgentRunContext context) {}

    /**
     * 本轮模型响应结束后。
     */
    default void afterModel(AgentRunContext context) {}
}
