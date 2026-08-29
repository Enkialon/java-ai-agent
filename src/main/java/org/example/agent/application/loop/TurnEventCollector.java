package org.example.agent.application.loop;

import org.example.agent.application.event.AgentEvent.TextDeltaEvent;
import org.example.agent.application.event.AgentEventSink;
import org.example.agent.application.llm.ModelEvent;
import org.example.agent.application.llm.ModelEventSink;
import org.example.agent.domain.tool.ToolCall;

import java.util.ArrayList;
import java.util.List;

/**
 * 收集单轮 Model 流事件，并生成最终 {@link TurnResult}。
 */
final class TurnEventCollector implements ModelEventSink {

    private final AgentEventSink sink;

    private final StringBuilder text = new StringBuilder();
    private final List<ToolCall> toolCalls = new ArrayList<>();

    TurnEventCollector(AgentEventSink sink) {
        this.sink = sink;
    }

    @Override
    public void emit(ModelEvent event) {
        switch (event) {
            case ModelEvent.TextDelta delta -> onTextDelta(delta);
            case ModelEvent.ToolCall toolCall -> onToolCall(toolCall);
        }
    }

    private void onTextDelta(ModelEvent.TextDelta event) {
        text.append(event.delta());
        sink.emit(new TextDeltaEvent(event.delta()));
    }

    private void onToolCall(ModelEvent.ToolCall event) {
        toolCalls.add(new ToolCall(
                event.callId(),
                event.toolName(),
                event.arguments()));
    }

    TurnResult result() {
        return new TurnResult(
                text.toString(),
                List.copyOf(toolCalls));
    }
}
