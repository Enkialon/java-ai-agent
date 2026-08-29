package org.example.agent.infrastructure.llm;

import org.example.agent.application.llm.ModelClient;
import org.example.agent.infrastructure.config.AgentConfig;

/**
 * 按配置实例化 {@link ModelClient}。
 */
public final class ModelClientFactory {

    private ModelClientFactory() {
    }

    public static ModelClient create(AgentConfig.ModelConfig modelConfig) {
        return create(modelConfig.activeClient());
    }

    public static ModelClient create(AgentConfig.ModelClientSettings settings) {
        return switch (settings.type()) {
            case "stub" -> new StubModelClient();
            case "deepseek" -> new DeepSeekModelClient(settings);
            case "openai" -> new OpenAIModelClient(settings);
            default -> throw new IllegalArgumentException(
                    "unsupported model client type: " + settings.type()
                            + " (expected stub|deepseek|openai)");
        };
    }
}
