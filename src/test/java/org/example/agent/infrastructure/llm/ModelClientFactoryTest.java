package org.example.agent.infrastructure.llm;

import org.example.agent.application.llm.ModelClient;
import org.example.agent.infrastructure.config.AgentConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModelClientFactoryTest {

    @Test
    void create_stub() {
        ModelClient client = ModelClientFactory.create(AgentConfig.ModelClientSettings.stub());
        assertInstanceOf(StubModelClient.class, client);
    }

    @Test
    void create_deepseek() {
        ModelClient client = ModelClientFactory.create(new AgentConfig.ModelClientSettings(
                "deepseek", "sk-test", null, null));
        assertInstanceOf(DeepSeekModelClient.class, client);
    }

    @Test
    void create_openai() {
        ModelClient client = ModelClientFactory.create(new AgentConfig.ModelClientSettings(
                "openai", "sk-test", null, null));
        assertInstanceOf(OpenAIModelClient.class, client);
    }

    @Test
    void create_unknown_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                ModelClientFactory.create(new AgentConfig.ModelClientSettings(
                        "anthropic", null, null, null)));
    }
}
