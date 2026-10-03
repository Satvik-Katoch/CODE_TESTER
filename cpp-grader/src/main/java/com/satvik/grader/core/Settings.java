package com.satvik.grader.core;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Tiny persistent key/value store at {@code ~/.cpp-grader/settings.properties}. */
public final class Settings {

    private final Path dir;
    private final Path file;
    private final Properties props = new Properties();

    private Settings(Path dir) {
        this.dir = dir;
        this.file = dir.resolve("settings.properties");
    }

    public static Settings load() {
        Settings s = new Settings(Path.of(System.getProperty("user.home"), ".cpp-grader"));
        if (Files.isRegularFile(s.file)) {
            try (Reader r = Files.newBufferedReader(s.file, StandardCharsets.UTF_8)) {
                s.props.load(r);
            } catch (IOException ignored) {
                // corrupt settings -> start fresh
            }
        }
        return s;
    }

    public Path dir() {
        return dir;
    }

    public String get(String key, String def) {
        return props.getProperty(key, def);
    }

    public int getInt(String key, int def) {
        try {
            return Integer.parseInt(props.getProperty(key, String.valueOf(def)).trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public boolean getBool(String key, boolean def) {
        return Boolean.parseBoolean(props.getProperty(key, String.valueOf(def)));
    }

    public void put(String key, Object value) {
        props.setProperty(key, value == null ? "" : value.toString());
    }

    public void save() {
        try {
            Files.createDirectories(dir);
            try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                props.store(w, "Satvik's C++ Code Grader settings");
            }
        } catch (IOException ignored) {
            // non-fatal
        }
    }

    public String readText(String name) {
        Path p = dir.resolve(name);
        try {
            return Files.isRegularFile(p) ? Files.readString(p, StandardCharsets.UTF_8) : null;
        } catch (IOException e) {
            return null;
        }
    }

    public void writeText(String name, String text) {
        try {
            Files.createDirectories(dir);
            Files.writeString(dir.resolve(name), text, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // non-fatal
        }
    }
}
