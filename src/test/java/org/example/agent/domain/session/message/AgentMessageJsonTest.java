package org.example.agent.domain.session.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.agent.api.AgentHistoryResource;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentMessageJsonTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void serializesPolymorphicMessages() throws Exception {
        List<AgentMessage> messages = List.of(
                new AgentMessage.UserMessage("hello"),
                new AgentMessage.AssistantMessage("hi"),
                new AgentMessage.ToolCallMessage("c1", "bash", "{\"command\":\"ls\"}"),
                new AgentMessage.ToolResultMessage("c1", "exit_code=0\nfile.txt"));

        String json = mapper.writeValueAsString(
                new AgentHistoryResource.HistoryResponse("sess_1", 4L, messages));
        assertTrue(json.contains("user"), () -> json);
        assertTrue(json.contains("tool_call"), () -> json);

        AgentHistoryResource.HistoryResponse roundTrip =
                mapper.readValue(json, AgentHistoryResource.HistoryResponse.class);
        assertEquals(4, roundTrip.messages().size());
        assertInstanceOf(AgentMessage.UserMessage.class, roundTrip.messages().get(0));
        assertEquals("hello", ((AgentMessage.UserMessage) roundTrip.messages().get(0)).content());
        assertInstanceOf(AgentMessage.ToolResultMessage.class, roundTrip.messages().get(3));
    }
}
