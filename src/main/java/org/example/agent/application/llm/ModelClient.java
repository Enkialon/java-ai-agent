package org.example.agent.application.llm;

/**
 * 模型客户端。
 * <p>
 * 阻塞当前（虚拟）线程直到本轮模型响应结束，过程中通过 sink 推送事件。
 */
public interface ModelClient {

    void stream(ModelRequest request, ModelEventSink sink);
}
