package org.example.agent.domain.session;

/**
 * 当前用户无权访问指定 Session 时抛出。
 */
public class SessionAccessDeniedException extends RuntimeException {

    public SessionAccessDeniedException(String sessionId, String userId) {
        super("User '" + userId + "' is not allowed to access session '" + sessionId + "'");
    }
}
