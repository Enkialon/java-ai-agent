package org.example.agent.application.hook;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Typed;
import jakarta.inject.Inject;
import org.example.agent.application.runtime.AgentRunContext;

import java.util.List;
import java.util.Objects;

/**
 * {@link AgentLifecycleHook} 组合入口。
 */
@ApplicationScoped
@Typed(AgentLifecycleHookService.class)
public class AgentLifecycleHookService implements AgentLifecycleHook {

    private final List<AgentLifecycleHook> hooks;

    @Inject
    public AgentLifecycleHookService(Instance<AgentLifecycleHook> hooks) {
        this(hooks.stream().toList());
    }

    public AgentLifecycleHookService(List<AgentLifecycleHook> hooks) {
        this.hooks = List.copyOf(Objects.requireNonNull(hooks, "hooks must not be null"));
    }

    @Override
    public void beforeAgent(AgentRunContext context) {
        for (AgentLifecycleHook hook : hooks) {
            hook.beforeAgent(context);
        }
    }

    @Override
    public void afterAgent(AgentRunContext context) {
        for (AgentLifecycleHook hook : hooks) {
            hook.afterAgent(context);
        }
    }
}
