package org.example.agent.infrastructure.llm;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.http.StreamResponse;
import com.openai.models.chat.completions.ChatCompletionChunk;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import org.example.agent.application.llm.ModelClient;
import org.example.agent.application.llm.ModelEvent;
import org.example.agent.application.llm.ModelEventSink;
import org.example.agent.application.llm.ModelRequest;
import org.example.agent.infrastructure.config.AgentConfig;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/**
 * 基于 openai-java SDK 的 OpenAI-compatible Chat Completions 客户端。
 * <p>
 * DeepSeek / OpenAI 共用此实现，差异仅在 baseUrl / model / apiKey。
 * <p>
 * 不使用 {@code ChatCompletionAccumulator}：DeepSeek 等兼容接口常在最终 chunk
 * 只带 {@code usage}、不带 {@code choices}，累加器会抛
 * {@code IllegalStateException: choices is required}。
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
        TreeMap<Long, PendingToolCall> pendingToolCalls = new TreeMap<>();

        try (StreamResponse<ChatCompletionChunk> streamResponse =
                     client.chat().completions().createStreaming(params)) {
            streamResponse.stream().forEach(chunk -> handleChunk(chunk, sink, pendingToolCalls));
        }

        for (PendingToolCall pending : pendingToolCalls.values()) {
            if (pending.id == null || pending.name == null) {
                continue;
            }
            sink.emit(new ModelEvent.ToolCall(
                    pending.id,
                    pending.name,
                    pending.arguments.toString()));
        }
    }

    private static void handleChunk(
            ChatCompletionChunk chunk,
            ModelEventSink sink,
            TreeMap<Long, PendingToolCall> pendingToolCalls) {
        for (ChatCompletionChunk.Choice choice : safeChoices(chunk)) {
            ChatCompletionChunk.Choice.Delta delta = choice.delta();
            delta.content().ifPresent(content -> {
                if (!content.isEmpty()) {
                    sink.emit(new ModelEvent.TextDelta(content));
                }
            });
            delta.toolCalls().ifPresent(toolCalls -> {
                for (ChatCompletionChunk.Choice.Delta.ToolCall toolCall : toolCalls) {
                    mergeToolCall(pendingToolCalls, toolCall);
                }
            });
        }
    }

    /**
     * usage-only 等末包可能缺少 choices；用安全读取避免 SDK getRequired 抛错。
     */
    static List<ChatCompletionChunk.Choice> safeChoices(ChatCompletionChunk chunk) {
        return chunk._choices().asKnown().orElse(List.of());
    }

    static void mergeToolCall(
            TreeMap<Long, PendingToolCall> pendingToolCalls,
            ChatCompletionChunk.Choice.Delta.ToolCall toolCall) {
        PendingToolCall pending = pendingToolCalls.computeIfAbsent(
                toolCall.index(),
                ignored -> new PendingToolCall());
        toolCall.id().ifPresent(id -> pending.id = id);
        toolCall.function().ifPresent(function -> {
            function.name().ifPresent(name -> pending.name = name);
            function.arguments().ifPresent(args -> pending.arguments.append(args));
        });
    }

    static final class PendingToolCall {
        String id;
        String name;
        final StringBuilder arguments = new StringBuilder();
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

    private static Optional<String> fallbackApiKey(String type) {
        return switch (type) {
            case "deepseek" -> env("DEEPSEEK_API_KEY");
            case "openai" -> env("OPENAI_API_KEY");
            default -> Optional.empty();
        };
    }

    private static Optional<String> env(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(value.trim());
    }
}
