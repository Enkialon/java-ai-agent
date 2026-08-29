package org.example.agent.domain.environment;

/**
 * 探测当前宿主机 {@link MachineEnvironment}。
 */
public interface MachineEnvironmentProbe {

    MachineEnvironment probe();
}
