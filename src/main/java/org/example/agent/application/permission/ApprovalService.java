package org.example.agent.application.permission;

import jakarta.enterprise.context.ApplicationScoped;
import org.example.agent.domain.permission.ApprovalDecision;
import org.example.agent.domain.permission.ApprovalNotFoundException;
import org.example.agent.domain.tool.ToolCall;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 人机审批闸门：ASK 模式下阻塞等待用户 approve/deny。
 * <p>
 * 设计前提：Agent Loop 跑在虚拟线程上，{@link #await} 阻塞不会占用平台线程。
 */
@ApplicationScoped
public class ApprovalService {

    public static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(5);

    private final ConcurrentHashMap<String, Gate> gates = new ConcurrentHashMap<>();
    private final Duration timeout;

    public ApprovalService() {
        this(DEFAULT_TIMEOUT);
    }

    public ApprovalService(Duration timeout) {
        this.timeout = Objects.requireNonNull(timeout, "timeout must not be null");
    }

    /**
     * 注册待审批项并阻塞直至用户决定或超时。
     * 超时视为 {@link ApprovalDecision#DENIED}。
     */
    public ApprovalDecision await(String sessionId, ToolCall toolCall, String permission) {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(toolCall, "toolCall must not be null");
        Objects.requireNonNull(permission, "permission must not be null");

        String key = key(sessionId, toolCall.callId());
        Gate gate = new Gate(PendingApproval.from(sessionId, toolCall, permission));
        Gate previous = gates.putIfAbsent(key, gate);
        if (previous != null) {
            throw new IllegalStateException(
                    "Approval already pending for session '" + sessionId
                            + "' callId '" + toolCall.callId() + "'");
        }

        try {
            return gate.future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            gates.remove(key, gate);
            gate.future.complete(ApprovalDecision.DENIED);
            return ApprovalDecision.DENIED;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            gates.remove(key, gate);
            gate.future.complete(ApprovalDecision.DENIED);
            return ApprovalDecision.DENIED;
        } catch (java.util.concurrent.ExecutionException e) {
            gates.remove(key, gate);
            throw new IllegalStateException("Approval wait failed", e.getCause());
        } finally {
            gates.remove(key, gate);
        }
    }

    public void approve(String sessionId, String callId) {
        complete(sessionId, callId, ApprovalDecision.APPROVED);
    }

    public void deny(String sessionId, String callId) {
        complete(sessionId, callId, ApprovalDecision.DENIED);
    }

    public Optional<PendingApproval> find(String sessionId, String callId) {
        Gate gate = gates.get(key(sessionId, callId));
        return gate == null ? Optional.empty() : Optional.of(gate.pending);
    }

    private void complete(String sessionId, String callId, ApprovalDecision decision) {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(callId, "callId must not be null");

        Gate gate = gates.get(key(sessionId, callId));
        if (gate == null) {
            throw new ApprovalNotFoundException(sessionId, callId);
        }
        if (!gate.future.complete(decision)) {
            throw new ApprovalNotFoundException(sessionId, callId);
        }
    }

    private static String key(String sessionId, String callId) {
        return sessionId + "\0" + callId;
    }

    private static final class Gate {
        private final PendingApproval pending;
        private final CompletableFuture<ApprovalDecision> future = new CompletableFuture<>();

        private Gate(PendingApproval pending) {
            this.pending = pending;
        }
    }
}
