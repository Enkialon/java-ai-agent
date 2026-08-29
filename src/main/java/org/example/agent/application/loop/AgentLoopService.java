package org.example.agent.application.loop;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.example.agent.application.event.AgentEvent.AgentEndEvent;
import org.example.agent.application.event.AgentEvent.AgentStartEvent;
import org.example.agent.application.event.AgentEventSink;
import org.example.agent.application.runtime.AgentRunContext;
import org.example.agent.application.tool.ToolExecutionService;
import org.example.agent.domain.tool.ToolCall;

/**
 * Agent Loop：普通同步状态机，适合跑在虚拟线程上。
 *
 * <pre>
 * AgentStart
 *   → for turn
 *        → ModelTurn
 *        → 无 Tool → AgentEnd return
 *        → 有 Tool → 逐个执行 → 下一轮
 * </pre>
 */
@ApplicationScoped
public class AgentLoopService {

    private static final int MAX_TURNS = 20;

    private final ModelTurnService modelTurnService;
    private final ToolExecutionService toolExecutionService;

    @Inject
    public AgentLoopService(
            ModelTurnService modelTurnService,
            ToolExecutionService toolExecutionService) {
        this.modelTurnService = modelTurnService;
        this.toolExecutionService = toolExecutionService;
    }

    public AgentRunResult run(AgentRunContext context, AgentEventSink sink) {
        // 告诉用户开始执行任务了
        sink.emit(new AgentStartEvent());

        // 循环执行
        for (int turn = 1; turn <= MAX_TURNS; turn++) {
            // 执行模型
            TurnResult result = modelTurnService.run(context, sink);

            if (!result.hasToolCalls()) {
                AgentRunResult runResult = new AgentRunResult(result.assistantText());
                sink.emit(new AgentEndEvent(runResult));
                return runResult;
            }

            for (ToolCall call : result.toolCalls()) {
                toolExecutionService.execute(context, call, sink);
            }
        }

        throw new IllegalStateException("Agent exceeded max turns: " + MAX_TURNS);
    }
}
