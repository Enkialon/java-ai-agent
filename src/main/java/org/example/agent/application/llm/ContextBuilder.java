package org.example.agent.application.llm;

import jakarta.enterprise.context.ApplicationScoped;
import org.example.agent.application.runtime.AgentRunContext;
import org.example.agent.application.runtime.RuntimeContext;
import org.example.agent.domain.session.AgentSession;

import java.util.ArrayList;
import java.util.List;

/**
 * 组装本轮发给模型的 {@link ModelRequest}：
 * <pre>
 * Runtime Context + Session History = ModelRequest
 * </pre>
 */
@ApplicationScoped
public class ContextBuilder {

    public ModelRequest build(AgentRunContext runContext) {
        return build(runContext.runtime(), runContext.session());
    }

    public ModelRequest build(RuntimeContext runtime, AgentSession session) {
        List<String> systemSections = new ArrayList<>();
        addIfPresent(systemSections, runtime.systemPrompt());
        addIfPresent(systemSections, runtime.agentsMd());
        systemSections.addAll(runtime.hookInjections());
        addIfPresent(systemSections, runtime.environmentInfo());

        LlmContext context = new LlmContext(
                systemSections,
                runtime.skills(),
                runtime.tools(),
                session.messages());
        return new ModelRequest(context);
    }

    private static void addIfPresent(List<String> sections, String value) {
        if (value != null && !value.isBlank()) {
            sections.add(value);
        }
    }
}
