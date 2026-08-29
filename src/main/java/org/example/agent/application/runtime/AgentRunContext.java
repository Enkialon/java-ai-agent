package org.example.agent.application.runtime;

import org.example.agent.domain.session.AgentSession;
import org.example.agent.domain.session.message.AgentMessage;

import java.util.List;
import java.util.Objects;

/**
 * 单次 Agent Loop 运行上下文。
 * <p>
 * 组合本轮 {@link RuntimeContext} 与跨请求的 {@link AgentSession}（Session History）。
 */
public record AgentRunContext(AgentSession session, RuntimeContext runtime) {

    public AgentRunContext(AgentSession session, RuntimeContext runtime) {
        this.session = Objects.requireNonNull(session, "session must not be null");
        this.runtime = Objects.requireNonNull(runtime, "runtime must not be null");
    }

    public List<AgentMessage> history() {
        return session.messages();
    }
}
