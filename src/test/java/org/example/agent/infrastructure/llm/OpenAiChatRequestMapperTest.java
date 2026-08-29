package org.example.agent.infrastructure.llm;

import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import org.example.agent.application.llm.LlmContext;
import org.example.agent.domain.session.message.AgentMessage;
import org.example.agent.domain.skill.SkillDescriptor;
import org.example.agent.domain.tool.ToolSpec;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiChatRequestMapperTest {

    private final OpenAiChatRequestMapper mapper = new OpenAiChatRequestMapper();

    @Test
    void toCreateParams_mapsSystemToolsAndHistory() {
        LlmContext context = new LlmContext(
                List.of("You are helpful."),
                List.of(new SkillDescriptor("demo", "Demo skill", "skills/demo")),
                List.of(new ToolSpec(
                        "read",
                        "Read a file",
                        """
                        {"type":"object","properties":{"path":{"type":"string"}},"required":["path"]}
                        """)),
                List.of(
                        new AgentMessage.UserMessage("read README"),
                        new AgentMessage.AssistantMessage("I'll read it."),
                        new AgentMessage.ToolCallMessage(
                                "call_1", "read", "{\"path\":\"README.md\"}"),
                        new AgentMessage.ToolResultMessage("call_1", "# Title")));

        ChatCompletionCreateParams params = mapper.toCreateParams(context, "deepseek-v4-flash");

        assertEquals("deepseek-v4-flash", params.model().asString());
        assertTrue(params.tools().isPresent());
        assertEquals(1, params.tools().orElseThrow().size());
        assertEquals("read", params.tools().orElseThrow().getFirst().asFunction().function().name());

        List<ChatCompletionMessageParam> messages = params.messages();
        assertFalse(messages.isEmpty());
        assertTrue(messages.getFirst().isSystem());
        String system = messages.getFirst().asSystem().content().asText();
        assertTrue(system.contains("You are helpful."));
        assertTrue(system.contains("demo"));
        assertTrue(messages.get(1).isUser());
        assertTrue(messages.get(2).isAssistant());
        assertTrue(messages.get(2).asAssistant().toolCalls().isPresent());
        assertEquals(1, messages.get(2).asAssistant().toolCalls().orElseThrow().size());
        assertTrue(messages.get(3).isTool());
        assertEquals("call_1", messages.get(3).asTool().toolCallId());
    }
}
