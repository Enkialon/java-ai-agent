package org.example.agent.infrastructure.prompt;

import jakarta.enterprise.context.ApplicationScoped;
import org.example.agent.domain.prompt.PromptRepository;

import java.util.Optional;

/**
 * 第一版内存 Prompt 仓储。
 */
@ApplicationScoped
public class InMemoryPromptRepository implements PromptRepository {

    private volatile String systemPrompt;
    private volatile String agentsMd;

    @Override
    public Optional<String> findSystemPrompt() {
        return Optional.ofNullable(systemPrompt);
    }

    @Override
    public Optional<String> findAgentsMd() {
        return Optional.ofNullable(agentsMd);
    }

    public void saveSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public void saveAgentsMd(String agentsMd) {
        this.agentsMd = agentsMd;
    }

    public void clear() {
        this.systemPrompt = null;
        this.agentsMd = null;
    }
}
