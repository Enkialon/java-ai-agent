package org.example.agent.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.example.agent.application.AgentService;

@Path("/api/agent/chat")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AgentChatResource {

    @Inject
    AgentService agentService;

    @POST
    public ChatResponse chat(ChatRequest request) {
        return agentService.chat(request.message());
    }

    public record ChatRequest(
            String message
    ) {
    }

    public record ChatResponse(
            String message
    ) {
    }
}