package org.example.agent.infrastructure.llm;

import org.example.agent.application.llm.ModelClient;
import org.example.agent.application.llm.ModelEventSink;
import org.example.agent.application.llm.ModelRequest;
import org.example.agent.infrastructure.config.AgentConfig;

/**
 * DeepSeek ModelClient：OpenAI-compatible API，底层使用 openai-java SDK。
 */
public final class DeepSeekModelClient implements ModelClient {

    public static final String DEFAULT_BASE_URL = "https://api.deepseek.com";
    public static final String DEFAULT_MODEL = "deepseek-v4-flash";

    private final OpenAiCompatibleModelClient delegate;

    public DeepSeekModelClient(AgentConfig.ModelClientSettings settings) {
        this.delegate = new OpenAiCompatibleModelClient(withDefaults(settings));
    }

    DeepSeekModelClient(OpenAiCompatibleModelClient delegate) {
        this.delegate = delegate;
    }

    @Override
    public void stream(ModelRequest request, ModelEventSink sink) {
        delegate.stream(request, sink);
    }

    private static AgentConfig.ModelClientSettings withDefaults(
            AgentConfig.ModelClientSettings settings) {
        return new AgentConfig.ModelClientSettings(
                "deepseek",
                settings.apiKey(),
                settings.baseUrlOptional().orElse(DEFAULT_BASE_URL),
                settings.modelOptional().orElse(DEFAULT_MODEL));
    }
}
