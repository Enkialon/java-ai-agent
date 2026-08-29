package org.example.agent.domain.permission;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PermissionModeTest {

    @Test
    void parse_acceptsAllowDenyAsk_caseInsensitive() {
        assertEquals(PermissionMode.ALLOW, PermissionMode.parse("allow"));
        assertEquals(PermissionMode.DENY, PermissionMode.parse("DENY"));
        assertEquals(PermissionMode.ASK, PermissionMode.parse(" Ask "));
    }

    @Test
    void parse_blankDefaultsToAllow() {
        assertEquals(PermissionMode.ALLOW, PermissionMode.parse(null));
        assertEquals(PermissionMode.ALLOW, PermissionMode.parse("  "));
    }

    @Test
    void parse_unknown_throws() {
        assertThrows(IllegalArgumentException.class, () -> PermissionMode.parse("maybe"));
    }
}
