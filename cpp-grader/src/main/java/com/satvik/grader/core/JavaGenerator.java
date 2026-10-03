package com.satvik.grader.core;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Compiles and runs the stress-test generator written in Java ({@code Generator.java}).
 * <p>
 * The generator is executed as {@code java -cp <classes> <MainClass> <iteration>} so that, exactly
 * like the old C++ generator's {@code argv[1]}, test #i is always generated from seed i.
 */
public final class JavaGenerator {

    private JavaGenerator() {
    }

    public record Build(boolean success, String diagnostics, String mainClass, Path classDir, long millis) {
    }

    private static final Pattern PACKAGE = Pattern.compile("^\\s*package\\s+([\\w.]+)\\s*;", Pattern.MULTILINE);
    private static final Pattern PUBLIC_TYPE = Pattern.compile(
            "\\bpublic\\s+(?:final\\s+|abstract\\s+|strictfp\\s+)*(?:class|record|enum|interface)\\s+(\\w+)");
    private static final Pattern ANY_TYPE = Pattern.compile("\\b(?:class|record|enum|interface)\\s+(\\w+)");
    private static final Pattern MAIN_METHOD = Pattern.compile("\\bstatic\\s+void\\s+main\\s*\\(");

    public static Build compile(ProcessRunner runner, Path source, Path outDir, long timeoutMillis) {
        long t0 = System.nanoTime();
        try {
            Files.createDirectories(outDir);
            String code = new String(Files.readAllBytes(source), StandardCharsets.UTF_8);
            String mainClass = detectMainClass(code, source);

            String diagnostics;
            boolean ok;
            JavaCompiler jc = ToolProvider.getSystemJavaCompiler();
            if (jc != null) {
                ByteArrayOutputStream err = new ByteArrayOutputStream();
                int rc = jc.run(null, err, err, "-d", outDir.toString(), "-encoding", "UTF-8",
                        "-Xlint:none", source.toString());
                ok = rc == 0;
                diagnostics = err.toString(StandardCharsets.UTF_8).strip();
            } else {
                List<String> cmd = new ArrayList<>(List.of(tool("javac"), "-d", outDir.toString(),
                        "-encoding", "UTF-8", "-Xlint:none", source.toString()));
                ProcessRunner.Result r = runner.run(cmd, null, null, timeoutMillis);
                ok = r.ok();
                diagnostics = (r.stderr() + "\n" + r.stdout()).strip();
                if (r.timedOut()) {
                    diagnostics = "javac timed out.\n" + diagnostics;
                }
            }
            long ms = (System.nanoTime() - t0) / 1_000_000L;
            if (ok) {
                String rel = mainClass.replace('.', '/') + ".class";
                if (!Files.exists(outDir.resolve(rel))) {
                    return new Build(false, "Compiled, but could not find main class '" + mainClass
                            + "'. Make sure the public class name matches the file name.", mainClass, outDir, ms);
                }
            }
            return new Build(ok, diagnostics, mainClass, outDir, ms);
        } catch (IOException e) {
            return new Build(false, "Could not compile generator: " + e.getMessage(), null, outDir,
                    (System.nanoTime() - t0) / 1_000_000L);
        }
    }

    /** Command that runs one generator iteration with the given seed. */
    public static List<String> runCommand(Build build, String seed) {
        return List.of(tool("java"),
                "-XX:TieredStopAtLevel=1",   // faster JVM start-up - generators are short-lived
                "-XX:+UseSerialGC",
                "-Xss64m",
                "-Dfile.encoding=UTF-8",
                "-cp", build.classDir().toString(),
                build.mainClass(), seed);
    }

    static String detectMainClass(String code, Path source) {
        String pkg = null;
        Matcher pm = PACKAGE.matcher(code);
        if (pm.find()) {
            pkg = pm.group(1);
        }
        String name = null;
        Matcher pub = PUBLIC_TYPE.matcher(code);
        if (pub.find()) {
            name = pub.group(1);
        } else {
            Matcher main = MAIN_METHOD.matcher(code);
            if (main.find()) {
                Matcher t = ANY_TYPE.matcher(code);
                while (t.find() && t.start() < main.start()) {
                    name = t.group(1);
                }
            }
        }
        if (name == null) {
            String file = source.getFileName().toString();
            name = file.endsWith(".java") ? file.substring(0, file.length() - 5) : file;
        }
        return pkg == null ? name : pkg + "." + name;
    }

    /** Resolves {@code java}/{@code javac} from the running JDK, falling back to PATH. */
    static String tool(String name) {
        String exe = CppCompiler.isWindows() ? name + ".exe" : name;
        Path p = Path.of(System.getProperty("java.home"), "bin", exe);
        return Files.isRegularFile(p) ? p.toString() : name;
    }
}
