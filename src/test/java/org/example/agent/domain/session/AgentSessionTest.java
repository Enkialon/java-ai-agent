package org.example.agent.domain.session;

import org.example.agent.domain.session.message.AgentMessage;
import org.example.agent.domain.session.message.AgentMessage.AssistantMessage;
import org.example.agent.domain.session.message.AgentMessage.ToolCallMessage;
import org.example.agent.domain.session.message.AgentMessage.ToolResultMessage;
import org.example.agent.domain.session.message.AgentMessage.UserMessage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentSessionTest {

    @Test
    void addMessage_appendsInOrder_andIncrementsVersion() {
        AgentSession session = new AgentSession("S001", "U001");
        assertEquals(0L, session.version());

        session.addMessage(new UserMessage("帮我查看当前目录"));
        session.addMessage(new AssistantMessage("我先查看当前目录"));
        session.addMessage(new ToolCallMessage("call_001", "execute", "{\"command\":\"ls\"}"));
        session.addMessage(new ToolResultMessage("call_001", "src\nbuild.gradle"));
        session.addMessage(new AssistantMessage("当前目录包含 src、build.gradle..."));

        List<AgentMessage> messages = session.messages();
        assertEquals(5, messages.size());
        assertEquals(5L, session.version());

        assertTrue(messages.get(0) instanceof UserMessage);
        assertTrue(messages.get(1) instanceof AssistantMessage);
        assertTrue(messages.get(2) instanceof ToolCallMessage);
        assertTrue(messages.get(3) instanceof ToolResultMessage);
        assertTrue(messages.get(4) instanceof AssistantMessage);

        ToolCallMessage toolCall = (ToolCallMessage) messages.get(2);
        ToolResultMessage toolResult = (ToolResultMessage) messages.get(3);
        assertEquals(toolCall.callId(), toolResult.callId());
    }

    @Test
    void messages_isUnmodifiable() {
        AgentSession session = new AgentSession("S001", "U001");
        session.addMessage(new UserMessage("hello"));

        assertThrows(UnsupportedOperationException.class,
                () -> session.messages().add(new UserMessage("hack")));
    }

    @Test
    void activateSkill_updatesActiveSkill_andIncrementsVersion() {
        AgentSession session = new AgentSession("S001", "U001");
        session.activateSkill("filesystem");

        assertEquals("filesystem", session.activeSkill());
        assertEquals(1L, session.version());
    }
}
