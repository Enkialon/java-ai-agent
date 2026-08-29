package org.example.agent.infrastructure.llm;

import org.example.agent.application.llm.ModelClient;
import org.example.agent.application.llm.ModelEventSink;
import org.example.agent.application.llm.ModelRequest;
import org.example.agent.infrastructure.config.AgentConfig;

/**
 * OpenAI ModelClient：官方 API，底层使用 openai-java SDK。
 */
public final class OpenAIModelClient implements ModelClient {

    public static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";
    public static final String DEFAULT_MODEL = "gpt-4o";

    private final OpenAiCompatibleModelClient delegate;

    public OpenAIModelClient(AgentConfig.ModelClientSettings settings) {
        this.delegate = new OpenAiCompatibleModelClient(withDefaults(settings));
    }

    OpenAIModelClient(OpenAiCompatibleModelClient delegate) {
        this.delegate = delegate;
    }

    @Override
    public void stream(ModelRequest request, ModelEventSink sink) {
        delegate.stream(request, sink);
    }

    private static AgentConfig.ModelClientSettings withDefaults(
            AgentConfig.ModelClientSettings settings) {
        return new AgentConfig.ModelClientSettings(
                "openai",
                settings.apiKey(),
                settings.baseUrlOptional().orElse(DEFAULT_BASE_URL),
                settings.modelOptional().orElse(DEFAULT_MODEL));
    }
}
