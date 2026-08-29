package org.example.agent.application.hook;

import org.example.agent.application.runtime.AgentRunContext;
import org.example.agent.application.runtime.RuntimeContext;
import org.example.agent.domain.session.AgentSession;
import org.example.agent.domain.tool.ToolCall;
import org.example.agent.domain.tool.ToolResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HookServiceTest {

    @Test
    void lifecycleHooks_delegateInOrder() {
        List<String> calls = new ArrayList<>();
        AgentLifecycleHookService service = new AgentLifecycleHookService(List.of(
                recordingLifecycle("first", calls),
                recordingLifecycle("second", calls)));

        AgentRunContext context = newContext();
        service.beforeAgent(context);
        service.afterAgent(context);

        assertEquals(List.of(
                "first:beforeAgent",
                "second:beforeAgent",
                "first:afterAgent",
                "second:afterAgent"
        ), calls);
    }

    @Test
    void modelHooks_delegateInOrder() {
        List<String> calls = new ArrayList<>();
        ModelHookService service = new ModelHookService(List.of(
                recordingModel("first", calls),
                recordingModel("second", calls)));

        AgentRunContext context = newContext();
        service.beforeModel(context);
        service.afterModel(context);

        assertEquals(List.of(
                "first:beforeModel",
                "second:beforeModel",
                "first:afterModel",
                "second:afterModel"
        ), calls);
    }

    @Test
    void toolHooks_delegateInOrder() {
        List<String> calls = new ArrayList<>();
        ToolHookService service = new ToolHookService(List.of(
                recordingTool("first", calls),
                recordingTool("second", calls)));

        AgentRunContext context = newContext();
        ToolCall toolCall = new ToolCall("call_1", "execute", "{}");
        ToolResult toolResult = new ToolResult("call_1", "ok");

        service.beforeTool(context, toolCall);
        service.afterTool(context, toolCall, toolResult);

        assertEquals(List.of(
                "first:beforeTool",
                "second:beforeTool",
                "first:afterTool",
                "second:afterTool"
        ), calls);
    }

    private static AgentRunContext newContext() {
        return new AgentRunContext(
                new AgentSession("S001", "U001"),
                new RuntimeContext());
    }

    private static AgentLifecycleHook recordingLifecycle(String name, List<String> calls) {
        return new AgentLifecycleHook() {
            @Override
            public void beforeAgent(AgentRunContext context) {
                calls.add(name + ":beforeAgent");
            }

            @Override
            public void afterAgent(AgentRunContext context) {
                calls.add(name + ":afterAgent");
            }
        };
    }

    private static ModelHook recordingModel(String name, List<String> calls) {
        return new ModelHook() {
            @Override
            public void beforeModel(AgentRunContext context) {
                calls.add(name + ":beforeModel");
            }

            @Override
            public void afterModel(AgentRunContext context) {
                calls.add(name + ":afterModel");
            }
        };
    }

    private static ToolHook recordingTool(String name, List<String> calls) {
        return new ToolHook() {
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
