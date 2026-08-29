package org.example.agent.infrastructure.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Objects;

/**
 * 解析 ToolCall.arguments JSON。
 */
public final class ToolArguments {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final JsonNode root;

    private ToolArguments(JsonNode root) {
        this.root = root;
    }

    public static ToolArguments parse(String json) {
        Objects.requireNonNull(json, "json must not be null");
        try {
            JsonNode node = MAPPER.readTree(json.isBlank() ? "{}" : json);
            if (node == null || !node.isObject()) {
                throw new IllegalArgumentException("arguments must be a JSON object");
            }
            return new ToolArguments(node);
        } catch (IOException e) {
            throw new IllegalArgumentException("invalid arguments JSON: " + e.getMessage(), e);
        }
    }

    public String requireString(String field) {
        JsonNode value = root.get(field);
        if (value == null || value.isNull() || !value.isTextual()) {
            throw new IllegalArgumentException("missing or invalid string field: " + field);
        }
        return value.asText();
    }

    public String optionalString(String field, String defaultValue) {
        JsonNode value = root.get(field);
        if (value == null || value.isNull()) {
            return defaultValue;
        }
        if (!value.isTextual()) {
            throw new IllegalArgumentException("invalid string field: " + field);
        }
        return value.asText();
    }
}
