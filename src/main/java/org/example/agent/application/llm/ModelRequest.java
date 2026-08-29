package org.example.agent.application.llm;

import java.util.Objects;

/**
 * 发给模型的一次请求。
 */
public record ModelRequest(LlmContext context) {

    public ModelRequest {
        Objects.requireNonNull(context, "context must not be null");
    }
}
