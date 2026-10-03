package com.satvik.grader.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Runs an external process with stdin, captures stdout/stderr concurrently (no pipe dead-locks),
 * enforces a timeout and supports cancellation from another thread.
 * <p>
 * One instance can run many processes sequentially. Once {@link #cancel()} is called the runner
 * stays cancelled and every subsequent {@link #run} returns immediately.
 */
public final class ProcessRunner {

    /** Hard cap on captured output per stream, protects against infinite-print loops. */
    public static final int MAX_CAPTURE_BYTES = 32 * 1024 * 1024;

    private static final ExecutorService IO = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "process-io");
        t.setDaemon(true);
        return t;
    });

    public record Result(int exitCode, String stdout, String stderr, long elapsedMillis,
                         boolean timedOut, boolean cancelled, boolean truncated) {
        public boolean ok() {
            return !timedOut && !cancelled && exitCode == 0;
        }
    }

    private record Capture(String text, boolean truncated) {
    }

    private volatile boolean cancelled;
    private volatile Process current;

    public Result run(List<String> command, Path workDir, String stdin, long timeoutMillis) throws IOException {
        if (cancelled) {
            return new Result(-1, "", "", 0, false, true, false);
        }
        ProcessBuilder pb = new ProcessBuilder(command);
        if (workDir != null) {
            pb.directory(workDir.toFile());
        }
        long start = System.nanoTime();
        Process p = pb.start();
        current = p;
        if (cancelled) {
            kill(p);
        }

        Future<Capture> out = IO.submit(() -> capture(p.getInputStream()));
        Future<Capture> err = IO.submit(() -> capture(p.getErrorStream()));
        IO.submit(() -> {
            try (OutputStream os = p.getOutputStream()) {
                if (stdin != null && !stdin.isEmpty()) {
                    os.write(stdin.getBytes(StandardCharsets.UTF_8));
                }
            } catch (IOException ignored) {
                // process exited before reading all input - that's fine
            }
        });

        boolean finished;
        try {
            if (timeoutMillis > 0) {
                finished = p.waitFor(timeoutMillis, TimeUnit.MILLISECONDS);
            } else {
                p.waitFor();
                finished = true;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            finished = false;
        }
        long elapsed = (System.nanoTime() - start) / 1_000_000L;
        boolean wasCancelled = cancelled;
        boolean timedOut = !finished && !wasCancelled;
        if (!finished) {
            kill(p);
        }
        current = null;

        Capture o = await(out);
        Capture e = await(err);
        int code;
        try {
            code = p.exitValue();
        } catch (IllegalThreadStateException ex) {
            code = -1;
        }
        return new Result(code, o.text(), e.text(), elapsed, timedOut, cancelled, o.truncated() || e.truncated());
    }

    /** Kills the running process (if any) and makes all future runs return "cancelled". */
    public void cancel() {
        cancelled = true;
        Process p = current;
        if (p != null) {
            IO.submit(() -> kill(p));
        }
    }

    public boolean isCancelled() {
        return cancelled;
    }

    private static void kill(Process p) {
        try {
            p.descendants().forEach(ProcessHandle::destroyForcibly);
        } catch (Exception ignored) {
            // best effort
        }
        p.destroyForcibly();
        try {
            p.waitFor(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static Capture await(Future<Capture> f) {
        try {
            return f.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            f.cancel(true);
            return new Capture("", false);
        }
    }

    private static Capture capture(InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] chunk = new byte[16 * 1024];
        boolean truncated = false;
        try (in) {
            int n;
            while ((n = in.read(chunk)) != -1) {
                int room = MAX_CAPTURE_BYTES - buf.size();
                if (room > 0) {
                    buf.write(chunk, 0, Math.min(n, room));
                }
                if (n > room) {
                    truncated = true;
                }
            }
        }
        return new Capture(buf.toString(StandardCharsets.UTF_8), truncated);
    }
}
