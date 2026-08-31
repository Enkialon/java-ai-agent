package org.example.agent.application.workspace;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalFilesystemBrowserTest {

    @TempDir
    Path tempDir;

    @Test
    void list_returnsChildDirectoriesOnly() throws Exception {
        Files.createDirectory(tempDir.resolve("alpha"));
        Files.createDirectory(tempDir.resolve("beta"));
        Files.writeString(tempDir.resolve("file.txt"), "x");
        Files.createDirectory(tempDir.resolve(".hidden"));

        LocalFilesystemBrowser browser = new LocalFilesystemBrowser();
        LocalFilesystemBrowser.DirectoryListing listing = browser.list(tempDir.toString());

        assertEquals(tempDir.toAbsolutePath().normalize().toString(), listing.path());
        assertEquals(2, listing.entries().size());
        assertEquals("alpha", listing.entries().get(0).name());
        assertEquals("beta", listing.entries().get(1).name());
        assertTrue(listing.parent() != null);
    }

    @Test
    void list_rootsPath_returnsFilesystemRoots() {
        LocalFilesystemBrowser browser = new LocalFilesystemBrowser();
        LocalFilesystemBrowser.DirectoryListing listing = browser.list(LocalFilesystemBrowser.ROOTS_PATH);

        assertEquals(LocalFilesystemBrowser.ROOTS_PATH, listing.path());
        assertNull(listing.parent());
        assertFalse(listing.entries().isEmpty());

        long expected = StreamSupport.stream(
                        FileSystems.getDefault().getRootDirectories().spliterator(), false)
                .filter(root -> Files.isDirectory(root.toAbsolutePath().normalize()))
                .count();
        assertEquals(expected, listing.entries().size());
        for (LocalFilesystemBrowser.DirEntry entry : listing.entries()) {
            assertNotNull(entry.name());
            assertTrue(Files.isDirectory(Path.of(entry.path())));
        }
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void list_driveRoot_parentIsRootsWhenMultipleDrives() {
        long roots = StreamSupport.stream(
                FileSystems.getDefault().getRootDirectories().spliterator(), false)
                .count();
        if (roots <= 1) {
            return;
        }

        Path driveRoot = FileSystems.getDefault().getRootDirectories().iterator().next();
        LocalFilesystemBrowser browser = new LocalFilesystemBrowser();
        LocalFilesystemBrowser.DirectoryListing listing = browser.list(driveRoot.toString());

        assertEquals(LocalFilesystemBrowser.ROOTS_PATH, listing.parent());
    }
}
