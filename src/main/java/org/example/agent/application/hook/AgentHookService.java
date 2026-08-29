package org.example.agent.application.hook;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Typed;
import jakarta.inject.Inject;
import org.example.agent.application.runtime.AgentRunContext;
import org.example.agent.domain.tool.ToolCall;
import org.example.agent.domain.tool.ToolResult;

import java.util.List;
import java.util.Objects;

/**
 * AgentHook 组合入口：对外是一个 Hook，对内按顺序通知所有 Hook。
 */
@ApplicationScoped
@Typed(AgentHookService.class)
public class AgentHookService implements AgentHook {

    private final List<AgentHook> hooks;

    @Inject
    public AgentHookService(Instance<AgentHook> hooks) {
        this(hooks.stream().toList());
    }

    public AgentHookService(List<AgentHook> hooks) {
        this.hooks = List.copyOf(Objects.requireNonNull(hooks, "hooks must not be null"));
    }

    @Override
    public void beforeAgent(AgentRunContext context) {
        for (AgentHook hook : hooks) {
            hook.beforeAgent(context);
        }
    }

    @Override
    public void afterAgent(AgentRunContext context) {
        for (AgentHook hook : hooks) {
            hook.afterAgent(context);
        }
    }

    @Override
    public void beforeModel(AgentRunContext context) {
        for (AgentHook hook : hooks) {
            hook.beforeModel(context);
        }
    }

    @Override
    public void afterModel(AgentRunContext context) {
        for (AgentHook hook : hooks) {
            hook.afterModel(context);
        }
    }

    @Override
    public void beforeTool(AgentRunContext context, ToolCall toolCall) {
        for (AgentHook hook : hooks) {
            hook.beforeTool(context, toolCall);
        }
    }

    @Override
    public void afterTool(AgentRunContext context, ToolCall toolCall, ToolResult result) {
        for (AgentHook hook : hooks) {
            hook.afterTool(context, toolCall, result);
        }
    }
}
