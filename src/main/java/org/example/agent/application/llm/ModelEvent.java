package org.example.agent.application.llm;

import java.util.Objects;

/**
 * 模型流式输出事件。
 * <p>
 * 由 {@link ModelClient#stream(ModelRequest)} 产生，再由 Agent Loop 映射为 {@code AgentEvent}。
 */
public sealed interface ModelEvent
        permits ModelEvent.TextDelta,
                ModelEvent.ToolCall {

    /**
     * 文本增量。
     */
    record TextDelta(String delta) implements ModelEvent {
        public TextDelta {
            Objects.requireNonNull(delta, "delta must not be null");
        }
    }

    /**
     * 模型请求的一次完整 Tool 调用。
     */
    record ToolCall(
            String callId,
            String toolName,
            String arguments
    ) implements ModelEvent {
        public ToolCall {
            Objects.requireNonNull(callId, "callId must not be null");
            Objects.requireNonNull(toolName, "toolName must not be null");
            Objects.requireNonNull(arguments, "arguments must not be null");
        }
    }
}
