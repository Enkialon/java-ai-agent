package org.example.agent.application.hook;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.example.agent.application.runtime.AgentRunContext;
import org.example.agent.application.runtime.RuntimeContext;
import org.example.agent.domain.environment.MachineEnvironment;
import org.example.agent.domain.environment.MachineEnvironment.OsFamily;
import org.example.agent.domain.environment.MachineEnvironmentProbe;
import org.example.agent.domain.workspace.Workspace;

import java.util.Locale;
import java.util.Objects;

/**
 * 每轮调用模型前，将宿主机机器环境写入 Runtime Context 的 {@code environmentInfo}，
 * 再由 ContextBuilder 并入 System Context。
 * <p>
 * 使用覆盖写（而非 {@link RuntimeContext#inject}），避免多 turn 重复累积。
 */
@ApplicationScoped
public class MachineEnvironmentModelHook implements ModelHook {

    private final MachineEnvironmentProbe probe;

    @Inject
    public MachineEnvironmentModelHook(MachineEnvironmentProbe probe) {
        this.probe = Objects.requireNonNull(probe, "probe must not be null");
    }

    @Override
    public void beforeModel(AgentRunContext context) {
        Objects.requireNonNull(context, "context must not be null");
        MachineEnvironment env = probe.probe();
        Workspace workspace = context.runtime().workspace();
        String workspaceRoot = workspace == null ? null : workspace.root().toString();
        context.runtime().environmentInfo(format(env, workspaceRoot));
    }

    static String format(MachineEnvironment env, String workspaceRoot) {
        OsFamily family = env.osFamily();
        StringBuilder sb = new StringBuilder();
        sb.append("# Runtime Environment\n");
        sb.append("OS: ").append(env.osName())
                .append(" ").append(env.osVersion())
                .append(" (").append(env.osArch()).append(")\n");
        sb.append("OS family: ").append(family.name().toLowerCase(Locale.ROOT)).append('\n');
        sb.append("Path separator: ").append(visibleSeparator(env.fileSeparator())).append('\n');
        if (workspaceRoot != null && !workspaceRoot.isBlank()) {
            sb.append("Workspace: ").append(workspaceRoot).append('\n');
        }
        sb.append("User home: ").append(env.userHome()).append('\n');
        sb.append("User: ").append(env.userName()).append('\n');
        sb.append("Java: ").append(env.javaVersion()).append('\n');
        sb.append("Command guidance: ").append(commandGuidance(family));
        return sb.toString();
    }

    private static String visibleSeparator(String separator) {
        if ("\\".equals(separator)) {
            return "`\\` (Windows)";
        }
        if ("/".equals(separator)) {
            return "`/` (POSIX)";
        }
        return "`" + separator + "`";
    }

    private static String commandGuidance(OsFamily family) {
        return switch (family) {
            case WINDOWS ->
                    "Host is Windows. Prefer Windows-compatible commands and paths. "
                            + "Unix-only utilities (e.g. ls, grep, uname) may fail unless bash/Git Bash/WSL is available.";
            case MACOS ->
                    "Host is macOS. Prefer POSIX shell commands and Unix-style paths.";
            case LINUX ->
                    "Host is Linux. Prefer POSIX shell commands and Unix-style paths.";
            case OTHER ->
                    "Prefer commands and path styles matching the reported OS.";
        };
    }
}
