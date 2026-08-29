package org.example.agent.application.hook;

import org.example.agent.application.runtime.AgentRunContext;
import org.example.agent.application.runtime.RuntimeContext;
import org.example.agent.domain.session.AgentSession;
import org.example.agent.domain.tool.ToolCall;
import org.example.agent.domain.tool.ToolResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AgentHookServiceTest {

    @Test
    void delegatesToAllHooksInOrder() {
        List<String> calls = new ArrayList<>();
        AgentHook first = recordingHook("first", calls);
        AgentHook second = recordingHook("second", calls);

        AgentHookService service = new AgentHookService(List.of(first, second));
        AgentRunContext context = new AgentRunContext(
                new AgentSession("S001", "U001"),
                new RuntimeContext());
        ToolCall toolCall = new ToolCall();
        ToolResult toolResult = new ToolResult();

        service.beforeAgent(context);
        service.beforeModel(context);
        service.afterModel(context);
        service.beforeTool(context, toolCall);
        service.afterTool(context, toolCall, toolResult);
        service.afterAgent(context);

        assertEquals(List.of(
                "first:beforeAgent",
                "second:beforeAgent",
                "first:beforeModel",
                "second:beforeModel",
                "first:afterModel",
                "second:afterModel",
                "first:beforeTool",
                "second:beforeTool",
                "first:afterTool",
                "second:afterTool",
                "first:afterAgent",
                "second:afterAgent"
        ), calls);
    }

    @Test
    void emptyHooks_isNoOp() {
        AtomicInteger counter = new AtomicInteger();
        AgentHookService service = new AgentHookService(List.of());
        AgentRunContext context = new AgentRunContext(
                new AgentSession("S001", "U001"),
                new RuntimeContext());

        service.beforeAgent(context);
        assertEquals(0, counter.get());
    }

    private static AgentHook recordingHook(String name, List<String> calls) {
        return new AgentHook() {
            @Override
            public void beforeAgent(AgentRunContext context) {
                calls.add(name + ":beforeAgent");
            }

            @Override
            public void afterAgent(AgentRunContext context) {
                calls.add(name + ":afterAgent");
            }

            @Override
            public void beforeModel(AgentRunContext context) {
                calls.add(name + ":beforeModel");
            }

            @Override
            public void afterModel(AgentRunContext context) {
                calls.add(name + ":afterModel");
            }

            @Override
            public void beforeTool(AgentRunContext context, ToolCall toolCall) {
                calls.add(name + ":beforeTool");
            }

            @Override
            public void afterTool(AgentRunContext context, ToolCall toolCall, ToolResult result) {
                calls.add(name + ":afterTool");
            }
        };
    }
}
