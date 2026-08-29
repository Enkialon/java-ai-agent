package org.example.agent.application.hook;

import org.example.agent.application.llm.ContextBuilder;
import org.example.agent.application.runtime.AgentRunContext;
import org.example.agent.application.runtime.RuntimeContext;
import org.example.agent.domain.environment.MachineEnvironment;
import org.example.agent.domain.environment.MachineEnvironment.OsFamily;
import org.example.agent.domain.session.AgentSession;
import org.example.agent.domain.workspace.Workspace;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MachineEnvironmentModelHookTest {

    @TempDir
    Path tempDir;

    @Test
    void beforeModel_writesEnvironmentInfoIntoRuntimeContext() {
        MachineEnvironmentModelHook hook = new MachineEnvironmentModelHook(this::windowsEnv);
        AgentRunContext context = newContext();

        hook.beforeModel(context);

        String info = context.runtime().environmentInfo();
        assertTrue(info.contains("# Runtime Environment"));
        assertTrue(info.contains("OS family: windows"));
        assertTrue(info.contains("Workspace: " + tempDir.toAbsolutePath().normalize()));
        assertTrue(info.contains("Prefer Windows-compatible commands"));
    }

    @Test
    void beforeModel_overwritesPreviousEnvironmentInfoAcrossTurns() {
        MachineEnvironmentModelHook hook = new MachineEnvironmentModelHook(this::windowsEnv);
        AgentRunContext context = newContext();
        context.runtime().environmentInfo("stale");

        hook.beforeModel(context);
        String first = context.runtime().environmentInfo();
        hook.beforeModel(context);
        String second = context.runtime().environmentInfo();

        assertEquals(first, second);
        assertTrue(context.runtime().hookInjections().isEmpty());
    }

    @Test
    void beforeModel_sectionAppearsInSystemContextViaContextBuilder() {
        MachineEnvironmentModelHook hook = new MachineEnvironmentModelHook(this::windowsEnv);
        ContextBuilder builder = new ContextBuilder();
        AgentRunContext context = newContext();

        hook.beforeModel(context);
        List<String> sections = builder.build(context).context().systemSections();

        assertEquals(1, sections.size());
        assertTrue(sections.get(0).startsWith("# Runtime Environment"));
        assertTrue(sections.get(0).contains("OS family: windows"));
    }

    @Test
    void osFamily_detectsWindowsMacLinux() {
        assertEquals(OsFamily.WINDOWS, windowsEnv().osFamily());
        assertEquals(
                OsFamily.MACOS,
                new MachineEnvironment(
                        "Mac OS X", "14.0", "aarch64", "/", "\n", "/Users/a", "a", "21").osFamily());
        assertEquals(
                OsFamily.LINUX,
                new MachineEnvironment(
                        "Linux", "6.8.0", "amd64", "/", "\n", "/home/a", "a", "21").osFamily());
    }

    @Test
    void format_linuxGuidance() {
        String text = MachineEnvironmentModelHook.format(
                new MachineEnvironment("Linux", "6.8.0", "amd64", "/", "\n", "/home/a", "a", "21"),
                "/workspace");
        assertTrue(text.contains("OS family: linux"));
        assertTrue(text.contains("Prefer POSIX shell commands"));
        assertTrue(text.contains("Workspace: /workspace"));
    }

    private AgentRunContext newContext() {
        return new AgentRunContext(
                new AgentSession("S001", "U001"),
                new RuntimeContext().workspace(new Workspace(tempDir)));
    }

    private MachineEnvironment windowsEnv() {
        return new MachineEnvironment(
                "Windows 11",
                "10.0",
                "amd64",
                "\\",
                "\r\n",
                "C:\\Users\\demo",
                "demo",
                "21.0.1");
    }
}
