package org.example.agent.domain.permission;

/**
 * 工具权限模式：放行 / 拒绝 / 人机审批。
 */
public enum PermissionMode {
    ALLOW,
    DENY,
    ASK;

    public static PermissionMode parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return ALLOW;
        }
        return switch (raw.trim().toLowerCase()) {
            case "allow" -> ALLOW;
            case "deny" -> DENY;
            case "ask" -> ASK;
            default -> throw new IllegalArgumentException(
                    "Unknown permission mode: '" + raw + "' (expected allow|deny|ask)");
        };
    }
}
