package org.example.agent.api;

import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.example.agent.application.AgentService;
import org.example.agent.application.event.AgentEvent;
import org.example.agent.application.session.AgentSessionManager;
import org.example.agent.domain.session.AgentSession;
import org.jboss.resteasy.reactive.RestStreamElementType;

/**
 * HTTP/SSE 适配层：Reactive 止步于此，Agent 核心跑在虚拟线程上。
 * <p>
 * Session 必须在请求线程解析：虚拟线程上 RequestScoped 不可用。
 */
@Path("/api/agent/chat")
@Consumes(MediaType.APPLICATION_JSON)
public class AgentChatResource {

    @Inject
    AgentService agentService;

    @Inject
    AgentSessionManager sessionManager;

    @POST
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    public Multi<AgentEvent> chat(ChatRequest request) {
        AgentSession session = sessionManager.getOrCreate();
        return Multi.createFrom().emitter(emitter ->
                Thread.startVirtualThread(() -> {
                    try {
                        agentService.chat(session, request.message(), emitter::emit);
                        emitter.complete();
                    } catch (Throwable error) {
                        emitter.fail(error);
                    }
                }));
    }

    public record ChatRequest(String message) {
    }
}
