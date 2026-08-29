package org.example.agent.infrastructure.environment;

import jakarta.enterprise.context.ApplicationScoped;
import org.example.agent.domain.environment.MachineEnvironment;
import org.example.agent.domain.environment.MachineEnvironmentProbe;

/**
 * 基于 JVM system properties 探测宿主机环境。
 */
@ApplicationScoped
public class SystemMachineEnvironmentProbe implements MachineEnvironmentProbe {

    @Override
    public MachineEnvironment probe() {
        return new MachineEnvironment(
                property("os.name", "unknown"),
                property("os.version", "unknown"),
                property("os.arch", "unknown"),
                property("file.separator", "/"),
                property("line.separator", "\n"),
                property("user.home", ""),
                property("user.name", ""),
                property("java.version", "unknown"));
    }

    private static String property(String key, String defaultValue) {
        String value = System.getProperty(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
