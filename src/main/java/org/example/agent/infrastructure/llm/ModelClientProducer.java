package org.example.agent.infrastructure.llm;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.example.agent.application.llm.ModelClient;
import org.example.agent.infrastructure.config.AgentConfiguration;

/**
 * 根据 {@code agent.yaml} 中 {@code agent.model.active} 产出唯一 {@link ModelClient}。
 */
@ApplicationScoped
public class ModelClientProducer {

    private final AgentConfiguration configuration;

    @Inject
    public ModelClientProducer(AgentConfiguration configuration) {
        this.configuration = configuration;
    }

    @Produces
    @ApplicationScoped
    public ModelClient modelClient() {
        return ModelClientFactory.create(configuration.get().model());
    }
}
