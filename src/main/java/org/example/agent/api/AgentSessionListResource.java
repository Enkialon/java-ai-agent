package org.example.agent.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.example.agent.application.session.AgentSessionManager;
import org.example.agent.application.session.SessionSummary;

import java.util.List;

/**
 * 返回当前用户的内存会话列表，供 Web UI 侧栏展示。
 */
@Path("/api/agent/sessions")
@Produces(MediaType.APPLICATION_JSON)
public class AgentSessionListResource {

    @Inject
    AgentSessionManager sessionManager;

    @GET
    public SessionListResponse list() {
        return new SessionListResponse(sessionManager.listForCurrentUser());
    }

    public record SessionListResponse(List<SessionSummary> sessions) {
    }
}
