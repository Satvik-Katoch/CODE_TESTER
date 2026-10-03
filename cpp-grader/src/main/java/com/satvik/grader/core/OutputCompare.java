package com.satvik.grader.core;

import java.util.ArrayList;
import java.util.List;

/** Output normalisation / comparison helpers. */
public final class OutputCompare {

    private OutputCompare() {
    }

    /**
     * Normalises an output for verdict purposes: CRLF -> LF, trailing whitespace removed from every
     * line, and leading/trailing blank space stripped from the whole text (legacy {@code strip()}).
     */
    public static String normalize(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (String line : s.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1)) {
            sb.append(line.stripTrailing()).append('\n');
        }
        return sb.toString().strip();
    }

    public static boolean same(String a, String b) {
        return normalize(a).equals(normalize(b));
    }

    /** Python-style {@code splitlines()}: no trailing empty element for a final newline. */
    public static List<String> lines(String s) {
        List<String> out = new ArrayList<>();
        if (s == null || s.isEmpty()) {
            return out;
        }
        String[] parts = s.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        int n = parts.length;
        if (n > 0 && parts[n - 1].isEmpty()) {
            n--;
        }
        for (int i = 0; i < n; i++) {
            out.add(parts[i]);
        }
        return out;
    }
}
