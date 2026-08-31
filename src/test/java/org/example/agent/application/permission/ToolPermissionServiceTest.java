package org.example.agent.application.permission;

import org.example.agent.application.event.AgentEvent;
import org.example.agent.application.event.AgentEvent.ToolApprovalRequiredEvent;
import org.example.agent.application.event.AgentEvent.ToolApprovalResolvedEvent;
import org.example.agent.application.hook.ToolHookService;
import org.example.agent.application.runtime.AgentRunContext;
import org.example.agent.application.runtime.RuntimeContext;
import org.example.agent.application.tool.FunctionalTool;
import org.example.agent.application.tool.ToolExecutionService;
import org.example.agent.domain.session.AgentSession;
import org.example.agent.domain.session.message.AgentMessage;
import org.example.agent.domain.tool.ToolCall;
import org.example.agent.domain.tool.ToolResult;
import org.example.agent.domain.workspace.Workspace;
import org.example.agent.infrastructure.config.AgentConfig;
import org.example.agent.infrastructure.config.AgentConfiguration;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolPermissionServiceTest {

    @Test
    void deny_skipsToolExecution_andWritesErrorResult() {
        ToolPermissionService permissions = newPermissions("deny", "allow");
        AtomicBoolean executed = new AtomicBoolean(false);
        ToolExecutionService execution = new ToolExecutionService(
                new ToolHookService(List.of()),
                permissions);

        AgentRunContext context = newContext(executed);
        List<AgentEvent> events = new ArrayList<>();
        execution.execute(context, new ToolCall("c1", "bash", "{\"command\":\"rm -rf /\"}"), events::add);

        assertFalse(executed.get());
        assertEquals(1, events.size());
        assertInstanceOf(AgentEvent.ToolExecutionEndEvent.class, events.get(0));
        AgentMessage.ToolResultMessage result =
                (AgentMessage.ToolResultMessage) context.session().messages().get(0);
        assertTrue(result.result().contains("Permission bash"));
        assertTrue(result.result().startsWith("Error:"));
    }

    @Test
    void allow_executesTool() {
        ToolPermissionService permissions = newPermissions("allow", "allow");
        AtomicBoolean executed = new AtomicBoolean(false);
        ToolExecutionService execution = new ToolExecutionService(
                new ToolHookService(List.of()),
                permissions);

        AgentRunContext context = newContext(executed);
        List<AgentEvent> events = new ArrayList<>();
        execution.execute(context, new ToolCall("c1", "bash", "{}"), events::add);

        assertTrue(executed.get());
        assertEquals(2, events.size());
        assertInstanceOf(AgentEvent.ToolExecutionStartEvent.class, events.get(0));
        assertInstanceOf(AgentEvent.ToolExecutionEndEvent.class, events.get(1));
    }

    @Test
    void ask_approve_thenExecutes() throws Exception {
        ApprovalService approvalService = new ApprovalService(Duration.ofSeconds(5));
        ToolPermissionService permissions = newPermissions("ask", "allow", approvalService);
        AtomicBoolean executed = new AtomicBoolean(false);
        ToolExecutionService execution = new ToolExecutionService(
                new ToolHookService(List.of()),
                permissions);

        AgentRunContext context = newContext(executed);
        List<AgentEvent> events = new ArrayList<>();
        CountDownLatch waiting = new CountDownLatch(1);

        Thread runner = Thread.startVirtualThread(() -> {
            waiting.countDown();
            execution.execute(context, new ToolCall("c1", "bash", "{}"), events::add);
        });

        assertTrue(waiting.await(2, TimeUnit.SECONDS));
        awaitRequiredEvent(events);
        approvalService.approve("S001", "c1");
        runner.join(Duration.ofSeconds(2));

        assertTrue(executed.get());
        assertInstanceOf(ToolApprovalRequiredEvent.class, events.get(0));
        assertInstanceOf(ToolApprovalResolvedEvent.class, events.get(1));
        assertTrue(((ToolApprovalResolvedEvent) events.get(1)).approved());
        assertInstanceOf(AgentEvent.ToolExecutionStartEvent.class, events.get(2));
        assertInstanceOf(AgentEvent.ToolExecutionEndEvent.class, events.get(3));
    }

    @Test
    void ask_deny_skipsExecution() throws Exception {
        ApprovalService approvalService = new ApprovalService(Duration.ofSeconds(5));
        ToolPermissionService permissions = newPermissions("ask", "allow", approvalService);
        AtomicBoolean executed = new AtomicBoolean(false);
        ToolExecutionService execution = new ToolExecutionService(
                new ToolHookService(List.of()),
                permissions);

        AgentRunContext context = newContext(executed);
        List<AgentEvent> events = new ArrayList<>();
        CountDownLatch waiting = new CountDownLatch(1);

        Thread runner = Thread.startVirtualThread(() -> {
            waiting.countDown();
            execution.execute(context, new ToolCall("c1", "bash", "{}"), events::add);
        });

        assertTrue(waiting.await(2, TimeUnit.SECONDS));
        awaitRequiredEvent(events);
        approvalService.deny("S001", "c1");
        runner.join(Duration.ofSeconds(2));

        assertFalse(executed.get());
        assertInstanceOf(ToolApprovalRequiredEvent.class, events.get(0));
        assertFalse(((ToolApprovalResolvedEvent) events.get(1)).approved());
        assertInstanceOf(AgentEvent.ToolExecutionEndEvent.class, events.get(2));
    }

    @Test
    void edit_mapsToWritePermission() {
        ToolPermissionService permissions = newPermissions("allow", "deny");
        AgentRunContext context = new AgentRunContext(
                new AgentSession("S001", "U001"),
                new RuntimeContext());

        Optional<ToolResult> denied = permissions.authorize(
                context,
                new ToolCall("c1", "edit", "{}"),
                event -> {
                });

        assertTrue(denied.isPresent());
        assertTrue(denied.get().content().contains("Permission write"));
    }

    @Test
    void read_isNotGated() {
        ToolPermissionService permissions = newPermissions("deny", "deny");
        Optional<ToolResult> denied = permissions.authorize(
                new AgentRunContext(new AgentSession("S001", "U001"), new RuntimeContext()),
                new ToolCall("c1", "read", "{}"),
                event -> {
                });
        assertTrue(denied.isEmpty());
    }

    @Test
    void powershell_mapsToBashPermission() {
        ToolPermissionService permissions = newPermissions("deny", "allow");
        Optional<ToolResult> denied = permissions.authorize(
                new AgentRunContext(new AgentSession("S001", "U001"), new RuntimeContext()),
                new ToolCall("c1", "powershell", "{\"command\":\"dir\"}"),
                event -> {
                });
        assertTrue(denied.isPresent());
        assertTrue(denied.get().content().contains("Permission bash"));
    }

    private static ToolPermissionService newPermissions(String bash, String write) {
        return newPermissions(bash, write, new ApprovalService(Duration.ofSeconds(1)));
    }

    private static ToolPermissionService newPermissions(
            String bash,
            String write,
            ApprovalService approvalService) {
        AgentConfig config = new AgentConfig(
                AgentConfig.ModelConfig.defaults(),
                new AgentConfig.PermissionsConfig(bash, write, "deny"),
                AgentConfig.LoopConfig.defaults());
        return new ToolPermissionService(new AgentConfiguration(config), approvalService);
    }

    private static AgentRunContext newContext(AtomicBoolean executed) {
        RuntimeContext runtime = new RuntimeContext()
                .workspace(new Workspace(Path.of("").toAbsolutePath()))
                .addTool(new FunctionalTool(
                        "bash",
                        "bash",
                        "{}",
                        (call, workspace) -> {
                            executed.set(true);
                            return ToolResult.ok(call.callId(), "ran");
                        }));
        return new AgentRunContext(new AgentSession("S001", "U001"), runtime);
    }

    private static void awaitRequiredEvent(List<AgentEvent> events) throws InterruptedException {
        for (int i = 0; i < 50; i++) {
            if (!events.isEmpty() && events.get(0) instanceof ToolApprovalRequiredEvent) {
                return;
            }
            Thread.sleep(20);
        }
        throw new AssertionError("ToolApprovalRequiredEvent not emitted");
    }
}
