package org.example.agent.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.example.agent.application.session.AgentSessionManager;
import org.example.agent.domain.session.AgentSession;
import org.example.agent.infrastructure.config.AgentConfig;
import org.example.agent.infrastructure.config.AgentConfiguration;

/**
 * Web UI 顶部状态：workspace / model。
 */
@Path("/api/agent/status")
@Produces(MediaType.APPLICATION_JSON)
public class AgentStatusResource {

    @Inject
    AgentSessionManager sessionManager;

    @Inject
    AgentConfiguration configuration;

    @GET
    public StatusResponse status() {
        AgentSession session = sessionManager.getOrCreate();
        AgentConfig.ModelClientSettings client = configuration.get().model().activeClient();
        String model = client.modelOptional()
                .orElseGet(() -> configuration.get().model().active());
        return new StatusResponse(
                session.sessionId(),
                session.workspacePath().orElse(null),
                configuration.get().model().active(),
                model);
    }

    public record StatusResponse(
            String sessionId,
            String workspace,
            String modelClient,
            String model
    ) {
    }
}
