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
 * {@link ToolHook} 组合入口。
 */
@ApplicationScoped
@Typed(ToolHookService.class)
public class ToolHookService implements ToolHook {

    private final List<ToolHook> hooks;

    @Inject
    public ToolHookService(Instance<ToolHook> hooks) {
        this(hooks.stream().toList());
    }

    public ToolHookService(List<ToolHook> hooks) {
        this.hooks = List.copyOf(Objects.requireNonNull(hooks, "hooks must not be null"));
    }

    @Override
    public void beforeTool(AgentRunContext context, ToolCall toolCall) {
        for (ToolHook hook : hooks) {
            hook.beforeTool(context, toolCall);
        }
    }

    @Override
    public void afterTool(AgentRunContext context, ToolCall toolCall, ToolResult result) {
        for (ToolHook hook : hooks) {
            hook.afterTool(context, toolCall, result);
        }
    }
}
