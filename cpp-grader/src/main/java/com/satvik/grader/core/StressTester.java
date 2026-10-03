package com.satvik.grader.core;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Generator (Java) vs Brute force (C++) vs Optimised (C++) stress loop - the Java port of
 * {@code CppGraderApp.run_stress_test}. Run it on a background thread; callbacks arrive on that thread.
 */
public final class StressTester implements Runnable {

    public record Config(Path generator, Path brute, SourceSpec main, String compiler, List<String> flags,
                         int iterations, long timeoutMillis) {
    }

    public enum Kind {
        PASSED, MISMATCH, STOPPED,
        GEN_COMPILE_ERROR, BRUTE_COMPILE_ERROR, MAIN_COMPILE_ERROR,
        GEN_FAILED, BRUTE_FAILED, MAIN_FAILED,
        SETUP_ERROR
    }

    /** Final result. {@code input/expected/actual} fill the three result panes. */
    public record Outcome(Kind kind, int iteration, String input, String expected, String actual, String message) {
        public boolean hasCounterExample() {
            return kind == Kind.MISMATCH || kind == Kind.MAIN_FAILED;
        }
    }

    public interface Listener {
        void onStatus(String text, Tone tone);

        void onProgress(int done, int total);

        void onFinished(Outcome outcome);
    }

    private static final long COMPILE_TIMEOUT = 120_000;
    private static final ExecutorService POOL = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "stress-compile");
        t.setDaemon(true);
        return t;
    });

    private final Config cfg;
    private final Listener listener;
    private final List<ProcessRunner> runners = new CopyOnWriteArrayList<>();
    private volatile boolean stopped;

    public StressTester(Config cfg, Listener listener) {
        this.cfg = cfg;
        this.listener = listener;
    }

    public void stop() {
        stopped = true;
        runners.forEach(ProcessRunner::cancel);
    }

    private ProcessRunner newRunner() {
        ProcessRunner r = new ProcessRunner();
        runners.add(r);
        if (stopped) {
            r.cancel();
        }
        return r;
    }

    @Override
    public void run() {
        Outcome outcome;
        try (TempDir tmp = TempDir.create("grader-stress-")) {
            outcome = execute(tmp.path());
        } catch (Exception e) {
            outcome = new Outcome(Kind.SETUP_ERROR, 0, "", "", "", "Error: " + e.getMessage());
        }
        listener.onFinished(outcome);
    }

    private Outcome stoppedAt(int done) {
        return new Outcome(Kind.STOPPED, done, "", "", "", "Stopped by user after " + done + " test(s)");
    }

    private Outcome execute(Path dir) throws Exception {
        listener.onStatus("Compiling files...", Tone.INFO);

        // --- Optimised (main) source ---
        Path mainSrc;
        if (cfg.main().fromFile()) {
            mainSrc = cfg.main().file();
            if (mainSrc == null || !Files.isRegularFile(mainSrc)) {
                return new Outcome(Kind.MAIN_COMPILE_ERROR, 0, "", "",
                        "Main solution file not found:\n" + mainSrc, "Error compiling Main Solution");
            }
        } else {
            String code = cfg.main().code();
            if (code == null || code.isBlank()) {
                return new Outcome(Kind.MAIN_COMPILE_ERROR, 0, "", "", "The editor is empty.",
                        "Error compiling Main Solution");
            }
            mainSrc = dir.resolve("opt_src.cpp");
            Files.writeString(mainSrc, code, StandardCharsets.UTF_8);
        }

        Path genClasses = dir.resolve("gen-classes");
        Path bruteExe = CppCompiler.exePath(dir, "brute");
        Path optExe = CppCompiler.exePath(dir, "opt");

        // Compile all three in parallel (g++ with bits/stdc++.h is slow).
        ProcessRunner genRunner = newRunner();
        ProcessRunner bruteRunner = newRunner();
        ProcessRunner mainRunner = newRunner();
        CompletableFuture<JavaGenerator.Build> fGen = CompletableFuture.supplyAsync(
                () -> JavaGenerator.compile(genRunner, cfg.generator(), genClasses, COMPILE_TIMEOUT), POOL);
        CompletableFuture<CppCompiler.Result> fBrute = CompletableFuture.supplyAsync(
                () -> CppCompiler.compile(bruteRunner, cfg.compiler(), cfg.brute(), bruteExe, cfg.flags(), COMPILE_TIMEOUT), POOL);
        Path finalMainSrc = mainSrc;
        CompletableFuture<CppCompiler.Result> fMain = CompletableFuture.supplyAsync(
                () -> CppCompiler.compile(mainRunner, cfg.compiler(), finalMainSrc, optExe, cfg.flags(), COMPILE_TIMEOUT), POOL);

        JavaGenerator.Build gen = fGen.join();
        CppCompiler.Result brute = fBrute.join();
        CppCompiler.Result main = fMain.join();

        if (stopped) {
            return stoppedAt(0);
        }
        if (!gen.success()) {
            return new Outcome(Kind.GEN_COMPILE_ERROR, 0, "Generator compilation failed:\n\n" + gen.diagnostics(),
                    "", "", "Error compiling Generator");
        }
        if (!brute.success()) {
            return new Outcome(Kind.BRUTE_COMPILE_ERROR, 0, "", "Brute force compilation failed:\n\n" + brute.diagnostics(),
                    "", "Error compiling Brute Force");
        }
        if (!main.success()) {
            return new Outcome(Kind.MAIN_COMPILE_ERROR, 0, "", "", "Main solution compilation failed:\n\n" + main.diagnostics(),
                    "Error compiling Main Solution");
        }

        int n = cfg.iterations();
        long timeout = cfg.timeoutMillis();
        long genTimeout = Math.max(timeout, 10_000);   // JVM start-up counts against the generator
        ProcessRunner runner = newRunner();
        listener.onStatus("Running " + n + " tests...", Tone.INFO);

        for (int i = 1; i <= n; i++) {
            if (stopped) {
                return stoppedAt(i - 1);
            }
            listener.onStatus("Running Test " + i + "/" + n + "...", Tone.INFO);

            // 1. Generate input (seed = iteration number)
            ProcessRunner.Result g = runner.run(JavaGenerator.runCommand(gen, String.valueOf(i)), dir, null, genTimeout);
            if (stopped || g.cancelled()) {
                return stoppedAt(i - 1);
            }
            if (!g.ok()) {
                return new Outcome(Kind.GEN_FAILED, i, failure("Generator", g, genTimeout), "", "",
                        "Generator " + (g.timedOut() ? "timed out" : "crashed") + " on test " + i);
            }
            String input = g.stdout();

            // 2. Brute force
            ProcessRunner.Result b = runner.run(List.of(bruteExe.toString()), dir, input, timeout);
            if (stopped || b.cancelled()) {
                return stoppedAt(i - 1);
            }
            if (!b.ok()) {
                return new Outcome(Kind.BRUTE_FAILED, i, input, failure("Brute force", b, timeout), "",
                        "Brute Force " + (b.timedOut() ? "timed out" : "crashed") + " on test " + i);
            }

            // 3. Optimised
            ProcessRunner.Result o = runner.run(List.of(optExe.toString()), dir, input, timeout);
            if (stopped || o.cancelled()) {
                return stoppedAt(i - 1);
            }
            if (!o.ok()) {
                String actual = failure("Optimized code", o, timeout);
                if (!o.stdout().isBlank()) {
                    actual += "\n\n--- partial stdout ---\n" + o.stdout();
                }
                return new Outcome(Kind.MAIN_FAILED, i, input, b.stdout(), actual,
                        "Optimized Code " + (o.timedOut() ? "timed out" : "crashed") + " on test " + i);
            }

            // 4. Compare
            if (!OutputCompare.same(b.stdout(), o.stdout())) {
                return new Outcome(Kind.MISMATCH, i, input, b.stdout(), o.stdout(), "Mismatch found on test " + i + "!");
            }
            listener.onProgress(i, n);
        }
        return new Outcome(Kind.PASSED, n, "", "", "", "Passed all " + n + " tests!");
    }

    private static String failure(String who, ProcessRunner.Result r, long timeout) {
        if (r.timedOut()) {
            return "TIME LIMIT EXCEEDED\n" + who + " was still running after " + (timeout / 1000.0) + " s.";
        }
        String s = "CRASHED\n" + who + " finished with " + ExitCodes.describe(r.exitCode());
        if (!r.stderr().isBlank()) {
            s += "\n\n--- stderr ---\n" + r.stderr().stripTrailing();
        }
        return s;
    }
}
