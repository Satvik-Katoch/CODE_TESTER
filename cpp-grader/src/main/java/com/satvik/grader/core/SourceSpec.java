package com.satvik.grader.core;

import java.nio.file.Path;

/**
 * Where the solution's source comes from: either the text in the editor or a file on disk
 * (the legacy "Use File Path" checkbox).
 */
public record SourceSpec(boolean fromFile, Path file, String code) {

    public static SourceSpec ofFile(Path file) {
        return new SourceSpec(true, file, null);
    }

    public static SourceSpec ofCode(String code) {
        return new SourceSpec(false, null, code);
    }
}
