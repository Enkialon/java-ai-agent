package org.example.agent.infrastructure.sandbox;

import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 启动 {@link ProcessBuilder}，异步有界消费 stdout，并在主线程等待超时。
 */
@ApplicationScoped
public class ProcessRunner {

    public CommandResult run(
            ProcessBuilder builder,
            long timeoutSeconds,
            int maxOutputChars) throws IOException, InterruptedException {
        Objects.requireNonNull(builder, "builder must not be null");
        if (timeoutSeconds <= 0) {
            throw new IllegalArgumentException("timeoutSeconds must be > 0");
        }
        if (maxOutputChars <= 0) {
            throw new IllegalArgumentException("maxOutputChars must be > 0");
        }

        Process process = builder.start();
        BoundedStreamReader reader = new BoundedStreamReader(process.getInputStream(), maxOutputChars);
        Thread readerThread = Thread.ofVirtual().name("process-stdout").start(reader);

        try {
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                joinQuietly(readerThread);
                throwIfReaderFailed(reader);
                return CommandResult.timedOut(reader.output(), reader.truncated());
            }

            joinQuietly(readerThread);
            throwIfReaderFailed(reader);
            return CommandResult.completed(process.exitValue(), reader.output(), reader.truncated());
        } catch (InterruptedException e) {
            process.destroyForcibly();
            joinQuietly(readerThread);
            Thread.currentThread().interrupt();
            throw e;
        } catch (RuntimeException e) {
            process.destroyForcibly();
            joinQuietly(readerThread);
            throw e;
        }
    }

    private static void throwIfReaderFailed(BoundedStreamReader reader) throws IOException {
        Exception failure = reader.failure();
        if (failure == null) {
            return;
        }
        if (failure instanceof IOException io) {
            throw io;
        }
        throw new IOException("failed reading process output", failure);
    }

    private static void joinQuietly(Thread thread) {
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
