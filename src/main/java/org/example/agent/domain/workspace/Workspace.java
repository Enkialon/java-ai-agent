package org.example.agent.domain.workspace;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Agent 工作区根目录：文件类 Tool 的路径解析与越界校验基准。
 * <p>
 * 由 Session 绑定的路径解析而来，按次运行创建。
 */
public final class Workspace {

    private final Path root;

    public Workspace(Path root) {
        this.root = Objects.requireNonNull(root, "root must not be null")
                .toAbsolutePath()
                .normalize();
    }

    public Path root() {
        return root;
    }

    /**
     * 将相对（或落在工作区内的绝对）路径解析为规范化绝对路径，并禁止逃逸工作区。
     */
    public Path resolvePath(String path) {
        Objects.requireNonNull(path, "path must not be null");
        if (path.isBlank()) {
            throw new IllegalArgumentException("path must not be blank");
        }

        Path requested = Path.of(path);
        Path resolved = (requested.isAbsolute() ? requested : root.resolve(requested))
                .toAbsolutePath()
                .normalize();

        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("path escapes workspace: " + path);
        }
        return resolved;
    }
}
