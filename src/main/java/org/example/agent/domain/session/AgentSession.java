package org.example.agent.domain.session;

import org.example.agent.domain.session.message.AgentMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 一次完整的 Agent 会话。
 * <p>
 * 只承载会话身份、工作区绑定与有序历史，不关心存储与 HTTP 等基础设施。
 */
public class AgentSession {

    private final String sessionId;
    private final String userId;
    private String workspacePath;
    private String activeSkill;
    private final List<AgentMessage> messages;
    private long version;

    public AgentSession(String sessionId, String userId) {
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId must not be null");
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.messages = new ArrayList<>();
        this.version = 0L;
    }

    public void addMessage(AgentMessage message) {
        Objects.requireNonNull(message, "message must not be null");
        messages.add(message);
        version++;
    }

    public List<AgentMessage> messages() {
        return Collections.unmodifiableList(messages);
    }

    /**
     * 绑定前端选定的工作区根路径（本机绝对路径）。
     */
    public void bindWorkspace(String workspacePath) {
        Objects.requireNonNull(workspacePath, "workspacePath must not be null");
        if (workspacePath.isBlank()) {
            throw new IllegalArgumentException("workspacePath must not be blank");
        }
        this.workspacePath = workspacePath;
        version++;
    }

    public Optional<String> workspacePath() {
        return Optional.ofNullable(workspacePath);
    }

    public void activateSkill(String skill) {
        this.activeSkill = Objects.requireNonNull(skill, "skill must not be null");
        version++;
    }

    public String sessionId() {
        return sessionId;
    }

    public String userId() {
        return userId;
    }

    public String activeSkill() {
        return activeSkill;
    }

    public long version() {
        return version;
    }
}
