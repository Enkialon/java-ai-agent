package org.example.agent.infrastructure.environment;

import jakarta.enterprise.context.ApplicationScoped;
import org.example.agent.domain.environment.EnvironmentRepository;

import java.util.Optional;

/**
 * 第一版内存环境信息仓储（可选静态覆盖；机器环境由 Hook 注入）。
 */
@ApplicationScoped
public class InMemoryEnvironmentRepository implements EnvironmentRepository {

    private volatile String current;

    @Override
    public Optional<String> findCurrent() {
        return Optional.ofNullable(current);
    }

    public void save(String current) {
        this.current = current;
    }

    public void clear() {
        this.current = null;
    }
}
