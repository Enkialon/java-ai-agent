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
        assertTrue(repository.findById("S001").isPresent());
    }

    @Test
    void listForCurrentUser_returnsOwnedSessionsNewestFirst() {
        AgentSession older = new AgentSession("S001", "U001", 100L);
        AgentSession newer = new AgentSession("S002", "U001", 200L);
        AgentSession otherUser = new AgentSession("S003", "U002", 300L);
        repository.save(older);
        repository.save(newer);
        repository.save(otherUser);
        newer.addMessage(new UserMessage("第二条"));

        var summaries = manager("S002", "U001").listForCurrentUser();

        assertEquals(2, summaries.size());
        assertEquals("S002", summaries.get(0).sessionId());
        assertEquals("第二条", summaries.get(0).title());
        assertEquals("S001", summaries.get(1).sessionId());
        assertEquals("新会话", summaries.get(1).title());
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
        session.bindWorkspace("/tmp/demo");
        session.addMessage(new UserMessage("帮我查看当前目录"));
        session.addMessage(new AssistantMessage("我先查看当前目录"));
        session.addMessage(new ToolCallMessage("call_001", "execute", "{\"command\":\"ls\"}"));
        session.addMessage(new ToolResultMessage("call_001", "src\nbuild.gradle"));
        session.addMessage(new AssistantMessage("当前目录包含 src、build.gradle..."));
        manager.save(session);

        AgentSession restored = manager.getOrCreate();

        assertEquals(5, restored.messages().size());
        assertEquals(6L, restored.version());
        assertEquals("/tmp/demo", restored.workspacePath().orElseThrow());
        assertTrue(restored.messages().get(0) instanceof UserMessage);
        assertTrue(restored.messages().get(1) instanceof AssistantMessage);
        assertTrue(restored.messages().get(2) instanceof ToolCallMessage);
        assertTrue(restored.messages().get(3) instanceof ToolResultMessage);
        assertTrue(restored.messages().get(4) instanceof AssistantMessage);

        UserMessage first = (UserMessage) restored.messages().get(0);
        assertEquals("帮我查看当前目录", first.content());
    }
}
