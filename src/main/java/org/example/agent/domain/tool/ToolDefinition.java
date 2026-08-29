package org.example.agent.domain.tool;

import java.util.Objects;

/**
 * Tool 结构化定义，供 Tool Calling / Runtime Context 使用。
 */
public record ToolDefinition(
        String name,
        String description,
        /**
         * JSON Schema。
         */
        String inputSchema
) {

    public ToolDefinition {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(description, "description must not be null");
        Objects.requireNonNull(inputSchema, "inputSchema must not be null");
    }
}
