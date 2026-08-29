package org.example.agent.application.loop;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.example.agent.application.event.AgentEvent.MessageEndEvent;
import org.example.agent.application.event.AgentEventSink;
import org.example.agent.application.hook.ModelHookService;
import org.example.agent.application.llm.ContextBuilder;
import org.example.agent.application.llm.ModelClient;
import org.example.agent.application.llm.ModelRequest;
import org.example.agent.application.runtime.AgentRunContext;
import org.example.agent.domain.session.message.AgentMessage;
import org.example.agent.domain.tool.ToolCall;

/**
 * 一次模型 Turn 的流程编排。
 * <p>
 * 负责：
 * <ol>
 *   <li>{@code beforeModel} → 组装 {@link ModelRequest}</li>
 *   <li>阻塞消费 {@link ModelClient} 流（事件收集委托给 {@link TurnEventCollector}）</li>
 *   <li>将本轮 Assistant / ToolCall 写入 Session → {@code afterModel}</li>
 *   <li>产出 {@link TurnResult} 供 Agent Loop 决定是否继续执行 Tool</li>
 * </ol>
 * 不负责 Tool 执行与 Agent 级生命周期。
 */
@ApplicationScoped
public class ModelTurnService {

    private final ModelHookService hooks;
    private final ContextBuilder contextBuilder;
    private final ModelClient modelClient;

    @Inject
    public ModelTurnService(
            ModelHookService hooks,
            ContextBuilder contextBuilder,
            ModelClient modelClient) {
        this.hooks = hooks;
        this.contextBuilder = contextBuilder;
        this.modelClient = modelClient;
    }

    /**
     * 执行一轮模型调用，阻塞直到本轮流结束。
     *
     * @param context 本轮运行上下文（Session History + Runtime）
     * @param sink    Agent 事件输出；用于推送 TextDelta / MessageEnd
     * @return 本轮聚合结果（完整文本 + ToolCall 列表）
     */
    public TurnResult run(AgentRunContext context, AgentEventSink sink) {
        hooks.beforeModel(context);

        ModelRequest request = contextBuilder.build(context);

        TurnEventCollector collector = new TurnEventCollector(sink);
        modelClient.stream(request, collector);

        TurnResult result = collector.result();

        persistTurnToSession(context, result);

        hooks.afterModel(context);

        if (!result.assistantText().isEmpty()) {
            sink.emit(new MessageEndEvent(result.assistantText()));
        }

        return result;
    }

    /**
     * 将本轮最终消息写入 Session。
     * <p>
     * 顺序固定为：AssistantText（若有）→ 全部 ToolCall；
     * ToolResult 由后续 Tool 执行层写入。
     */
    private void persistTurnToSession(AgentRunContext context, TurnResult result) {
        if (!result.assistantText().isEmpty()) {
            context.session().addMessage(
                    new AgentMessage.AssistantMessage(result.assistantText()));
        }
        for (ToolCall toolCall : result.toolCalls()) {
            context.session().addMessage(new AgentMessage.ToolCallMessage(
                    toolCall.callId(),
                    toolCall.toolName(),
                    toolCall.arguments()));
        }
    }
}
