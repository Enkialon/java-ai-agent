package org.example.agent.domain.environment;

import java.util.Optional;

/**
 * 当前运行环境信息仓储。
 */
public interface EnvironmentRepository {

    Optional<String> findCurrent();
}
