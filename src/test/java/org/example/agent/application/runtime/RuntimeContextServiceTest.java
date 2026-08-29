package org.example.agent.application.runtime;

import org.example.agent.application.tool.FunctionalTool;
import org.example.agent.application.workspace.SessionWorkspaceResolver;
import org.example.agent.domain.session.AgentSession;
import org.example.agent.domain.session.WorkspaceNotBoundException;
import org.example.agent.domain.skill.SkillDescriptor;
import org.example.agent.domain.tool.Tool;
import org.example.agent.domain.tool.ToolResult;
import org.example.agent.infrastructure.prompt.InMemoryPromptRepository;
import org.example.agent.infrastructure.skill.InMemorySkillRepository;
import org.example.agent.infrastructure.tool.InMemoryToolRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeContextServiceTest {

    @TempDir
    Path tempDir;

    private InMemoryPromptRepository promptRepository;
    private InMemorySkillRepository skillRepository;
    private InMemoryToolRepository toolRepository;
    private RuntimeContextService service;

    @BeforeEach
    void setUp() {
        promptRepository = new InMemoryPromptRepository();
        skillRepository = new InMemorySkillRepository();
        toolRepository = new InMemoryToolRepository();
        service = new RuntimeContextService(
                promptRepository,
                skillRepository,
                toolRepository,
                new SessionWorkspaceResolver());
    }

    @Test
    void load_assemblesRuntimeContextFromRepositories() {
        SkillDescriptor skill = new SkillDescriptor(
                "wechat-chat",
                "查询微信聊天记录",
                ".agents/skills/wechat-chat/SKILL.md");
        Tool tool = new FunctionalTool(
                "queryOrder",
                "查询订单",
                "{\"type\":\"object\",\"properties\":{\"orderId\":{\"type\":\"string\"}}}",
                (call, workspace) -> ToolResult.ok(call.callId(), ""));

        promptRepository.saveSystemPrompt("You are a helpful agent.");
        promptRepository.saveAgentsMd("# AGENTS.md");
        skillRepository.save(skill);
        toolRepository.save(tool);

        AgentSession session = boundSession();
        RuntimeContext context = service.load(session);

        assertEquals("You are a helpful agent.", context.systemPrompt());
        assertEquals("# AGENTS.md", context.agentsMd());
        assertEquals(1, context.skills().size());
        assertEquals(skill, context.skills().get(0));
        assertEquals(1, context.tools().size());
        assertSame(tool, context.tools().get(0));
        assertNull(context.environmentInfo());
        assertTrue(context.hookInjections().isEmpty());
        assertEquals(tempDir.toAbsolutePath().normalize(), context.workspace().root());
    }

    @Test
    void createRunContext_bindsSessionAndLoadedRuntime() {
        promptRepository.saveSystemPrompt("system");
        AgentSession session = boundSession();

        AgentRunContext runContext = service.createRunContext(session);

        assertSame(session, runContext.session());
        assertEquals("system", runContext.runtime().systemPrompt());
        assertEquals(tempDir.toAbsolutePath().normalize(), runContext.runtime().workspace().root());
    }

    @Test
    void createRunContext_requiresBoundWorkspace() {
        AgentSession session = new AgentSession("S001", "U001");

        assertThrows(WorkspaceNotBoundException.class, () -> service.createRunContext(session));
    }

    private AgentSession boundSession() {
        AgentSession session = new AgentSession("S001", "U001");
        session.bindWorkspace(tempDir.toAbsolutePath().normalize().toString());
        return session;
    }
}
