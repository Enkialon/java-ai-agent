package org.example.agent.application.runtime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.example.agent.domain.environment.EnvironmentRepository;
import org.example.agent.domain.prompt.PromptRepository;
import org.example.agent.domain.session.AgentSession;
import org.example.agent.domain.skill.SkillRepository;
import org.example.agent.domain.tool.ToolRepository;

/**
 * 通过各仓储组装本轮 {@link RuntimeContext}。
 * <p>
 * Hook 注入不属于仓储材料，由运行过程中动态写入。
 */
@ApplicationScoped
public class RuntimeContextService {

    private final PromptRepository promptRepository;
    private final SkillRepository skillRepository;
    private final ToolRepository toolRepository;
    private final EnvironmentRepository environmentRepository;

    @Inject
    public RuntimeContextService(
            PromptRepository promptRepository,
            SkillRepository skillRepository,
            ToolRepository toolRepository,
            EnvironmentRepository environmentRepository) {
        this.promptRepository = promptRepository;
        this.skillRepository = skillRepository;
        this.toolRepository = toolRepository;
        this.environmentRepository = environmentRepository;
    }

    public RuntimeContext load() {
        RuntimeContext context = new RuntimeContext();
        promptRepository.findSystemPrompt().ifPresent(context::systemPrompt);
        promptRepository.findAgentsMd().ifPresent(context::agentsMd);
        skillRepository.findAll().forEach(context::addSkill);
        toolRepository.findAll().forEach(context::addTool);
        environmentRepository.findCurrent().ifPresent(context::environmentInfo);
        return context;
    }

    public AgentRunContext createRunContext(AgentSession session) {
        return new AgentRunContext(session, load());
    }
}
