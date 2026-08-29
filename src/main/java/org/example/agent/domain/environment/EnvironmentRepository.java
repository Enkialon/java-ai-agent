package org.example.agent.domain.environment;

import java.util.Optional;

/**
 * 可选的静态环境信息覆盖（手工配置等）。
 * <p>
 * 宿主机机器环境由 {@link MachineEnvironmentProbe} 探测，
 * 经 ModelHook 写入 Runtime Context，不经由本仓储。
 */
public interface EnvironmentRepository {

    Optional<String> findCurrent();
}
