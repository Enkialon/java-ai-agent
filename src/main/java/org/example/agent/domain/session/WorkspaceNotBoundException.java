package org.example.agent.domain.session;

/**
 * 当前 Session 尚未绑定工作区。
 */
public class WorkspaceNotBoundException extends RuntimeException {

    public WorkspaceNotBoundException(String sessionId) {
        super("workspace not bound for session '" + sessionId
                + "'; call PUT /api/agent/session/workspace first");
    }
}
