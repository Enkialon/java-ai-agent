package org.example.agent.application.permission;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.example.agent.application.event.AgentEvent.ToolApprovalRequiredEvent;
import org.example.agent.application.event.AgentEvent.ToolApprovalResolvedEvent;
import org.example.agent.application.event.AgentEventSink;
import org.example.agent.application.runtime.AgentRunContext;
import org.example.agent.domain.permission.ApprovalDecision;
import org.example.agent.domain.permission.PermissionMode;
import org.example.agent.domain.tool.ToolCall;
import org.example.agent.domain.tool.ToolResult;
import org.example.agent.infrastructure.config.AgentConfig;
import org.example.agent.infrastructure.config.AgentConfiguration;

import java.util.Objects;
import java.util.Optional;

/**
 * 按 {@code agent.yaml} permissions 对工具调用做 allow / deny / ask 裁决。
 * <p>
 * 映射：{@code bash}→bash；{@code write}/{@code edit}→write；其余工具默认 allow。
 * {@code network} 预留，当前无对应内置工具。
 */
@ApplicationScoped
public class ToolPermissionService {

    private final AgentConfiguration configuration;
    private final ApprovalService approvalService;

    @Inject
    public ToolPermissionService(
            AgentConfiguration configuration,
            ApprovalService approvalService) {
        this.configuration = Objects.requireNonNull(configuration, "configuration must not be null");
        this.approvalService = Objects.requireNonNull(approvalService, "approvalService must not be null");
    }

    /**
     * @return empty 表示放行执行；present 表示拒绝，调用方应写入该结果并跳过真实执行
     */
    public Optional<ToolResult> authorize(
            AgentRunContext context,
            ToolCall toolCall,
            AgentEventSink sink) {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(toolCall, "toolCall must not be null");
        Objects.requireNonNull(sink, "sink must not be null");

        Optional<String> permissionKey = permissionKeyFor(toolCall.toolName());
        if (permissionKey.isEmpty()) {
            return Optional.empty();
        }

        String key = permissionKey.get();
        PermissionMode mode = modeFor(key);
        return switch (mode) {
            case ALLOW -> Optional.empty();
            case DENY -> Optional.of(deniedResult(toolCall, key, "permission denied"));
            case ASK -> ask(context, toolCall, sink, key);
        };
    }

    private Optional<ToolResult> ask(
            AgentRunContext context,
            ToolCall toolCall,
            AgentEventSink sink,
            String permission) {
        sink.emit(new ToolApprovalRequiredEvent(
                toolCall.callId(),
                toolCall.toolName(),
                toolCall.arguments(),
                permission));

        ApprovalDecision decision = approvalService.await(
                context.session().sessionId(),
                toolCall,
                permission);

        boolean approved = decision == ApprovalDecision.APPROVED;
        sink.emit(new ToolApprovalResolvedEvent(toolCall.callId(), approved));

        if (approved) {
            return Optional.empty();
        }
        return Optional.of(deniedResult(toolCall, permission, "user denied approval"));
    }

    private PermissionMode modeFor(String permissionKey) {
        AgentConfig.PermissionsConfig permissions = configuration.get().permissions();
        String raw = switch (permissionKey) {
            case "bash" -> permissions.bash();
            case "write" -> permissions.write();
            case "network" -> permissions.network();
            default -> "allow";
        };
        return PermissionMode.parse(raw);
    }

    /**
     * @return 权限配置键；empty 表示不受 permissions 约束（如 read）
     */
    static Optional<String> permissionKeyFor(String toolName) {
        return switch (toolName) {
            case "bash" -> Optional.of("bash");
            case "write", "edit" -> Optional.of("write");
            default -> Optional.empty();
        };
    }

    private static ToolResult deniedResult(ToolCall toolCall, String permission, String reason) {
        return ToolResult.error(
                toolCall.callId(),
                "Permission " + permission + ": " + reason);
    }
}
