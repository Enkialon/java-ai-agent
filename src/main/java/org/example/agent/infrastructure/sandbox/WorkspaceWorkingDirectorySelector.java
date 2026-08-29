package org.example.agent.infrastructure.sandbox;

import jakarta.enterprise.context.ApplicationScoped;
import org.example.agent.domain.workspace.Workspace;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 基于给定 {@link Workspace} 解析并校验工作目录，禁止逃逸工作区。
 */
@ApplicationScoped
public class WorkspaceWorkingDirectorySelector implements WorkingDirectorySelector {

    @Override
    public Path select(Workspace workspace, String requested) {
        if (requested == null || requested.isBlank()) {
            return workspace.root();
        }

        Path resolved = workspace.resolvePath(requested);
        if (!Files.exists(resolved)) {
            throw new IllegalArgumentException("working directory does not exist: " + requested);
        }
        if (!Files.isDirectory(resolved)) {
            throw new IllegalArgumentException("working directory is not a directory: " + requested);
        }
        return resolved;
    }
}
