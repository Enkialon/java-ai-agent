package org.example.agent.application.llm;

import org.example.agent.domain.session.message.AgentMessage;
import org.example.agent.domain.skill.SkillDescriptor;
import org.example.agent.domain.tool.ToolDefinition;

import java.util.List;

/**
 * 本轮真正传给 LLM 的 Context。
 * <p>
 * {@code systemSections}：System Prompt / AGENTS.md / Hook 注入 / 环境等文本片段；
 * {@code skills} / {@code tools}：结构化定义，不压成纯 String；
 * {@code history}：Session History。
 */
public record LlmContext(
        List<String> systemSections,
        List<SkillDescriptor> skills,
        List<ToolDefinition> tools,
        List<AgentMessage> history
) {

    public LlmContext {
        systemSections = List.copyOf(systemSections);
        skills = List.copyOf(skills);
        tools = List.copyOf(tools);
        history = List.copyOf(history);
    }
}
