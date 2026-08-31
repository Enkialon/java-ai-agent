package org.example.agent.application.workspace;

import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.StreamSupport;

/**
 * 浏览本机目录树，供 Web UI 选择工作区。
 * <p>
 * Windows 多盘符时，盘符根（如 {@code C:\}）的上级为虚拟「盘符列表」
 * （{@link #ROOTS_PATH}），以便切换到 D: 等其它盘。
 */
@ApplicationScoped
public class LocalFilesystemBrowser {

    /**
     * 虚拟路径：列出本机所有文件系统根（盘符 / 挂载点）。
     */
    public static final String ROOTS_PATH = "__roots__";

    private static final int MAX_ENTRIES = 500;

    public DirectoryListing list(String rawPath) {
        if (isRootsPath(rawPath)) {
            return listRoots();
        }

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

        return new DirectoryListing(
                dir.toAbsolutePath().normalize().toString(),
                resolveParent(dir),
                List.copyOf(entries));
    }

    public String defaultStartPath() {
        return Path.of(System.getProperty("user.home", ".")).toAbsolutePath().normalize().toString();
    }

    private DirectoryListing listRoots() {
        List<DirEntry> entries = new ArrayList<>();
        for (Path root : FileSystems.getDefault().getRootDirectories()) {
            Path absolute = root.toAbsolutePath().normalize();
            if (!Files.isDirectory(absolute)) {
                continue;
            }
            entries.add(new DirEntry(displayRootName(absolute), absolute.toString()));
        }
        entries.sort(Comparator.comparing(DirEntry::name, String.CASE_INSENSITIVE_ORDER));
        return new DirectoryListing(ROOTS_PATH, null, List.copyOf(entries));
    }

    /**
     * 盘符根（parent == null）且本机有多个根时，上级指向虚拟盘符列表。
     */
    private String resolveParent(Path dir) {
        Path parent = dir.getParent();
        if (parent != null) {
            return parent.toAbsolutePath().normalize().toString();
        }
        if (rootCount() > 1) {
            return ROOTS_PATH;
        }
        return null;
    }

    private static long rootCount() {
        return StreamSupport.stream(
                FileSystems.getDefault().getRootDirectories().spliterator(), false)
                .count();
    }

    private static boolean isRootsPath(String rawPath) {
        return rawPath != null && ROOTS_PATH.equals(rawPath.trim());
    }

    private static String displayRootName(Path root) {
        String text = root.toString();
        // Windows: "C:\" → "C:"
        if (text.length() >= 2 && text.charAt(1) == ':') {
            return text.substring(0, 2);
        }
        return text;
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
