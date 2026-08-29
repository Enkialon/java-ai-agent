package org.example.agent.infrastructure.session;

import jakarta.enterprise.context.ApplicationScoped;
import org.example.agent.domain.session.AgentSession;
import org.example.agent.domain.session.AgentSessionRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 第一版内存仓储，仅供开发与测试。
 */
@ApplicationScoped
public class InMemoryAgentSessionRepository implements AgentSessionRepository {

    private final Map<String, AgentSession> sessions = new ConcurrentHashMap<>();

    @Override
    public Optional<AgentSession> findById(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    @Override
    public void save(AgentSession session) {
        sessions.put(session.sessionId(), session);
    }

    @Override
    public void delete(String sessionId) {
        sessions.remove(sessionId);
    }
}
