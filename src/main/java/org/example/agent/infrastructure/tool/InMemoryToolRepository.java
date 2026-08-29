package org.example.agent.infrastructure.tool;

import jakarta.enterprise.context.ApplicationScoped;
import org.example.agent.domain.tool.ToolDefinition;
import org.example.agent.domain.tool.ToolRepository;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 第一版内存 Tool 仓储。
 */
@ApplicationScoped
public class InMemoryToolRepository implements ToolRepository {

    private final List<ToolDefinition> tools = new CopyOnWriteArrayList<>();

    @Override
    public List<ToolDefinition> findAll() {
        return List.copyOf(tools);
    }

    public void save(ToolDefinition tool) {
        tools.add(tool);
    }

    public void clear() {
        tools.clear();
    }
}
