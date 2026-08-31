package org.example.agent.infrastructure.sandbox;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Windows 本地实现：{@code powershell.exe -NoProfile -NonInteractive -Command} 并设置初始 cwd。
 * <p>
 * 不经过 WSL/bash，避免依赖 HCS。
 */
public final class WindowsSandboxRuntime implements SandboxRuntime {

    @Override
    public ProcessBuilder createProcess(Path workingDirectory, String command) {
        Objects.requireNonNull(workingDirectory, "workingDirectory must not be null");
        Objects.requireNonNull(command, "command must not be null");

        ProcessBuilder builder = new ProcessBuilder(
                "powershell.exe",
                "-NoProfile",
                "-NonInteractive",
                "-ExecutionPolicy",
                "Bypass",
                "-Command",
                command);
        builder.directory(workingDirectory.toFile());
        return builder;
    }
}
