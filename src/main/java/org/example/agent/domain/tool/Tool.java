package org.example.agent.domain.tool;

/**
 * 可执行 Tool：描述与能力绑定在同一对象上。
 * <p>
 * 注册进 Runtime 的必须是本接口实现；发给模型的 schema 由其投影为 {@link ToolSpec}。
 */
public interface Tool {

    String name();

    String description();

    /**
     * 参数 JSON Schema。
     */
    String inputSchema();

    ToolResult execute(ToolCall call);

    default ToolSpec spec() {
        return new ToolSpec(name(), description(), inputSchema());
    }
}
