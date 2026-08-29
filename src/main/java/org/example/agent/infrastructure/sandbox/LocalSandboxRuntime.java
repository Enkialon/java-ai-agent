package org.example.agent.infrastructure.sandbox;

import jakarta.enterprise.context.ApplicationScoped;

import java.nio.file.Path;
import java.util.Objects;

/**
 * 本地无隔离实现：仅用 {@code bash -lc} 并设置初始 cwd。
 * <p>
 * 不构成 OS 级 sandbox；真实隔离应换成其他 {@link SandboxRuntime} 实现。
 */
@ApplicationScoped
public class LocalSandboxRuntime implements SandboxRuntime {

    @Override
    public ProcessBuilder createProcess(Path workingDirectory, String command) {
        Objects.requireNonNull(workingDirectory, "workingDirectory must not be null");
        Objects.requireNonNull(command, "command must not be null");

        ProcessBuilder builder = new ProcessBuilder("bash", "-lc", command);
        builder.directory(workingDirectory.toFile());
        return builder;
    }
}
