package org.example.agent.infrastructure.tool.builtin;

import org.example.agent.infrastructure.sandbox.SandboxCommandExecutor;
import org.example.agent.infrastructure.sandbox.WorkingDirectorySelector;

/**
 * 通过 {@link SandboxCommandExecutor} 执行 PowerShell 命令（Windows 宿主机）。
 */
public final class PowerShellTool extends AbstractShellTool {

    public PowerShellTool(
            WorkingDirectorySelector workingDirectorySelector,
            SandboxCommandExecutor commandExecutor) {
        super(workingDirectorySelector, commandExecutor);
    }

    PowerShellTool(
            WorkingDirectorySelector workingDirectorySelector,
            SandboxCommandExecutor commandExecutor,
            long timeoutSeconds,
            int maxOutputChars) {
        super(workingDirectorySelector, commandExecutor, timeoutSeconds, maxOutputChars);
    }

    @Override
    public String name() {
        return "powershell";
    }

    @Override
    public String description() {
        return "Run a PowerShell command in the workspace. "
                + "Prefer Windows-native commands and paths (e.g. Get-ChildItem, dir). "
                + "Optional working_directory must stay inside the workspace. "
                + "Times out after " + timeoutSeconds() + "s; output capped at "
                + maxOutputChars() + " chars.";
    }
}
