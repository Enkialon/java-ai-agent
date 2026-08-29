package org.example.agent.application.loop;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.example.agent.application.hook.AgentHookService;
import org.example.agent.application.llm.ContextBuilder;
import org.example.agent.application.llm.LlmContext;
import org.example.agent.application.runtime.AgentRunContext;

/**
 * Agent Loop 入口：在给定 {@link AgentRunContext} 上执行一轮（或多轮）推理与 Tool 调用。
 */
@ApplicationScoped
public class AgentLoopService {

    private final AgentHookService hooks;
    private final ContextBuilder contextBuilder;

    @Inject
    public AgentLoopService(AgentHookService hooks, ContextBuilder contextBuilder) {
        this.hooks = hooks;
        this.contextBuilder = contextBuilder;
    }

    /**
     * 执行 Agent Loop。
     *
     * @param runContext 本轮运行上下文（Session History + Runtime Context）
     * @return 本轮运行结果
     */
    public AgentRunResult run(AgentRunContext runContext) {
        hooks.beforeAgent(runContext);
        try {
            // 后续在此展开：beforeModel → 调模型 → afterModel → Tool 循环
            hooks.beforeModel(runContext);
            LlmContext llmContext = contextBuilder.build(runContext);
            // 执行工具


            hooks.afterModel(runContext);

            return new AgentRunResult("");
        } finally {
            hooks.afterAgent(runContext);
        }
    }
}
