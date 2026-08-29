package org.example.agent.infrastructure.tool;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.example.agent.domain.tool.Tool;
import org.example.agent.domain.tool.ToolRepository;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 内存 Tool 仓储：CDI 启动时收集所有 {@link Tool} Bean，也支持测试手动注册。
 */
@ApplicationScoped
public class InMemoryToolRepository implements ToolRepository {

    private final List<Tool> tools = new CopyOnWriteArrayList<>();

    public InMemoryToolRepository() {
    }

    @Inject
    public InMemoryToolRepository(Instance<Tool> toolBeans) {
        toolBeans.forEach(tools::add);
    }

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
