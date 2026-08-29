package org.example.agent.application.runtime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.example.agent.application.workspace.WorkspaceResolver;
import org.example.agent.domain.prompt.PromptRepository;
import org.example.agent.domain.session.AgentSession;
import org.example.agent.domain.skill.SkillRepository;
import org.example.agent.domain.tool.ToolRepository;
import org.example.agent.domain.workspace.Workspace;

/**
 * 通过各仓储组装本轮 {@link RuntimeContext}。
 * <p>
 * Hook 注入（含机器环境）不属于仓储材料，由运行过程中动态写入
 * （例如 {@link org.example.agent.application.hook.MachineEnvironmentModelHook}）。
 */
@ApplicationScoped
public class RuntimeContextService {

    private final PromptRepository promptRepository;
    private final SkillRepository skillRepository;
    private final ToolRepository toolRepository;
    private final WorkspaceResolver workspaceResolver;

    @Inject
    public RuntimeContextService(
            PromptRepository promptRepository,
            SkillRepository skillRepository,
            ToolRepository toolRepository,
            WorkspaceResolver workspaceResolver) {
        this.promptRepository = promptRepository;
        this.skillRepository = skillRepository;
        this.toolRepository = toolRepository;
        this.workspaceResolver = workspaceResolver;
    }

    public RuntimeContext load(AgentSession session) {
        Workspace workspace = workspaceResolver.resolve(session);

        RuntimeContext context = new RuntimeContext();
        context.workspace(workspace);
        promptRepository.findSystemPrompt().ifPresent(context::systemPrompt);
        promptRepository.findAgentsMd().ifPresent(context::agentsMd);
        skillRepository.findAll().forEach(context::addSkill);
        toolRepository.findAll().forEach(context::addTool);
        return context;
    }

    public AgentRunContext createRunContext(AgentSession session) {
        return new AgentRunContext(session, load(session));
    }
}
