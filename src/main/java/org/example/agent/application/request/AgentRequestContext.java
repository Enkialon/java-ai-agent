package org.example.agent.application.request;

/**
 * 当前 HTTP 请求身份。
 *
 * @param sessionId Agent 会话 ID
 * @param userId    当前用户 ID
 */
public record AgentRequestContext(
        String sessionId,
        String userId) {
}
