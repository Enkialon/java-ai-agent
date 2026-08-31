package org.example.agent.infrastructure.session;

import jakarta.enterprise.context.ApplicationScoped;
import org.example.agent.domain.session.AgentSession;
import org.example.agent.domain.session.AgentSessionRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
    public List<AgentSession> findByUserId(String userId) {
        List<AgentSession> matched = new ArrayList<>();
        for (AgentSession session : sessions.values()) {
            if (session.userId().equals(userId)) {
                matched.add(session);
            }
        }
        matched.sort(Comparator.comparingLong(AgentSession::createdAt).reversed());
        return List.copyOf(matched);
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
