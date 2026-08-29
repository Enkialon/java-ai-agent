package org.example.agent.application;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.example.agent.api.AgentChatResource;
import org.example.agent.application.request.AgentRequestContext;
import org.example.agent.application.runtime.AgentRunContext;
import org.example.agent.application.runtime.RuntimeContextService;
import org.example.agent.application.session.AgentSessionManager;
import org.example.agent.domain.session.AgentSession;

@ApplicationScoped
public class AgentService {

    @Inject
    AgentSessionManager agentSessionManager;
    @Inject
    RuntimeContextService runtimeContextService;

    public AgentChatResource.ChatResponse chat(String message) {

        // 获取上下文. 比如说所有skill提示词, agent.md
        AgentSession agentSession = agentSessionManager.getOrCreate();
        AgentRunContext runContext = runtimeContextService.createRunContext(agentSession);





        return null;
    }
}
