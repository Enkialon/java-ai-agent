package org.example.agent.application.loop;

import java.util.Objects;

/**
 * 一次 Agent Loop 的运行结果。
 *
 * @param message 最终面向用户的回复
 */
public record AgentRunResult(String message) {

    public AgentRunResult {
        Objects.requireNonNull(message, "message must not be null");
    }
}
