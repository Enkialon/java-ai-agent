package org.example.agent.infrastructure.sandbox;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.example.agent.domain.environment.MachineEnvironment;
import org.example.agent.domain.environment.MachineEnvironmentProbe;

/**
 * 按宿主机 OS 产出唯一 {@link SandboxRuntime}。
 */
@ApplicationScoped
public class SandboxRuntimeProducer {

    private final MachineEnvironmentProbe probe;

    @Inject
    public SandboxRuntimeProducer(MachineEnvironmentProbe probe) {
        this.probe = probe;
    }

    @Produces
    @ApplicationScoped
    public SandboxRuntime sandboxRuntime() {
        return forOsFamily(probe.probe().osFamily());
    }

    static SandboxRuntime forOsFamily(MachineEnvironment.OsFamily osFamily) {
        return switch (osFamily) {
            case WINDOWS -> new WindowsSandboxRuntime();
            default -> new PosixSandboxRuntime();
        };
    }
}
