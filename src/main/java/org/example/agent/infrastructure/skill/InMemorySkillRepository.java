package org.example.agent.infrastructure.skill;

import jakarta.enterprise.context.ApplicationScoped;
import org.example.agent.domain.skill.SkillDescriptor;
import org.example.agent.domain.skill.SkillRepository;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 第一版内存 Skill 仓储。
 */
@ApplicationScoped
public class InMemorySkillRepository implements SkillRepository {

    private final List<SkillDescriptor> skills = new CopyOnWriteArrayList<>();

    @Override
    public List<SkillDescriptor> findAll() {
        return List.copyOf(skills);
    }

    public void save(SkillDescriptor skill) {
        skills.add(skill);
    }

    public void clear() {
        skills.clear();
    }
}
