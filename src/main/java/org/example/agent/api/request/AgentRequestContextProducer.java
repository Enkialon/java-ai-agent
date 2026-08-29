package org.example.agent.api.request;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.HttpHeaders;
import org.example.agent.application.request.AgentRequestContext;

@ApplicationScoped
public class AgentRequestContextProducer {

    @Inject
    HttpHeaders headers;

    @Produces
    @RequestScoped
    public AgentRequestContext create() {
        return new AgentRequestContext(
                headers.getHeaderString("X-Agent-Session-Id"),
                headers.getHeaderString("X-Agent-User-Id")
        );
    }
}
