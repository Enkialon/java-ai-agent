package org.example.agent.infrastructure.llm;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.http.StreamResponse;
import com.openai.helpers.ChatCompletionAccumulator;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionChunk;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessage;
import com.openai.models.chat.completions.ChatCompletionMessageToolCall;
import org.example.agent.application.llm.ModelClient;
import org.example.agent.application.llm.ModelEvent;
import org.example.agent.application.llm.ModelEventSink;
import org.example.agent.application.llm.ModelRequest;
import org.example.agent.infrastructure.config.AgentConfig;

import java.util.Objects;

/**
 * 基于 openai-java SDK 的 OpenAI-compatible Chat Completions 客户端。
 * <p>
 * DeepSeek / OpenAI 共用此实现，差异仅在 baseUrl / model / apiKey。
 */
public final class OpenAiCompatibleModelClient implements ModelClient {

    private final OpenAIClient client;
    private final String model;
    private final OpenAiChatRequestMapper mapper;

    public OpenAiCompatibleModelClient(AgentConfig.ModelClientSettings settings) {
        this(
                buildClient(settings),
                settings.modelOptional().orElseThrow(() ->
                        new IllegalArgumentException("model is required for type=" + settings.type())),
                new OpenAiChatRequestMapper());
    }

    OpenAiCompatibleModelClient(OpenAIClient client, String model, OpenAiChatRequestMapper mapper) {
        this.client = Objects.requireNonNull(client, "client must not be null");
        this.model = Objects.requireNonNull(model, "model must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public void stream(ModelRequest request, ModelEventSink sink) {
        ChatCompletionCreateParams params = mapper.toCreateParams(request.context(), model);
        ChatCompletionAccumulator accumulator = ChatCompletionAccumulator.create();

        try (StreamResponse<ChatCompletionChunk> streamResponse =
                     client.chat().completions().createStreaming(params)) {
            streamResponse.stream().forEach(chunk -> {
                accumulator.accumulate(chunk);
                emitTextDeltas(chunk, sink);
            });
        }

        emitToolCalls(accumulator.chatCompletion(), sink);
    }

    private static void emitTextDeltas(ChatCompletionChunk chunk, ModelEventSink sink) {
        for (ChatCompletionChunk.Choice choice : chunk.choices()) {
            choice.delta().content().ifPresent(delta -> {
                if (!delta.isEmpty()) {
                    sink.emit(new ModelEvent.TextDelta(delta));
                }
            });
        }
    }

    private static void emitToolCalls(ChatCompletion completion, ModelEventSink sink) {
        if (completion.choices().isEmpty()) {
            return;
        }
        ChatCompletionMessage message = completion.choices().getFirst().message();
        message.toolCalls().ifPresent(toolCalls -> {
            for (ChatCompletionMessageToolCall toolCall : toolCalls) {
                if (!toolCall.isFunction()) {
                    continue;
                }
                var functionCall = toolCall.asFunction();
                sink.emit(new ModelEvent.ToolCall(
                        functionCall.id(),
                        functionCall.function().name(),
                        functionCall.function().arguments()));
            }
        });
    }

    private static OpenAIClient buildClient(AgentConfig.ModelClientSettings settings) {
        String apiKey = settings.apiKeyOptional()
                .or(() -> fallbackApiKey(settings.type()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "api-key is required for type=" + settings.type()));
        OpenAIOkHttpClient.Builder builder = OpenAIOkHttpClient.builder().apiKey(apiKey);
        settings.baseUrlOptional().ifPresent(builder::baseUrl);
        return builder.build();
    }

    private static java.util.Optional<String> fallbackApiKey(String type) {
        return switch (type) {
            case "deepseek" -> env("DEEPSEEK_API_KEY");
            case "openai" -> env("OPENAI_API_KEY");
            default -> java.util.Optional.empty();
        };
    }

    private static java.util.Optional<String> env(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(value.trim());
    }
}
