package org.example.agent.application.event;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.example.agent.application.loop.AgentRunResult;

import java.util.Objects;

/**
 * Agent 运行过程事件（流式输出 / 生命周期），通常不全部持久化。
 * <p>
 * 与 {@link org.example.agent.domain.session.message.AgentMessage}（会话历史）不同：
 * Event 负责实时推送，Message 负责最终落库。
 * <p>
 * SSE JSON 带 {@code type} 判别字段，供 Web UI 增量渲染。
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = AgentEvent.AgentStartEvent.class, name = "agent_start"),
        @JsonSubTypes.Type(value = AgentEvent.TextDeltaEvent.class, name = "text_delta"),
        @JsonSubTypes.Type(value = AgentEvent.ToolApprovalRequiredEvent.class, name = "tool_approval_required"),
        @JsonSubTypes.Type(value = AgentEvent.ToolApprovalResolvedEvent.class, name = "tool_approval_resolved"),
        @JsonSubTypes.Type(value = AgentEvent.ToolExecutionStartEvent.class, name = "tool_start"),
        @JsonSubTypes.Type(value = AgentEvent.ToolExecutionEndEvent.class, name = "tool_end"),
        @JsonSubTypes.Type(value = AgentEvent.MessageEndEvent.class, name = "message_end"),
        @JsonSubTypes.Type(value = AgentEvent.AgentEndEvent.class, name = "agent_end")
})
public sealed interface AgentEvent
        permits AgentEvent.AgentStartEvent,
                AgentEvent.TextDeltaEvent,
                AgentEvent.ToolApprovalRequiredEvent,
                AgentEvent.ToolApprovalResolvedEvent,
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
     * 工具需要人机审批（permissions=ask）。
     */
    record ToolApprovalRequiredEvent(
            String callId,
            String toolName,
            String arguments,
            String permission
    ) implements AgentEvent {
        public ToolApprovalRequiredEvent {
            Objects.requireNonNull(callId, "callId must not be null");
            Objects.requireNonNull(toolName, "toolName must not be null");
            Objects.requireNonNull(arguments, "arguments must not be null");
            Objects.requireNonNull(permission, "permission must not be null");
        }
    }

    /**
     * 人机审批已结束（批准或拒绝/超时）。
     */
    record ToolApprovalResolvedEvent(
            String callId,
            boolean approved
    ) implements AgentEvent {
        public ToolApprovalResolvedEvent {
            Objects.requireNonNull(callId, "callId must not be null");
        }
    }

    /**
     * Tool 开始执行。
     */
    record ToolExecutionStartEvent(
            String callId,
            String toolName,
            String arguments
    ) implements AgentEvent {
        public ToolExecutionStartEvent {
            Objects.requireNonNull(callId, "callId must not be null");
            Objects.requireNonNull(toolName, "toolName must not be null");
            Objects.requireNonNull(arguments, "arguments must not be null");
        }
    }

    /**
     * Tool 执行结束。
     */
    record ToolExecutionEndEvent(
            String callId,
            boolean success,
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
