package org.example.agent.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.example.agent.application.workspace.LocalFilesystemBrowser;

/**
 * 本机目录浏览：供前端选择 Session 工作区。
 */
@Path("/api/agent/filesystem")
@Produces(MediaType.APPLICATION_JSON)
public class AgentFilesystemResource {

    @Inject
    LocalFilesystemBrowser filesystemBrowser;

    @GET
    public Response list(@QueryParam("path") String path) {
        try {
            return Response.ok(filesystemBrowser.list(path)).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }
}
