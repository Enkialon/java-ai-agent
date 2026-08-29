package org.example.agent.application.workspace;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
