package org.example.agent.infrastructure.sandbox;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.nio.file.Path;
import java.util.Objects;

/**
 * 组合 {@link SandboxRuntime} + {@link ProcessRunner}，对外提供统一命令执行入口。
 */
@ApplicationScoped
public class SandboxCommandExecutor {

    private final ProcessRunner runner;
    private final SandboxRuntime sandboxRuntime;

    @Inject
    public SandboxCommandExecutor(ProcessRunner runner, SandboxRuntime sandboxRuntime) {
        this.runner = runner;
        this.sandboxRuntime = sandboxRuntime;
    }

    public CommandResult execute(
            Path cwd,
            String command,
            long timeoutSeconds,
            int maxOutputChars) throws Exception {
        Objects.requireNonNull(cwd, "cwd must not be null");
        Objects.requireNonNull(command, "command must not be null");

        ProcessBuilder builder = sandboxRuntime.createProcess(cwd, command);
        builder.redirectErrorStream(true);
        return runner.run(builder, timeoutSeconds, maxOutputChars);
    }
}
