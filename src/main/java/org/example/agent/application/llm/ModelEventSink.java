package org.example.agent.application.llm;

/**
 * 模型事件输出通道。
 */
@FunctionalInterface
public interface ModelEventSink {

    void emit(ModelEvent event);
}
