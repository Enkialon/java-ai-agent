package org.example.agent.application.hook;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Typed;
import jakarta.inject.Inject;
import org.example.agent.application.runtime.AgentRunContext;

import java.util.List;
import java.util.Objects;

/**
 * {@link ModelHook} 组合入口。
 */
@ApplicationScoped
@Typed(ModelHookService.class)
public class ModelHookService implements ModelHook {

    private final List<ModelHook> hooks;

    @Inject
    public ModelHookService(Instance<ModelHook> hooks) {
        this(hooks.stream().toList());
    }

    public ModelHookService(List<ModelHook> hooks) {
        this.hooks = List.copyOf(Objects.requireNonNull(hooks, "hooks must not be null"));
    }

    @Override
    public void beforeModel(AgentRunContext context) {
        for (ModelHook hook : hooks) {
            hook.beforeModel(context);
        }
    }

    @Override
    public void afterModel(AgentRunContext context) {
        for (ModelHook hook : hooks) {
            hook.afterModel(context);
        }
    }
}
