package org.example.agent.infrastructure.llm;

import org.junit.jupiter.api.Test;

import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OpenAiCompatibleModelClientTest {

    @Test
    void mergeToolCall_accumulatesFragmentsByIndex() {
        TreeMap<Long, OpenAiCompatibleModelClient.PendingToolCall> pending = new TreeMap<>();

        // Simulate first fragment: id + name + partial args
        OpenAiCompatibleModelClient.PendingToolCall first = pending.computeIfAbsent(0L, i ->
                new OpenAiCompatibleModelClient.PendingToolCall());
        first.id = "call_1";
        first.name = "bash";
        first.arguments.append("{\"command\":");

        OpenAiCompatibleModelClient.PendingToolCall second = pending.computeIfAbsent(0L, i ->
                new OpenAiCompatibleModelClient.PendingToolCall());
        second.arguments.append("\"ls\"}");

        assertEquals(1, pending.size());
        assertEquals("call_1", pending.get(0L).id);
        assertEquals("bash", pending.get(0L).name);
        assertEquals("{\"command\":\"ls\"}", pending.get(0L).arguments.toString());
        assertNull(pending.get(1L));
    }
}
