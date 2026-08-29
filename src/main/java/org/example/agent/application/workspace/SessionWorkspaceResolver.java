package org.example.agent.application.workspace;

import jakarta.enterprise.context.ApplicationScoped;
import org.example.agent.domain.session.AgentSession;
import org.example.agent.domain.session.WorkspaceNotBoundException;
import org.example.agent.domain.workspace.Workspace;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 用 Session 已绑定的本机路径构造 {@link Workspace}。
 */
@ApplicationScoped
public class SessionWorkspaceResolver implements WorkspaceResolver {

    @Override
    public Workspace resolve(AgentSession session) {
        String path = session.workspacePath()
                .orElseThrow(() -> new WorkspaceNotBoundException(session.sessionId()));

        Path root = Path.of(path).toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) {
            throw new IllegalArgumentException("workspace is not a directory: " + root);
        }
        return new Workspace(root);
    }

    /**
     * 校验前端传入路径并归一化为绝对路径字符串，供 Session 持久化。
     */
    public String normalizeBindPath(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("path must not be blank");
        }
        Path root = Path.of(path).toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) {
            throw new IllegalArgumentException("workspace is not a directory: " + root);
        }
        return root.toString();
    }
}
