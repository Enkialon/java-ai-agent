package org.example.agent.application.runtime;

import org.example.agent.application.llm.ContextBuilder;
import org.example.agent.domain.skill.SkillDescriptor;
import org.example.agent.domain.tool.Tool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 本轮 Runtime / System Context。
 * <p>
 * 一般不写入 Session History，由 {@link ContextBuilder} 与历史一并组装成 LLM Context。
 */
public class RuntimeContext {

    private String systemPrompt;
    private String agentsMd;
    private final List<SkillDescriptor> skills = new ArrayList<>();
    private final List<Tool> tools = new ArrayList<>();
    private final List<String> hookInjections = new ArrayList<>();
    private String environmentInfo;

    public String systemPrompt() {
        return systemPrompt;
    }

    public RuntimeContext systemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
        return this;
    }

    public String agentsMd() {
        return agentsMd;
    }

    public RuntimeContext agentsMd(String agentsMd) {
        this.agentsMd = agentsMd;
        return this;
    }

    public List<SkillDescriptor> skills() {
        return Collections.unmodifiableList(skills);
    }

    public RuntimeContext addSkill(SkillDescriptor skill) {
        skills.add(Objects.requireNonNull(skill, "skill must not be null"));
        return this;
    }

    public List<Tool> tools() {
        return Collections.unmodifiableList(tools);
    }

    public RuntimeContext addTool(Tool tool) {
        tools.add(Objects.requireNonNull(tool, "tool must not be null"));
        return this;
    }

    public Optional<Tool> findTool(String name) {
        Objects.requireNonNull(name, "name must not be null");
        return tools.stream()
                .filter(tool -> tool.name().equals(name))
                .findFirst();
    }

    public List<String> hookInjections() {
        return Collections.unmodifiableList(hookInjections);
    }

    /**
     * Hook 动态注入的上下文片段。
     */
    public RuntimeContext inject(String fragment) {
        hookInjections.add(Objects.requireNonNull(fragment, "fragment must not be null"));
        return this;
    }

    public String environmentInfo() {
        return environmentInfo;
    }

    public RuntimeContext environmentInfo(String environmentInfo) {
        this.environmentInfo = environmentInfo;
        return this;
    }
}
