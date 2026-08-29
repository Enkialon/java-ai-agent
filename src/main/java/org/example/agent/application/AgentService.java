package org.example.agent.application;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.example.agent.application.event.AgentEventSink;
import org.example.agent.application.hook.AgentLifecycleHookService;
import org.example.agent.application.loop.AgentLoopService;
import org.example.agent.application.runtime.AgentRunContext;
import org.example.agent.application.runtime.RuntimeContextService;
import org.example.agent.application.session.AgentSessionManager;
import org.example.agent.domain.session.AgentSession;
import org.example.agent.domain.session.message.AgentMessage;

/**
 * Agent 应用入口：普通同步编排，通过 {@link AgentEventSink} 推送过程事件。
 */
@ApplicationScoped
public class AgentService {

    @Inject
    AgentSessionManager agentSessionManager;
    @Inject
    RuntimeContextService runtimeContextService;
    @Inject
    AgentLoopService agentLoopService;
    @Inject
    AgentLifecycleHookService agentHook;

    /**
     * 在已解析的 Session 上执行一轮 chat（可在虚拟线程调用）。
     */
    public void chat(AgentSession session, String message, AgentEventSink sink) {
        session.addMessage(new AgentMessage.UserMessage(message));

        AgentRunContext runContext =
                runtimeContextService.createRunContext(session);

        agentHook.beforeAgent(runContext);
        try {
            agentLoopService.run(runContext, sink);
        } finally {
            agentHook.afterAgent(runContext);
            agentSessionManager.save(session);
        }
    }
}
