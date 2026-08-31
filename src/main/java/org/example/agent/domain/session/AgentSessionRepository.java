package org.example.agent.domain.session;

import java.util.List;
import java.util.Optional;

/**
 * Agent 会话仓储接口。
 * <p>
 * Domain 层不关心具体存储实现（内存 / Redis / DB 等）。
 */
public interface AgentSessionRepository {

    Optional<AgentSession> findById(String sessionId);

    List<AgentSession> findByUserId(String userId);

    void save(AgentSession session);

    void delete(String sessionId);
}
