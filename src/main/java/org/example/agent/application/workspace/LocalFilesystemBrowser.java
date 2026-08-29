package org.example.agent.application.workspace;

import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 浏览本机目录树，供 Web UI 选择工作区。
 */
@ApplicationScoped
public class LocalFilesystemBrowser {

    private static final int MAX_ENTRIES = 500;

    public DirectoryListing list(String rawPath) {
        Path dir = resolveStart(rawPath);
        if (!Files.isDirectory(dir)) {
            throw new IllegalArgumentException("不是目录: " + dir);
        }
        if (!Files.isReadable(dir)) {
            throw new IllegalArgumentException("目录不可读: " + dir);
        }

        List<DirEntry> entries = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path child : stream) {
                if (entries.size() >= MAX_ENTRIES) {
                    break;
                }
                if (!Files.isDirectory(child) || Files.isSymbolicLink(child)) {
                    continue;
                }
                String name = child.getFileName().toString();
                if (name.startsWith(".")) {
                    continue;
                }
                entries.add(new DirEntry(name, child.toAbsolutePath().normalize().toString()));
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("无法列出目录: " + dir + " — " + e.getMessage(), e);
        }

        entries.sort(Comparator.comparing(DirEntry::name, String.CASE_INSENSITIVE_ORDER));

        Path parent = dir.getParent();
        return new DirectoryListing(
                dir.toAbsolutePath().normalize().toString(),
                parent == null ? null : parent.toAbsolutePath().normalize().toString(),
                List.copyOf(entries));
    }

    public String defaultStartPath() {
        return Path.of(System.getProperty("user.home", ".")).toAbsolutePath().normalize().toString();
    }

    private Path resolveStart(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return Path.of(defaultStartPath());
        }
        return Path.of(rawPath).toAbsolutePath().normalize();
    }

    public record DirectoryListing(String path, String parent, List<DirEntry> entries) {
        public DirectoryListing {
            Objects.requireNonNull(path, "path must not be null");
            entries = entries == null ? List.of() : List.copyOf(entries);
        }
    }

    public record DirEntry(String name, String path) {
        public DirEntry {
            Objects.requireNonNull(name, "name must not be null");
            Objects.requireNonNull(path, "path must not be null");
        }
    }
}
