package org.example.agent.application.llm;

import org.example.agent.application.runtime.AgentRunContext;
import org.example.agent.application.runtime.RuntimeContext;
import org.example.agent.application.tool.FunctionalTool;
import org.example.agent.domain.session.AgentSession;
import org.example.agent.domain.session.message.AgentMessage.UserMessage;
import org.example.agent.domain.skill.SkillDescriptor;
import org.example.agent.domain.tool.Tool;
import org.example.agent.domain.tool.ToolResult;
import org.example.agent.domain.tool.ToolSpec;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContextBuilderTest {

    private final ContextBuilder builder = new ContextBuilder();

    @Test
    void build_combinesRuntimeContextAndSessionHistory() {
        SkillDescriptor skill = new SkillDescriptor(
                "wechat-chat",
                "查询微信聊天记录",
                ".agents/skills/wechat-chat/SKILL.md");
        Tool tool = new FunctionalTool(
                "queryOrder",
                "查询订单",
                "{\"type\":\"object\",\"properties\":{\"orderId\":{\"type\":\"string\"}}}",
                call -> new ToolResult(call.callId(), ""));

        RuntimeContext runtime = new RuntimeContext()
                .systemPrompt("You are a helpful agent.")
                .agentsMd("# AGENTS.md")
                .addSkill(skill)
                .addTool(tool)
                .inject("hook: cwd=/tmp")
                .environmentInfo("os=linux");

        AgentSession session = new AgentSession("S001", "U001");
        session.addMessage(new UserMessage("帮我查看当前目录"));

        LlmContext llmContext = builder.build(new AgentRunContext(session, runtime)).context();

        assertEquals(4, llmContext.systemSections().size());
        assertEquals("You are a helpful agent.", llmContext.systemSections().get(0));
        assertEquals("# AGENTS.md", llmContext.systemSections().get(1));
        assertEquals("hook: cwd=/tmp", llmContext.systemSections().get(2));
        assertEquals("os=linux", llmContext.systemSections().get(3));

        assertEquals(List.of(skill), llmContext.skills());
        assertEquals(List.of(tool.spec()), llmContext.tools());
        assertEquals(
                new ToolSpec(
                        "queryOrder",
                        "查询订单",
                        "{\"type\":\"object\",\"properties\":{\"orderId\":{\"type\":\"string\"}}}"),
                llmContext.tools().get(0));

        assertEquals(1, llmContext.history().size());
        assertTrue(llmContext.history().get(0) instanceof UserMessage);
        assertEquals("帮我查看当前目录", ((UserMessage) llmContext.history().get(0)).content());
    }

    @Test
    void build_skipsBlankRuntimeSections() {
        RuntimeContext runtime = new RuntimeContext()
                .systemPrompt(" ")
                .agentsMd(null);

        AgentSession session = new AgentSession("S001", "U001");

        LlmContext llmContext = builder.build(runtime, session).context();

        assertTrue(llmContext.systemSections().isEmpty());
        assertTrue(llmContext.skills().isEmpty());
        assertTrue(llmContext.tools().isEmpty());
        assertTrue(llmContext.history().isEmpty());
    }
}
