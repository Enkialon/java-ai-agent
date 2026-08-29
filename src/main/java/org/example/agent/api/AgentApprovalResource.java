package org.example.agent.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.example.agent.application.permission.ApprovalBindingService;
import org.example.agent.application.permission.PendingApproval;
import org.example.agent.domain.permission.ApprovalNotFoundException;

/**
 * 人机审批：对当前 Session 下挂起的工具调用做 approve / deny。
 */
@Path("/api/agent/session/approvals/{callId}")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AgentApprovalResource {

    @Inject
    ApprovalBindingService approvalBindingService;

    @GET
    public Response current(@PathParam("callId") String callId) {
        try {
            return Response.ok(toResponse(approvalBindingService.current(callId))).build();
        } catch (ApprovalNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        }
    }

    @POST
    @Path("/approve")
    public Response approve(@PathParam("callId") String callId) {
        try {
            return Response.ok(toResponse(approvalBindingService.approve(callId))).build();
        } catch (ApprovalNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        }
    }

    @POST
    @Path("/deny")
    public Response deny(@PathParam("callId") String callId) {
        try {
            return Response.ok(toResponse(approvalBindingService.deny(callId))).build();
        } catch (ApprovalNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        }
    }

    private static ApprovalResponse toResponse(PendingApproval pending) {
        return new ApprovalResponse(
                pending.callId(),
                pending.toolName(),
                pending.arguments(),
                pending.permission());
    }

    public record ApprovalResponse(
            String callId,
            String toolName,
            String arguments,
            String permission
    ) {
    }
}
