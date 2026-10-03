package com.satvik.grader.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/** Auto-deleting temporary directory (Java counterpart of Python's {@code tempfile.TemporaryDirectory}). */
public final class TempDir implements AutoCloseable {

    private final Path path;

    private TempDir(Path path) {
        this.path = path;
    }

    public static TempDir create(String prefix) throws IOException {
        return new TempDir(Files.createTempDirectory(prefix));
    }

    public Path path() {
        return path;
    }

    @Override
    public void close() {
        // Executables can stay locked for a moment on Windows after being killed - retry briefly.
        for (int attempt = 0; attempt < 3; attempt++) {
            if (deleteTree()) {
                return;
            }
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private boolean deleteTree() {
        if (!Files.exists(path)) {
            return true;
        }
        boolean[] ok = {true};
        try (Stream<Path> walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException e) {
                    ok[0] = false;
                }
            });
        } catch (IOException e) {
            return false;
        }
        return ok[0];
    }
}
