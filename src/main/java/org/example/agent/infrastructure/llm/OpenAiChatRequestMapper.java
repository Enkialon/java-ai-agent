package org.example.agent.infrastructure.llm;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.core.JsonValue;
import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;
import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessageFunctionToolCall;
import com.openai.models.chat.completions.ChatCompletionToolMessageParam;
import org.example.agent.application.llm.LlmContext;
import org.example.agent.domain.session.message.AgentMessage;
import org.example.agent.domain.skill.SkillDescriptor;
import org.example.agent.domain.tool.ToolSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 将 {@link LlmContext} 映射为 OpenAI Chat Completions 请求参数。
 */
public final class OpenAiChatRequestMapper {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public OpenAiChatRequestMapper() {
        this(new ObjectMapper());
    }

    public OpenAiChatRequestMapper(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    public ChatCompletionCreateParams toCreateParams(LlmContext context, String model) {
        ChatCompletionCreateParams.Builder builder = ChatCompletionCreateParams.builder()
                .model(model);

        String system = buildSystemPrompt(context);
        if (!system.isBlank()) {
            builder.addSystemMessage(system);
        }

        appendHistory(builder, context.history());

        for (ToolSpec tool : context.tools()) {
            builder.addFunctionTool(toFunctionDefinition(tool));
        }

        return builder.build();
    }

    private String buildSystemPrompt(LlmContext context) {
        List<String> sections = new ArrayList<>(context.systemSections());
        if (!context.skills().isEmpty()) {
            StringBuilder skills = new StringBuilder("Available skills:");
            for (SkillDescriptor skill : context.skills()) {
                skills.append("\n- ")
                        .append(skill.name())
                        .append(": ")
                        .append(skill.description())
                        .append(" (")
                        .append(skill.location())
                        .append(')');
            }
            sections.add(skills.toString());
        }
        return String.join("\n\n", sections);
    }

    private void appendHistory(
            ChatCompletionCreateParams.Builder builder,
            List<AgentMessage> history) {
        int i = 0;
        while (i < history.size()) {
            AgentMessage message = history.get(i);
            switch (message) {
                case AgentMessage.UserMessage user -> {
                    builder.addUserMessage(user.content());
                    i++;
                }
                case AgentMessage.AssistantMessage assistant -> {
                    i = appendAssistantWithOptionalToolCalls(builder, assistant, history, i + 1);
                }
                case AgentMessage.ToolCallMessage toolCall -> {
                    // 无前置 AssistantText 时，单独合成一条仅含 tool_calls 的 assistant 消息
                    i = appendOrphanToolCalls(builder, history, i);
                }
                case AgentMessage.ToolResultMessage toolResult -> {
                    builder.addMessage(ChatCompletionToolMessageParam.builder()
                            .toolCallId(toolResult.callId())
                            .content(toolResult.result())
                            .build());
                    i++;
                }
            }
        }
    }

    private int appendAssistantWithOptionalToolCalls(
            ChatCompletionCreateParams.Builder builder,
            AgentMessage.AssistantMessage assistant,
            List<AgentMessage> history,
            int nextIndex) {
        ChatCompletionAssistantMessageParam.Builder assistantBuilder =
                ChatCompletionAssistantMessageParam.builder()
                        .content(assistant.content());

        int i = nextIndex;
        while (i < history.size() && history.get(i) instanceof AgentMessage.ToolCallMessage toolCall) {
            assistantBuilder.addToolCall(toFunctionToolCall(toolCall));
            i++;
        }
        builder.addMessage(assistantBuilder.build());
        return i;
    }

    private int appendOrphanToolCalls(
            ChatCompletionCreateParams.Builder builder,
            List<AgentMessage> history,
            int startIndex) {
        ChatCompletionAssistantMessageParam.Builder assistantBuilder =
                ChatCompletionAssistantMessageParam.builder();
        int i = startIndex;
        while (i < history.size() && history.get(i) instanceof AgentMessage.ToolCallMessage toolCall) {
            assistantBuilder.addToolCall(toFunctionToolCall(toolCall));
            i++;
        }
        builder.addMessage(assistantBuilder.build());
        return i;
    }

    private static ChatCompletionMessageFunctionToolCall toFunctionToolCall(
            AgentMessage.ToolCallMessage toolCall) {
        return ChatCompletionMessageFunctionToolCall.builder()
                .id(toolCall.callId())
                .function(ChatCompletionMessageFunctionToolCall.Function.builder()
                        .name(toolCall.toolName())
                        .arguments(toolCall.arguments())
                        .build())
                .build();
    }

    private FunctionDefinition toFunctionDefinition(ToolSpec tool) {
        FunctionDefinition.Builder definition = FunctionDefinition.builder()
                .name(tool.name())
                .description(tool.description());
        parseParameters(tool.inputSchema()).ifPresent(definition::parameters);
        return definition.build();
    }

    private java.util.Optional<FunctionParameters> parseParameters(String inputSchema) {
        if (inputSchema == null || inputSchema.isBlank()) {
            return java.util.Optional.empty();
        }
        try {
            Map<String, Object> schema = objectMapper.readValue(inputSchema, MAP_TYPE);
            FunctionParameters.Builder parameters = FunctionParameters.builder();
            schema.forEach((key, value) ->
                    parameters.putAdditionalProperty(key, JsonValue.from(value)));
            return java.util.Optional.of(parameters.build());
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "invalid tool inputSchema JSON: " + e.getMessage(), e);
        }
    }
}
