package org.example.agent.application.workspace;

import org.example.agent.application.request.AgentRequestContext;
import org.example.agent.application.session.AgentSessionManager;
import org.example.agent.domain.session.AgentSession;
import org.example.agent.domain.session.WorkspaceNotBoundException;
import org.example.agent.infrastructure.session.InMemoryAgentSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WorkspaceBindingServiceTest {

    @TempDir
    Path tempDir;

    private InMemoryAgentSessionRepository repository;
    private WorkspaceBindingService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryAgentSessionRepository();
        AgentSessionManager sessionManager = new AgentSessionManager(
                repository,
                new AgentRequestContext("S001", "U001"));
        service = new WorkspaceBindingService(sessionManager, new SessionWorkspaceResolver());
    }

    @Test
    void bind_persistsNormalizedPathOnSession() {
        String bound = service.bind(tempDir.toString());

        assertEquals(tempDir.toAbsolutePath().normalize().toString(), bound);
        assertEquals(bound, service.current().orElseThrow());

        AgentSession session = repository.findById("S001").orElseThrow();
        assertEquals(bound, session.workspacePath().orElseThrow());
    }

    @Test
    void bind_rejectsMissingDirectory() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.bind(tempDir.resolve("missing").toString()));
    }

    @Test
    void resolve_requiresBoundWorkspace() {
        SessionWorkspaceResolver resolver = new SessionWorkspaceResolver();
        AgentSession session = new AgentSession("S001", "U001");

        assertThrows(WorkspaceNotBoundException.class, () -> resolver.resolve(session));
    }

    @Test
    void resolve_returnsWorkspace() {
        SessionWorkspaceResolver resolver = new SessionWorkspaceResolver();
        AgentSession session = new AgentSession("S001", "U001");
        session.bindWorkspace(tempDir.toAbsolutePath().normalize().toString());

        assertEquals(
                tempDir.toAbsolutePath().normalize(),
                resolver.resolve(session).root());
    }
}
