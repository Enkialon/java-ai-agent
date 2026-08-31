package org.example.agent.infrastructure.sandbox;

import org.example.agent.domain.environment.MachineEnvironment;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SandboxRuntimeProducerTest {

    @Test
    void forOsFamily_windowsUsesPowerShell() {
        SandboxRuntime runtime = SandboxRuntimeProducer.forOsFamily(MachineEnvironment.OsFamily.WINDOWS);
        assertInstanceOf(WindowsSandboxRuntime.class, runtime);

        ProcessBuilder builder = runtime.createProcess(Path.of("."), "Get-ChildItem");
        List<String> command = builder.command();
        assertEquals("powershell.exe", command.get(0));
        assertTrue(command.contains("-Command"));
        assertEquals("Get-ChildItem", command.get(command.size() - 1));
    }

    @Test
    void forOsFamily_linuxUsesBash() {
        SandboxRuntime runtime = SandboxRuntimeProducer.forOsFamily(MachineEnvironment.OsFamily.LINUX);
        assertInstanceOf(PosixSandboxRuntime.class, runtime);

        ProcessBuilder builder = runtime.createProcess(Path.of("."), "echo hi");
        assertEquals(List.of("bash", "-lc", "echo hi"), builder.command());
    }
}
