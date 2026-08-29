package org.example.agent.domain.session.message;

import java.util.Objects;

/**
 * Agent 会话历史中的一条消息。
 * <p>
 * 按真实发生顺序保存在会话中，构成执行时间线。
 */
public sealed interface AgentMessage
        permits AgentMessage.UserMessage,
                AgentMessage.AssistantMessage,
                AgentMessage.ToolCallMessage,
                AgentMessage.ToolResultMessage {

    /**
     * 用户输入。
     */
    record UserMessage(String content) implements AgentMessage {
        public UserMessage {
            Objects.requireNonNull(content, "content must not be null");
        }
    }

    /**
     * 模型产生的普通回复。
     */
    record AssistantMessage(String content) implements AgentMessage {
        public AssistantMessage {
            Objects.requireNonNull(content, "content must not be null");
        }
    }

    /**
     * 模型请求调用 Tool；{@code callId} 关联对应的 {@link ToolResultMessage}。
     */
    record ToolCallMessage(
            String callId,
            String toolName,
            String arguments
    ) implements AgentMessage {
        public ToolCallMessage {
            Objects.requireNonNull(callId, "callId must not be null");
            Objects.requireNonNull(toolName, "toolName must not be null");
            Objects.requireNonNull(arguments, "arguments must not be null");
        }
    }

    /**
     * Tool 执行结果；{@code callId} 对应触发该结果的 {@link ToolCallMessage}。
     */
    record ToolResultMessage(
            String callId,
            String result
    ) implements AgentMessage {
        public ToolResultMessage {
            Objects.requireNonNull(callId, "callId must not be null");
            Objects.requireNonNull(result, "result must not be null");
        }
    }
}
