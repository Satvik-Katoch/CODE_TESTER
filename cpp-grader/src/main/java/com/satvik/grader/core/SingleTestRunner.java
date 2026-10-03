package com.satvik.grader.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** "Compile &amp; Run" for one test case - the Java port of {@code CppGraderApp.run_test}. */
public final class SingleTestRunner {

    public enum Verdict { ACCEPTED, WRONG_ANSWER, COMPILATION_ERROR, RUNTIME_ERROR, TIME_LIMIT, ERROR }

    public record Request(SourceSpec source, String compiler, List<String> flags, String profileName,
                          String input, String expected, long runTimeoutMillis, long compileTimeoutMillis) {
    }

    public record Result(Verdict verdict, String output, boolean diffAvailable, long runMillis) {
    }

    @FunctionalInterface
    public interface Log {
        void line(String text, Tone tone);
    }

    private final ProcessRunner runner = new ProcessRunner();

    public void cancel() {
        runner.cancel();
    }

    public Result run(Request rq, Log log) {
        log.line("--- Starting Test ---", Tone.INFO);
        try (TempDir tmp = TempDir.create("grader-run-")) {
            Path src;
            SourceSpec source = rq.source();
            if (source.fromFile()) {
                Path f = source.file();
                if (f == null || !Files.isRegularFile(f)) {
                    log.line("Error: File not found.", Tone.ERROR);
                    return new Result(Verdict.ERROR, "", false, 0);
                }
                log.line("Using file: " + f.getFileName(), Tone.INFO);
                src = f;
            } else {
                String code = source.code();
                if (code == null || code.isBlank()) {
                    log.line("Error: Editor empty.", Tone.ERROR);
                    return new Result(Verdict.ERROR, "", false, 0);
                }
                src = tmp.path().resolve("main.cpp");
                Files.writeString(src, code, StandardCharsets.UTF_8);
                log.line("Using editor code.", Tone.INFO);
            }

            log.line("Flags [" + rq.profileName() + "]: " + (rq.flags().isEmpty() ? "(none)" : String.join(" ", rq.flags())),
                    Tone.PLAIN);
            Path exe = CppCompiler.exePath(tmp.path(), "main_executable");
            CppCompiler.Result c = CppCompiler.compile(runner, rq.compiler(), src, exe, rq.flags(), rq.compileTimeoutMillis());
            if (!c.success()) {
                if (c.cancelled()) {
                    log.line("Cancelled.", Tone.WARNING);
                    return new Result(Verdict.ERROR, "", false, 0);
                }
                log.line("Compilation Failed:\n" + c.diagnostics(), Tone.ERROR);
                return new Result(Verdict.COMPILATION_ERROR, "", false, 0);
            }
            log.line(String.format("Compilation successful. (%.2f s)", c.millis() / 1000.0), Tone.INFO);
            if (!c.diagnostics().isBlank()) {
                log.line("Compiler warnings:\n" + c.diagnostics(), Tone.WARNING);
            }

            String input = rq.input() == null ? "" : rq.input();
            if (!input.isEmpty() && !input.endsWith("\n")) {
                input += "\n";
            }
            ProcessRunner.Result r = runner.run(List.of(exe.toString()), null, input, rq.runTimeoutMillis());
            String out = r.stdout();
            if (!r.stderr().isBlank()) {
                log.line("Runtime Warning (stderr):\n" + r.stderr().stripTrailing(), Tone.WARNING);
            }
            if (r.truncated()) {
                log.line("Note: output exceeded " + (ProcessRunner.MAX_CAPTURE_BYTES >> 20) + " MB and was truncated.", Tone.WARNING);
            }
            if (r.cancelled()) {
                log.line("Cancelled.", Tone.WARNING);
                return new Result(Verdict.ERROR, out, true, r.elapsedMillis());
            }
            if (r.timedOut()) {
                log.line(String.format("Time Limit Exceeded: still running after %.1f s, process killed.",
                        rq.runTimeoutMillis() / 1000.0), Tone.ERROR);
                return new Result(Verdict.TIME_LIMIT, out, true, r.elapsedMillis());
            }
            if (r.exitCode() != 0) {
                log.line("Runtime Error: " + ExitCodes.describe(r.exitCode()), Tone.ERROR);
                log.line("Execution time: " + r.elapsedMillis() + " ms", Tone.PLAIN);
                return new Result(Verdict.RUNTIME_ERROR, out, true, r.elapsedMillis());
            }
            log.line("Execution time: " + r.elapsedMillis() + " ms", Tone.INFO);

            if (OutputCompare.same(out, rq.expected())) {
                log.line("\n=========  Result: PASSED  =========", Tone.SUCCESS);
                return new Result(Verdict.ACCEPTED, out, true, r.elapsedMillis());
            }
            log.line("\n=========  Result: FAILED  =========", Tone.FAILURE);
            log.line("Outputs do not match. Click 'Compare Output' for details.", Tone.INFO);
            return new Result(Verdict.WRONG_ANSWER, out, true, r.elapsedMillis());
        } catch (IOException e) {
            log.line("Error: " + e.getMessage(), Tone.ERROR);
            return new Result(Verdict.ERROR, "", false, 0);
        }
    }
}
