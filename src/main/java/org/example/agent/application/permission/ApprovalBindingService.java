package org.example.agent.application.permission;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.example.agent.application.session.AgentSessionManager;
import org.example.agent.domain.permission.ApprovalNotFoundException;
import org.example.agent.domain.session.AgentSession;

/**
 * Session 维度的审批应用服务：校验会话归属后完成 approve/deny。
 */
@ApplicationScoped
public class ApprovalBindingService {

    private final AgentSessionManager sessionManager;
    private final ApprovalService approvalService;

    @Inject
    public ApprovalBindingService(
            AgentSessionManager sessionManager,
            ApprovalService approvalService) {
        this.sessionManager = sessionManager;
        this.approvalService = approvalService;
    }

    public PendingApproval approve(String callId) {
        AgentSession session = sessionManager.getOrCreate();
        PendingApproval pending = requirePending(session.sessionId(), callId);
        approvalService.approve(session.sessionId(), callId);
        return pending;
    }

    public PendingApproval deny(String callId) {
        AgentSession session = sessionManager.getOrCreate();
        PendingApproval pending = requirePending(session.sessionId(), callId);
        approvalService.deny(session.sessionId(), callId);
        return pending;
    }

    public PendingApproval current(String callId) {
        AgentSession session = sessionManager.getOrCreate();
        return requirePending(session.sessionId(), callId);
    }

    private PendingApproval requirePending(String sessionId, String callId) {
        return approvalService.find(sessionId, callId)
                .orElseThrow(() -> new ApprovalNotFoundException(sessionId, callId));
    }
}
