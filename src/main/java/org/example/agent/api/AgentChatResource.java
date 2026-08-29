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
import org.jboss.resteasy.reactive.RestStreamElementType;

/**
 * HTTP/SSE 适配层：Reactive 止步于此，Agent 核心跑在虚拟线程上。
 */
@Path("/api/agent/chat")
@Consumes(MediaType.APPLICATION_JSON)
public class AgentChatResource {

    @Inject
    AgentService agentService;

    @POST
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    public Multi<AgentEvent> chat(ChatRequest request) {
        return Multi.createFrom().emitter(emitter ->
                // 通过多线程执行+callback的方式执行, 减少代码复杂性, 不然完整的事件机制. 太复杂. 难以理解
                Thread.startVirtualThread(() -> {
                    try {
                        agentService.chat(request.message(), emitter::emit);
                        emitter.complete();
                    } catch (Throwable error) {
                        emitter.fail(error);
                    }
                }));
    }

    public record ChatRequest(String message) {
    }
}
