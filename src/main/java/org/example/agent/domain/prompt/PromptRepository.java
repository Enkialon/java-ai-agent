package org.example.agent.domain.prompt;

import java.util.Optional;

/**
 * System Prompt / AGENTS.md 等提示词材料仓储。
 */
public interface PromptRepository {

    Optional<String> findSystemPrompt();

    Optional<String> findAgentsMd();
}
