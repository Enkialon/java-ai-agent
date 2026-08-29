package org.example.agent.domain.skill;

import java.util.Objects;

/**
 * Skill 描述，供 Runtime Context / 模型发现可用 Skill。
 */
public record SkillDescriptor(
        String name,
        String description,
        String location
) {

    public SkillDescriptor {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(description, "description must not be null");
        Objects.requireNonNull(location, "location must not be null");
    }
}
