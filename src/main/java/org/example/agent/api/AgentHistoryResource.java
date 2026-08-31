package org.example.agent.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.example.agent.application.session.AgentSessionManager;
import org.example.agent.domain.session.AgentSession;
import org.example.agent.domain.session.message.AgentMessage;

import java.util.List;

/**
 * 返回当前 Session 的内存历史，供 Web UI 刷新或切换会话后重建时间线。
 */
@Path("/api/agent/session/messages")
@Produces(MediaType.APPLICATION_JSON)
public class AgentHistoryResource {

    @Inject
    AgentSessionManager sessionManager;

    @GET
    public HistoryResponse history() {
        AgentSession session = sessionManager.getOrCreate();
        return new HistoryResponse(
                session.sessionId(),
                session.version(),
                session.messages());
    }

    public record HistoryResponse(
            String sessionId,
            long version,
            List<AgentMessage> messages
    ) {
    }
}
