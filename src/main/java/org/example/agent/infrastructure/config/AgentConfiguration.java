package org.example.agent.infrastructure.config;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.Objects;

/**
 * 进程级 Agent 配置持有者。
 */
@ApplicationScoped
public class AgentConfiguration {

    private final AgentConfig config;

    public AgentConfiguration() {
        this(AgentConfigLoader.load());
    }

    public AgentConfiguration(AgentConfig config) {
        this.config = Objects.requireNonNull(config, "config must not be null");
    }

    public AgentConfig get() {
        return config;
    }
}
