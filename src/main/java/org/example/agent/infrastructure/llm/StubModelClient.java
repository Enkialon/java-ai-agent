package org.example.agent.infrastructure.llm;

import org.example.agent.application.llm.ModelClient;
import org.example.agent.application.llm.ModelEventSink;
import org.example.agent.application.llm.ModelRequest;

/**
 * 占位模型客户端：暂不产生任何模型事件。
 */
public class StubModelClient implements ModelClient {

    @Override
    public void stream(ModelRequest request, ModelEventSink sink) {
        // no-op
    }
}
