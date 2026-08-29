package org.example.agent.application.workspace;

import org.example.agent.domain.session.AgentSession;
import org.example.agent.domain.workspace.Workspace;

/**
 * 根据 Session 解析本轮 {@link Workspace}。
 */
public interface WorkspaceResolver {

    Workspace resolve(AgentSession session);
}
