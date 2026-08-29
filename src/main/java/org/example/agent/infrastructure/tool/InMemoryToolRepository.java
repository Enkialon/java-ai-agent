package org.example.agent.infrastructure.tool;

import jakarta.enterprise.context.ApplicationScoped;
import org.example.agent.domain.tool.Tool;
import org.example.agent.domain.tool.ToolRepository;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 第一版内存 Tool 仓储。
 */
@ApplicationScoped
public class InMemoryToolRepository implements ToolRepository {

    private final List<Tool> tools = new CopyOnWriteArrayList<>();

    @Override
    public List<Tool> findAll() {
        return List.copyOf(tools);
    }

    public void save(Tool tool) {
        tools.add(tool);
    }

    public void clear() {
        tools.clear();
    }
}
