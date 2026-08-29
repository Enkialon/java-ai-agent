package org.example.agent.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.example.agent.application.workspace.WorkspaceBindingService;

/**
 * Session 工作区绑定：前端选定本机目录后写入当前 Session。
 */
@Path("/api/agent/session/workspace")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AgentWorkspaceResource {

    @Inject
    WorkspaceBindingService workspaceBindingService;

    @PUT
    public WorkspaceResponse bind(BindWorkspaceRequest request) {
        String path = workspaceBindingService.bind(request.path());
        return new WorkspaceResponse(path);
    }

    @GET
    public Response current() {
        return workspaceBindingService.current()
                .<Response>map(path -> Response.ok(new WorkspaceResponse(path)).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }

    public record BindWorkspaceRequest(String path) {
    }

    public record WorkspaceResponse(String path) {
    }
}
