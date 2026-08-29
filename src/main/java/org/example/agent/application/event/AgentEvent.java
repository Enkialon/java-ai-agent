package org.example.agent.application.event;

import org.example.agent.application.loop.AgentRunResult;

import java.util.Objects;

/**
 * Agent 运行过程事件（流式输出 / 生命周期），通常不全部持久化。
 * <p>
 * 与 {@link org.example.agent.domain.session.message.AgentMessage}（会话历史）不同：
 * Event 负责实时推送，Message 负责最终落库。
 */
public sealed interface AgentEvent
        permits AgentEvent.AgentStartEvent,
                AgentEvent.TextDeltaEvent,
                AgentEvent.ToolExecutionStartEvent,
                AgentEvent.ToolExecutionEndEvent,
                AgentEvent.MessageEndEvent,
                AgentEvent.AgentEndEvent {

    /**
     * 一次用户 prompt 对应的 Agent Run 开始。
     */
    record AgentStartEvent() implements AgentEvent {
    }

    /**
     * Assistant 文本增量，用于前端流式展示。
     */
    record TextDeltaEvent(String delta) implements AgentEvent {
        public TextDeltaEvent {
            Objects.requireNonNull(delta, "delta must not be null");
        }
    }

    /**
     * Tool 开始执行。
     */
    record ToolExecutionStartEvent(
            String callId,
            String toolName
    ) implements AgentEvent {
        public ToolExecutionStartEvent {
            Objects.requireNonNull(callId, "callId must not be null");
            Objects.requireNonNull(toolName, "toolName must not be null");
        }
    }

    /**
     * Tool 执行结束。
     */
    record ToolExecutionEndEvent(
            String callId,
            String result
    ) implements AgentEvent {
        public ToolExecutionEndEvent {
            Objects.requireNonNull(callId, "callId must not be null");
            Objects.requireNonNull(result, "result must not be null");
        }
    }

    /**
     * 一条完整消息结束；此时才应将最终内容写入 Session History。
     */
    record MessageEndEvent(String content) implements AgentEvent {
        public MessageEndEvent {
            Objects.requireNonNull(content, "content must not be null");
        }
    }

    /**
     * 一次 Agent Run 结束。
     */
    record AgentEndEvent(AgentRunResult result) implements AgentEvent {
        public AgentEndEvent {
            Objects.requireNonNull(result, "result must not be null");
        }
    }
}
