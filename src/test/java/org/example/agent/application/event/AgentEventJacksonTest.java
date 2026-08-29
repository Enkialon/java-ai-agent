package org.example.agent.application.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.agent.application.loop.AgentRunResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentEventJacksonTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void serializesWithTypeDiscriminator() throws Exception {
        String start = mapper.writeValueAsString(new AgentEvent.AgentStartEvent());
        assertTrue(start.contains("\"type\":\"agent_start\""), start);

        String delta = mapper.writeValueAsString(new AgentEvent.TextDeltaEvent("hi"));
        assertTrue(delta.contains("\"type\":\"text_delta\""), delta);
        assertTrue(delta.contains("\"delta\":\"hi\""), delta);

        String toolStart = mapper.writeValueAsString(
                new AgentEvent.ToolExecutionStartEvent("c1", "bash", "{\"command\":\"ls\"}"));
        assertTrue(toolStart.contains("\"type\":\"tool_start\""), toolStart);
        assertTrue(toolStart.contains("\"arguments\""), toolStart);

        String toolEnd = mapper.writeValueAsString(
                new AgentEvent.ToolExecutionEndEvent("c1", true, "ok"));
        assertTrue(toolEnd.contains("\"type\":\"tool_end\""), toolEnd);
        assertTrue(toolEnd.contains("\"success\":true"), toolEnd);

        String approval = mapper.writeValueAsString(
                new AgentEvent.ToolApprovalRequiredEvent("c1", "bash", "{}", "bash"));
        assertTrue(approval.contains("\"type\":\"tool_approval_required\""), approval);

        String end = mapper.writeValueAsString(
                new AgentEvent.AgentEndEvent(new AgentRunResult("done")));
        assertTrue(end.contains("\"type\":\"agent_end\""), end);
    }
}
