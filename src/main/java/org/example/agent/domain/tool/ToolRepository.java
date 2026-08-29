package org.example.agent.domain.tool;

import java.util.List;

/**
 * Tool 定义仓储。
 */
public interface ToolRepository {

    List<ToolDefinition> findAll();
}
