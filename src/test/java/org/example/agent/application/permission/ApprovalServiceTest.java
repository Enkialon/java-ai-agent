package org.example.agent.application.permission;

import org.example.agent.domain.permission.ApprovalDecision;
import org.example.agent.domain.permission.ApprovalNotFoundException;
import org.example.agent.domain.tool.ToolCall;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApprovalServiceTest {

    @Test
    void await_completesWhenApproved() throws Exception {
        ApprovalService service = new ApprovalService(Duration.ofSeconds(5));
        ToolCall call = new ToolCall("c1", "bash", "{\"command\":\"ls\"}");
        CountDownLatch waiting = new CountDownLatch(1);
        AtomicReference<ApprovalDecision> decision = new AtomicReference<>();

        Thread waiter = Thread.startVirtualThread(() -> {
            waiting.countDown();
            decision.set(service.await("S1", call, "bash"));
        });

        assertTrue(waiting.await(2, TimeUnit.SECONDS));
        awaitPending(service, "S1", "c1");
        service.approve("S1", "c1");
        waiter.join(Duration.ofSeconds(2));

        assertEquals(ApprovalDecision.APPROVED, decision.get());
    }

    @Test
    void await_timeout_isDenied() {
        ApprovalService service = new ApprovalService(Duration.ofMillis(50));
        ToolCall call = new ToolCall("c2", "bash", "{}");

        ApprovalDecision decision = service.await("S1", call, "bash");

        assertEquals(ApprovalDecision.DENIED, decision);
        assertThrows(ApprovalNotFoundException.class, () -> service.approve("S1", "c2"));
    }

    @Test
    void approve_unknown_throws() {
        ApprovalService service = new ApprovalService();
        assertThrows(ApprovalNotFoundException.class, () -> service.approve("S1", "missing"));
    }

    private static void awaitPending(ApprovalService service, String sessionId, String callId)
            throws InterruptedException {
        for (int i = 0; i < 50; i++) {
            if (service.find(sessionId, callId).isPresent()) {
                return;
            }
            Thread.sleep(20);
        }
        throw new AssertionError("pending approval not registered");
    }
}
