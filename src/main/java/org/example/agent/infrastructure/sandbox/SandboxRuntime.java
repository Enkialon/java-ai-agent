package org.example.agent.infrastructure.sandbox;

import java.nio.file.Path;

/**
 * 沙箱运行时：根据工作目录与命令构造 {@link ProcessBuilder}。
 * <p>
 * 可替换实现例如 Bubblewrap / Docker / Windows Sandbox / Remote。
 */
public interface SandboxRuntime {

    ProcessBuilder createProcess(Path workingDirectory, String command);
}
