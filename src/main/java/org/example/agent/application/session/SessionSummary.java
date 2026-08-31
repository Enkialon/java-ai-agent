package org.example.agent.application.session;

import org.example.agent.domain.session.AgentSession;

/**
 * 会话列表项（供 Web UI 侧栏展示）。
 */
public record SessionSummary(
        String sessionId,
        String title,
        long createdAt,
        long version
) {

    public static SessionSummary from(AgentSession session) {
        return new SessionSummary(
                session.sessionId(),
                SessionTitles.derive(session),
                session.createdAt(),
                session.version());
    }
}
