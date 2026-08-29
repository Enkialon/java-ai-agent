package org.example.agent.domain.tool;

import java.util.Objects;

/**
 * Tool 的只读 schema 视图，供 LLM Context / Tool Calling 使用。
 * <p>
 * 权威来源是可执行的 {@link Tool}，本类型仅作投影，不单独注册。
 */
public record ToolSpec(
        String name,
        String description,
        String inputSchema
) {

    public ToolSpec {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(description, "description must not be null");
        Objects.requireNonNull(inputSchema, "inputSchema must not be null");
    }
}
