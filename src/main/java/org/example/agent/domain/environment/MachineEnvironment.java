package org.example.agent.domain.environment;

import java.util.Locale;
import java.util.Objects;

/**
 * 当前宿主机机器环境快照（OS / 架构 / 路径风格等）。
 * <p>
 * 作为 Runtime Context 的一部分，由 Hook 在调用模型前写入 System Context，
 * 供模型选择与执行环境匹配的命令与路径写法。
 */
public final class MachineEnvironment {

    public enum OsFamily {
        WINDOWS,
        MACOS,
        LINUX,
        OTHER
    }

    private final String osName;
    private final String osVersion;
    private final String osArch;
    private final String fileSeparator;
    private final String lineSeparator;
    private final String userHome;
    private final String userName;
    private final String javaVersion;

    public MachineEnvironment(
            String osName,
            String osVersion,
            String osArch,
            String fileSeparator,
            String lineSeparator,
            String userHome,
            String userName,
            String javaVersion) {
        this.osName = Objects.requireNonNull(osName, "osName must not be null");
        this.osVersion = Objects.requireNonNull(osVersion, "osVersion must not be null");
        this.osArch = Objects.requireNonNull(osArch, "osArch must not be null");
        this.fileSeparator = Objects.requireNonNull(fileSeparator, "fileSeparator must not be null");
        this.lineSeparator = Objects.requireNonNull(lineSeparator, "lineSeparator must not be null");
        this.userHome = Objects.requireNonNull(userHome, "userHome must not be null");
        this.userName = Objects.requireNonNull(userName, "userName must not be null");
        this.javaVersion = Objects.requireNonNull(javaVersion, "javaVersion must not be null");
    }

    public String osName() {
        return osName;
    }

    public String osVersion() {
        return osVersion;
    }

    public String osArch() {
        return osArch;
    }

    public String fileSeparator() {
        return fileSeparator;
    }

    public String lineSeparator() {
        return lineSeparator;
    }

    public String userHome() {
        return userHome;
    }

    public String userName() {
        return userName;
    }

    public String javaVersion() {
        return javaVersion;
    }

    public OsFamily osFamily() {
        String normalized = osName.toLowerCase(Locale.ROOT);
        if (normalized.contains("win")) {
            return OsFamily.WINDOWS;
        }
        if (normalized.contains("mac") || normalized.contains("darwin")) {
            return OsFamily.MACOS;
        }
        if (normalized.contains("linux") || normalized.contains("nux") || normalized.contains("aix")) {
            return OsFamily.LINUX;
        }
        return OsFamily.OTHER;
    }
}
