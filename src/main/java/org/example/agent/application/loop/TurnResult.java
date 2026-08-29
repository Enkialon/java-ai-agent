package org.example.agent.application.loop;

import org.example.agent.domain.tool.ToolCall;

import java.util.List;
import java.util.Objects;

/**
 * 一次模型 Turn 的聚合结果。
 * <p>
 * Agent Loop 只根据它决定：继续执行 Tool，还是结束 Agent。
 */
public record TurnResult(
        String assistantText,
        List<ToolCall> toolCalls
) {

    public TurnResult {
        Objects.requireNonNull(assistantText, "assistantText must not be null");
        toolCalls = List.copyOf(Objects.requireNonNull(toolCalls, "toolCalls must not be null"));
    }

    public boolean hasToolCalls() {
        return !toolCalls.isEmpty();
    }
}
