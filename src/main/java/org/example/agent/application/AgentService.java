package org.example.agent.application;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.example.agent.api.AgentChatResource;
import org.example.agent.application.hook.AgentHook;
import org.example.agent.application.loop.AgentLoopService;
import org.example.agent.application.loop.AgentRunResult;
import org.example.agent.application.runtime.AgentRunContext;
import org.example.agent.application.runtime.RuntimeContextService;
import org.example.agent.application.session.AgentSessionManager;
import org.example.agent.domain.session.AgentSession;
import org.example.agent.domain.session.message.AgentMessage;

@ApplicationScoped
public class AgentService {

    @Inject
    AgentSessionManager agentSessionManager;
    @Inject
    RuntimeContextService runtimeContextService;
    @Inject
    AgentLoopService agentLoopService;
    AgentHook agentHook;

    public AgentChatResource.ChatResponse chat(String message) {
        // 获取消息
        AgentSession agentSession = agentSessionManager.getOrCreate();
        //添加消息
        agentSession.addMessage(new AgentMessage.UserMessage(message));
        //组装上下文
        AgentRunContext runContext =
                runtimeContextService.createRunContext(agentSession);
        // 执行hook
        agentHook.beforeAgent(runContext);
        AgentRunResult result = agentLoopService.run(runContext);
        agentHook.afterAgent(runContext);

        // 保存信息
        agentSessionManager.save(agentSession);
        return new AgentChatResource.ChatResponse(result.message());
    }
}
