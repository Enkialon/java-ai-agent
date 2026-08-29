package org.example.agent.application.loop;

import org.example.agent.application.event.AgentEvent;
import org.example.agent.application.event.AgentEvent.AgentEndEvent;
import org.example.agent.application.event.AgentEvent.AgentStartEvent;
import org.example.agent.application.event.AgentEvent.MessageEndEvent;
import org.example.agent.application.event.AgentEvent.TextDeltaEvent;
import org.example.agent.application.event.AgentEvent.ToolExecutionEndEvent;
import org.example.agent.application.event.AgentEvent.ToolExecutionStartEvent;
import org.example.agent.application.hook.ModelHook;
import org.example.agent.application.hook.ModelHookService;
import org.example.agent.application.hook.ToolHookService;
import org.example.agent.application.llm.ContextBuilder;
import org.example.agent.application.llm.ModelClient;
import org.example.agent.application.llm.ModelEvent;
import org.example.agent.application.runtime.AgentRunContext;
import org.example.agent.application.runtime.RuntimeContext;
import org.example.agent.application.tool.FunctionalTool;
import org.example.agent.application.tool.ToolExecutionService;
import org.example.agent.domain.session.AgentSession;
import org.example.agent.domain.session.message.AgentMessage;
import org.example.agent.domain.tool.Tool;
import org.example.agent.domain.tool.ToolResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentLoopServiceTest {

    @Test
    void run_withEmptyModel_emitsStartAndEnd() {
        AgentLoopService loopService = newLoopService((request, sink) -> {
        });
        List<AgentEvent> events = new ArrayList<>();

        AgentRunResult result = loopService.run(newContext(), events::add);

        assertEquals("", result.message());
        assertEquals(2, events.size());
        assertInstanceOf(AgentStartEvent.class, events.get(0));
        assertInstanceOf(AgentEndEvent.class, events.get(1));
    }

    @Test
    void run_streamsTextThenEnds_andPersistsAssistantMessage() {
        ModelClient modelClient = (request, sink) -> {
            sink.emit(new ModelEvent.TextDelta("你好"));
            sink.emit(new ModelEvent.TextDelta("世界"));
        };

        AgentRunContext context = newContext();
        List<AgentEvent> events = new ArrayList<>();
        AgentRunResult result = newLoopService(modelClient).run(context, events::add);

        assertEquals("你好世界", result.message());
        assertInstanceOf(AgentStartEvent.class, events.get(0));
        assertEquals(new TextDeltaEvent("你好"), events.get(1));
        assertEquals(new TextDeltaEvent("世界"), events.get(2));
        assertEquals(new MessageEndEvent("你好世界"), events.get(3));
        assertEquals(new AgentEndEvent(new AgentRunResult("你好世界")), events.get(4));
        assertEquals(
                new AgentMessage.AssistantMessage("你好世界"),
                context.session().messages().get(0));
    }

    @Test
    void run_toolCalls_arePersistedBeforeResults_thenAnotherTurn() {
        AtomicInteger turn = new AtomicInteger();
        ModelClient modelClient = (request, sink) -> {
            if (turn.getAndIncrement() == 0) {
                sink.emit(new ModelEvent.TextDelta("查询中"));
                sink.emit(new ModelEvent.ToolCall("call_1", "queryOrder", "{\"id\":\"1\"}"));
                sink.emit(new ModelEvent.ToolCall("call_2", "queryUser", "{\"id\":\"2\"}"));
                return;
            }
            sink.emit(new ModelEvent.TextDelta("完成"));
        };

        Tool queryOrder = echoTool("queryOrder");
        Tool queryUser = echoTool("queryUser");

        AgentRunContext context = newContext(queryOrder, queryUser);
        List<AgentEvent> events = new ArrayList<>();
        newLoopService(modelClient).run(context, events::add);

        List<AgentMessage> messages = context.session().messages();
        assertEquals(6, messages.size());
        assertEquals(new AgentMessage.AssistantMessage("查询中"), messages.get(0));
        assertInstanceOf(AgentMessage.ToolCallMessage.class, messages.get(1));
        assertInstanceOf(AgentMessage.ToolCallMessage.class, messages.get(2));
        assertInstanceOf(AgentMessage.ToolResultMessage.class, messages.get(3));
        assertInstanceOf(AgentMessage.ToolResultMessage.class, messages.get(4));
        assertEquals(new AgentMessage.AssistantMessage("完成"), messages.get(5));

        assertEquals("call_1", ((AgentMessage.ToolCallMessage) messages.get(1)).callId());
        assertEquals("call_2", ((AgentMessage.ToolCallMessage) messages.get(2)).callId());
        assertEquals("call_1", ((AgentMessage.ToolResultMessage) messages.get(3)).callId());
        assertEquals("call_2", ((AgentMessage.ToolResultMessage) messages.get(4)).callId());
        assertEquals("queryOrder-ok", ((AgentMessage.ToolResultMessage) messages.get(3)).result());
        assertEquals("queryUser-ok", ((AgentMessage.ToolResultMessage) messages.get(4)).result());

        assertInstanceOf(AgentStartEvent.class, events.get(0));
        assertEquals(new TextDeltaEvent("查询中"), events.get(1));
        assertEquals(new MessageEndEvent("查询中"), events.get(2));
        assertInstanceOf(ToolExecutionStartEvent.class, events.get(3));
        assertInstanceOf(ToolExecutionEndEvent.class, events.get(4));
        assertInstanceOf(ToolExecutionStartEvent.class, events.get(5));
        assertInstanceOf(ToolExecutionEndEvent.class, events.get(6));
        assertEquals(new TextDeltaEvent("完成"), events.get(7));
        assertEquals(new MessageEndEvent("完成"), events.get(8));
        assertInstanceOf(AgentEndEvent.class, events.get(9));
    }

    @Test
    void beforeModel_runsBeforeContextBuild() {
        List<String> order = new ArrayList<>();
        ModelHookService modelHooks = new ModelHookService(List.of(new ModelHook() {
            @Override
            public void beforeModel(AgentRunContext context) {
                order.add("beforeModel");
                context.runtime().inject("from-hook");
            }
        }));

        ModelClient modelClient = (request, sink) -> {
            order.add("stream");
            assertTrue(request.context().systemSections().contains("from-hook"));
        };

        AgentLoopService loopService = new AgentLoopService(
                new ModelTurnService(modelHooks, new ContextBuilder(), modelClient),
                new ToolExecutionService(new ToolHookService(List.of())));

        loopService.run(newContext(), event -> {
        });

        assertEquals(List.of("beforeModel", "stream"), order);
    }

    private static AgentLoopService newLoopService(ModelClient modelClient) {
        return new AgentLoopService(
                new ModelTurnService(new ModelHookService(List.of()), new ContextBuilder(), modelClient),
                new ToolExecutionService(new ToolHookService(List.of())));
    }

    private static AgentRunContext newContext(Tool... tools) {
        RuntimeContext runtime = new RuntimeContext();
        for (Tool tool : tools) {
            runtime.addTool(tool);
        }
        return new AgentRunContext(new AgentSession("S001", "U001"), runtime);
    }

    private static Tool echoTool(String name) {
        return new FunctionalTool(
                name,
                name,
                "{\"type\":\"object\"}",
                call -> ToolResult.ok(call.callId(), name + "-ok"));
    }
}
