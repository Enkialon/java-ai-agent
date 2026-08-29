package org.example.agent.infrastructure.tool.builtin;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.example.agent.domain.tool.Tool;
import org.example.agent.domain.tool.ToolCall;
import org.example.agent.domain.tool.ToolResult;
import org.example.agent.infrastructure.tool.ToolArguments;
import org.example.agent.infrastructure.tool.Workspace;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * 在工作区根目录执行 shell 命令。
 * <p>
 * {@link Workspace} 仅设定初始 cwd，不构成 OS 级 sandbox；
 * 真正的隔离需依赖容器 / 进程权限，而非解析 command 黑名单。
 */
@ApplicationScoped
public class BashTool implements Tool {

    private static final long DEFAULT_TIMEOUT_SECONDS = 30;
    private static final int DEFAULT_MAX_OUTPUT_CHARS = 100_000;

    private static final String INPUT_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "command": {
                  "type": "string",
                  "description": "Shell command to run with workspace as the initial cwd (not a sandbox)"
                }
              },
              "required": ["command"]
            }
            """;

    private final Workspace workspace;
    private final long timeoutSeconds;
    private final int maxOutputChars;

    @Inject
    public BashTool(Workspace workspace) {
        this(workspace, DEFAULT_TIMEOUT_SECONDS, DEFAULT_MAX_OUTPUT_CHARS);
    }

    BashTool(Workspace workspace, long timeoutSeconds, int maxOutputChars) {
        this.workspace = workspace;
        this.timeoutSeconds = timeoutSeconds;
        this.maxOutputChars = maxOutputChars;
    }

    @Override
    public String name() {
        return "bash";
    }

    @Override
    public String description() {
        return "Run a bash command with the workspace as the initial working directory "
                + "(not a filesystem sandbox). Returns stdout/stderr; times out after "
                + timeoutSeconds + "s; output capped at " + maxOutputChars + " chars.";
    }

    @Override
    public String inputSchema() {
        return INPUT_SCHEMA;
    }

    @Override
    public ToolResult execute(ToolCall call) {
        Process process = null;
        Thread readerThread = null;
        BoundedStreamReader reader = null;
        try {
            ToolArguments args = ToolArguments.parse(call.arguments());
            String command = args.requireString("command");
            if (command.isBlank()) {
                return ToolResult.error(call.callId(), "command must not be blank");
            }

            ProcessBuilder builder = new ProcessBuilder("bash", "-lc", command);
            builder.directory(workspace.root().toFile());
            builder.redirectErrorStream(true);

            process = builder.start();
            reader = new BoundedStreamReader(process.getInputStream(), maxOutputChars);
            readerThread = Thread.ofVirtual().name("bash-stdout").start(reader);

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                joinQuietly(readerThread);
                String partial = reader.output();
                String suffix = partial.isEmpty() ? "" : "\nPartial output:\n" + partial;
                return ToolResult.error(
                        call.callId(),
                        "command timed out after " + timeoutSeconds + "s" + suffix);
            }

            joinQuietly(readerThread);
            if (reader.failure() != null) {
                return ToolResult.error(call.callId(), "failed reading output: " + reader.failure().getMessage());
            }

            int exit = process.exitValue();
            String body = reader.output();
            if (body.isEmpty()) {
                body = "(no output)";
            } else if (reader.truncated()) {
                body = body + "\n...[truncated at " + maxOutputChars + " chars]";
            }
            return ToolResult.ok(call.callId(), "exit_code=" + exit + "\n" + body);
        } catch (Exception e) {
            if (process != null) {
                process.destroyForcibly();
            }
            joinQuietly(readerThread);
            return ToolResult.error(call.callId(), e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }

    private static void joinQuietly(Thread thread) {
        if (thread == null) {
            return;
        }
        try {
            thread.join(TimeUnit.SECONDS.toMillis(5));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 并发消费 stdout：最多保留 {@code maxChars}，超出后继续排空管道以免阻塞子进程。
     */
    static final class BoundedStreamReader implements Runnable {

        private final InputStream in;
        private final int maxChars;
        private final StringBuilder buffer = new StringBuilder();
        private volatile boolean truncated;
        private volatile Exception failure;

        BoundedStreamReader(InputStream in, int maxChars) {
            this.in = in;
            this.maxChars = maxChars;
        }

        @Override
        public void run() {
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                char[] chunk = new char[8192];
                int n;
                while ((n = reader.read(chunk)) >= 0) {
                    if (n == 0) {
                        continue;
                    }
                    int room = maxChars - buffer.length();
                    if (room > 0) {
                        int toAppend = Math.min(n, room);
                        buffer.append(chunk, 0, toAppend);
                        if (toAppend < n) {
                            truncated = true;
                        }
                    } else {
                        truncated = true;
                    }
                    // 超出上限后仍继续读，避免管道填满导致子进程阻塞
                }
            } catch (IOException e) {
                failure = e;
            }
        }

        String output() {
            return buffer.toString();
        }

        boolean truncated() {
            return truncated;
        }

        Exception failure() {
            return failure;
        }
    }
}
