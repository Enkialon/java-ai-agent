package org.example.agent.application.session;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.example.agent.application.request.AgentRequestContext;
import org.example.agent.domain.session.AgentSession;
import org.example.agent.domain.session.AgentSessionRepository;
import org.example.agent.domain.session.SessionAccessDeniedException;

/**
 * Agent 会话应用服务：从请求上下文获取或创建会话，并校验归属用户。
 */
@ApplicationScoped
public class AgentSessionManager {

    private final AgentSessionRepository repository;
    private final AgentRequestContext context;

    @Inject
    public AgentSessionManager(
            AgentSessionRepository repository,
            AgentRequestContext context) {
        this.repository = repository;
        this.context = context;
    }

    public AgentSession getOrCreate() {
        String sessionId = context.sessionId();
        String userId = context.userId();

        return repository.findById(sessionId)
                .map(session -> {
                    assertOwnedBy(session, userId);
                    return session;
                })
                .orElseGet(() -> new AgentSession(sessionId, userId));
    }

    public void save(AgentSession session) {
        repository.save(session);
    }

    private static void assertOwnedBy(AgentSession session, String userId) {
        if (!session.userId().equals(userId)) {
            throw new SessionAccessDeniedException(session.sessionId(), userId);
        }
    }
}
