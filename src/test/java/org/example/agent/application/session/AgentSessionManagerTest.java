package org.example.agent.application.session;

import org.example.agent.application.request.AgentRequestContext;
import org.example.agent.domain.session.AgentSession;
import org.example.agent.domain.session.SessionAccessDeniedException;
import org.example.agent.domain.session.message.AgentMessage.AssistantMessage;
import org.example.agent.domain.session.message.AgentMessage.ToolCallMessage;
import org.example.agent.domain.session.message.AgentMessage.ToolResultMessage;
import org.example.agent.domain.session.message.AgentMessage.UserMessage;
import org.example.agent.infrastructure.session.InMemoryAgentSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentSessionManagerTest {

    private InMemoryAgentSessionRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryAgentSessionRepository();
    }

    private AgentSessionManager manager(String sessionId, String userId) {
        return new AgentSessionManager(
                repository,
                new AgentRequestContext(sessionId, userId));
    }

    @Test
    void getOrCreate_createsNewSession() {
        AgentSession session = manager("S001", "U001").getOrCreate();

        assertEquals("S001", session.sessionId());
        assertEquals("U001", session.userId());
        assertTrue(session.messages().isEmpty());
        assertEquals(0L, session.version());
    }

    @Test
    void getOrCreate_returnsExistingSession_forSameUser() {
        AgentSessionManager manager = manager("S001", "U001");
        AgentSession created = manager.getOrCreate();
        created.addMessage(new UserMessage("hello"));
        manager.save(created);

        AgentSession loaded = manager.getOrCreate();

        assertSame(created, loaded);
        assertEquals(1, loaded.messages().size());
    }

    @Test
    void getOrCreate_deniesAccess_whenUserMismatch() {
        AgentSessionManager owner = manager("S001", "U001");
        owner.save(owner.getOrCreate());

        assertThrows(SessionAccessDeniedException.class,
                () -> manager("S001", "U002").getOrCreate());
    }

    @Test
    void save_andReload_preservesOrderedHistory() {
        AgentSessionManager manager = manager("S001", "U001");
        AgentSession session = manager.getOrCreate();
        session.addMessage(new UserMessage("帮我查看当前目录"));
        session.addMessage(new AssistantMessage("我先查看当前目录"));
        session.addMessage(new ToolCallMessage("call_001", "execute", "{\"command\":\"ls\"}"));
        session.addMessage(new ToolResultMessage("call_001", "src\nbuild.gradle"));
        session.addMessage(new AssistantMessage("当前目录包含 src、build.gradle..."));
        manager.save(session);

        AgentSession restored = manager.getOrCreate();

        assertEquals(5, restored.messages().size());
        assertEquals(5L, restored.version());
        assertTrue(restored.messages().get(0) instanceof UserMessage);
        assertTrue(restored.messages().get(1) instanceof AssistantMessage);
        assertTrue(restored.messages().get(2) instanceof ToolCallMessage);
        assertTrue(restored.messages().get(3) instanceof ToolResultMessage);
        assertTrue(restored.messages().get(4) instanceof AssistantMessage);

        UserMessage first = (UserMessage) restored.messages().get(0);
        assertEquals("帮我查看当前目录", first.content());
    }
}
