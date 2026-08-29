package org.example.agent.domain.permission;

/**
 * 待审批的工具调用不存在或已失效（超时/已处理）。
 */
public class ApprovalNotFoundException extends RuntimeException {

    public ApprovalNotFoundException(String sessionId, String callId) {
        super("No pending approval for session '" + sessionId + "' callId '" + callId + "'");
    }
}
