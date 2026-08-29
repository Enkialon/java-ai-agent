package org.example.agent.application.event;

/**
 * Agent 事件输出通道。
 * <p>
 * Agent 核心只负责 {@code emit}，不关心 SSE / Multi / WebSocket。
 */
@FunctionalInterface
public interface AgentEventSink {

    void emit(AgentEvent event);
}
