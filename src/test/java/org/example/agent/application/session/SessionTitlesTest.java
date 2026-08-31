package org.example.agent.application.session;

import org.example.agent.domain.session.AgentSession;
import org.example.agent.domain.session.message.AgentMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SessionTitlesTest {

    @Test
    void derive_usesFirstUserMessage() {
        AgentSession session = new AgentSession("S1", "U1", 100L);
        session.addMessage(new AgentMessage.UserMessage("帮我读 README"));

        assertEquals("帮我读 README", SessionTitles.derive(session));
    }

    @Test
    void derive_truncatesLongMessage() {
        AgentSession session = new AgentSession("S1", "U1", 100L);
        session.addMessage(new AgentMessage.UserMessage("x".repeat(60)));

        assertEquals(48, SessionTitles.derive(session).length());
    }

    @Test
    void derive_defaultsWhenNoUserMessage() {
        AgentSession session = new AgentSession("S1", "U1", 100L);

        assertEquals("新会话", SessionTitles.derive(session));
    }
}
