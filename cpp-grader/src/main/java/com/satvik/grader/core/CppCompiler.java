package com.satvik.grader.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Wraps {@code g++} (or any compatible compiler) invocation. */
public final class CppCompiler {

    private CppCompiler() {
    }

    public record Result(boolean success, String diagnostics, long millis, boolean timedOut, boolean cancelled) {
    }

    public static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    public static Path exePath(Path dir, String name) {
        return dir.resolve(isWindows() ? name + ".exe" : name);
    }

    /**
     * Builds {@code compiler source -o exe <flags...>}. Flags go last so that linker options such as
     * {@code -lm} or {@code -Wl,...} behave as expected.
     */
    public static List<String> command(String compiler, Path source, Path exe, List<String> flags) {
        List<String> cmd = new ArrayList<>();
        cmd.add(compiler);
        cmd.add(source.toString());
        cmd.add("-o");
        cmd.add(exe.toString());
        cmd.addAll(flags);
        return cmd;
    }

    public static Result compile(ProcessRunner runner, String compiler, Path source, Path exe,
                                 List<String> flags, long timeoutMillis) {
        try {
            ProcessRunner.Result r = runner.run(command(compiler, source, exe, flags), null, null, timeoutMillis);
            String diag = (r.stderr() + (r.stdout().isBlank() ? "" : "\n" + r.stdout())).strip();
            if (r.timedOut()) {
                return new Result(false, "Compilation timed out after " + (timeoutMillis / 1000) + " s.\n" + diag,
                        r.elapsedMillis(), true, false);
            }
            if (r.cancelled()) {
                return new Result(false, "Compilation cancelled.", r.elapsedMillis(), false, true);
            }
            boolean ok = r.exitCode() == 0 && Files.exists(exe);
            if (!ok && diag.isEmpty()) {
                diag = "Compiler exited with " + ExitCodes.describe(r.exitCode());
            }
            return new Result(ok, diag, r.elapsedMillis(), false, false);
        } catch (IOException e) {
            return new Result(false, "Could not start compiler '" + compiler + "': " + e.getMessage()
                    + "\nMake sure g++ is installed and on your PATH.", 0, false, false);
        }
    }
}
