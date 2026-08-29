package org.example.agent.domain.tool;

import java.util.List;

/**
 * Tool 仓储：存放可执行的 {@link Tool}（描述与能力合一）。
 */
public interface ToolRepository {

    List<Tool> findAll();
}
