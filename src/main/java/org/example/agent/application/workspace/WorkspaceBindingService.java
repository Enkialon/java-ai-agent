package org.example.agent.application.workspace;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.example.agent.application.session.AgentSessionManager;
import org.example.agent.domain.session.AgentSession;

import java.util.Optional;

/**
 * Session 工作区绑定应用服务。
 */
@ApplicationScoped
public class WorkspaceBindingService {

    private final AgentSessionManager sessionManager;
    private final SessionWorkspaceResolver workspaceResolver;

    @Inject
    public WorkspaceBindingService(
            AgentSessionManager sessionManager,
            SessionWorkspaceResolver workspaceResolver) {
        this.sessionManager = sessionManager;
        this.workspaceResolver = workspaceResolver;
    }

    public String bind(String path) {
        AgentSession session = sessionManager.getOrCreate();
        String normalized = workspaceResolver.normalizeBindPath(path);
        session.bindWorkspace(normalized);
        sessionManager.save(session);
        return normalized;
    }

    public Optional<String> current() {
        return sessionManager.getOrCreate().workspacePath();
    }
}
