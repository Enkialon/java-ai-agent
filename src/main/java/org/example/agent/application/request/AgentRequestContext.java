package org.example.agent.application.request;

/**
 * 当前 HTTP 请求身份。
 */
public class AgentRequestContext {

    private final String sessionId;
    private final String userId;

    public AgentRequestContext(String sessionId, String userId) {
        this.sessionId = sessionId;
        this.userId = userId;
    }

    public String sessionId() {
        return sessionId;
    }

    public String userId() {
        return userId;
    }
}
