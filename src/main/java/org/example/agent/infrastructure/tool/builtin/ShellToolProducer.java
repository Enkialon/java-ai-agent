package org.example.agent.infrastructure.tool.builtin;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.example.agent.domain.environment.MachineEnvironmentProbe;
import org.example.agent.domain.tool.Tool;
import org.example.agent.infrastructure.sandbox.SandboxCommandExecutor;
import org.example.agent.infrastructure.sandbox.WorkingDirectorySelector;

/**
 * 按宿主机 OS 产出唯一 shell 类 {@link Tool}（Windows → powershell，其余 → bash）。
 */
@ApplicationScoped
public class ShellToolProducer {

    private final MachineEnvironmentProbe probe;
    private final WorkingDirectorySelector workingDirectorySelector;
    private final SandboxCommandExecutor commandExecutor;

    @Inject
    public ShellToolProducer(
            MachineEnvironmentProbe probe,
            WorkingDirectorySelector workingDirectorySelector,
            SandboxCommandExecutor commandExecutor) {
        this.probe = probe;
        this.workingDirectorySelector = workingDirectorySelector;
        this.commandExecutor = commandExecutor;
    }

    @Produces
    @ApplicationScoped
    public Tool shellTool() {
        return switch (probe.probe().osFamily()) {
            case WINDOWS -> new PowerShellTool(workingDirectorySelector, commandExecutor);
            default -> new BashTool(workingDirectorySelector, commandExecutor);
        };
    }
}
