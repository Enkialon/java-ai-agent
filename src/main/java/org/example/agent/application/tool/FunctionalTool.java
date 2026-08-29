package org.example.agent.application.tool;

import org.example.agent.domain.tool.Tool;
import org.example.agent.domain.tool.ToolCall;
import org.example.agent.domain.tool.ToolResult;
import org.example.agent.domain.workspace.Workspace;

import java.util.Objects;
import java.util.function.BiFunction;

/**
 * 基于函数的 {@link Tool} 实现，便于测试与简单注册。
 */
public final class FunctionalTool implements Tool {

    private final String name;
    private final String description;
    private final String inputSchema;
    private final BiFunction<ToolCall, Workspace, ToolResult> handler;

    public FunctionalTool(
            String name,
            String description,
            String inputSchema,
            BiFunction<ToolCall, Workspace, ToolResult> handler) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.description = Objects.requireNonNull(description, "description must not be null");
        this.inputSchema = Objects.requireNonNull(inputSchema, "inputSchema must not be null");
        this.handler = Objects.requireNonNull(handler, "handler must not be null");
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public String inputSchema() {
        return inputSchema;
    }

    @Override
    public ToolResult execute(ToolCall call, Workspace workspace) {
        return handler.apply(call, workspace);
    }
}
