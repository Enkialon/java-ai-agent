package org.example.agent.domain.skill;

import java.util.List;

/**
 * Skill 描述仓储。
 */
public interface SkillRepository {

    List<SkillDescriptor> findAll();
}
