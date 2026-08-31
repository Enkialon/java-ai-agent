package org.example.agent.application.session;

import org.example.agent.domain.session.AgentSession;
import org.example.agent.domain.session.message.AgentMessage;

/**
 * 从会话历史推导列表展示标题。
 */
public final class SessionTitles {

    private static final int MAX_TITLE_LENGTH = 48;
    private static final String DEFAULT_TITLE = "新会话";

    private SessionTitles() {
    }

    public static String derive(AgentSession session) {
        for (AgentMessage message : session.messages()) {
            if (message instanceof AgentMessage.UserMessage user) {
                String content = user.content().trim();
                if (!content.isEmpty()) {
                    return content.length() <= MAX_TITLE_LENGTH
                            ? content
                            : content.substring(0, MAX_TITLE_LENGTH);
                }
            }
        }
        return DEFAULT_TITLE;
    }
}
