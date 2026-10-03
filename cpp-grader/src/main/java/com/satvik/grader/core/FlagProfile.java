package com.satvik.grader.core;

/**
 * The compilation-flag profiles offered in the toolbar drop-down.
 * Every profile's flags can be edited by the user; edits are remembered per profile.
 */
public enum FlagProfile {

    DEFAULT("Default",
            "Contest build (same flags as the legacy grader)",
            "-O2 -Wall"),

    DEBUG("DEBUG",
            "Defines LOCAL (enables your debug() macros) + checked STL, no optimisation",
            "-g -O0 -Wall -Wextra -Wshadow -DLOCAL -D_GLIBCXX_DEBUG -D_GLIBCXX_DEBUG_PEDANTIC "
                    + "-D_GLIBCXX_ASSERTIONS -fno-omit-frame-pointer"),

    CUSTOM("Custom",
            "Your own flags - edit them in the box",
            CppCompiler.isWindows()
                    ? "-std=c++20 -O2 -Wall -Wextra -Wl,--stack=268435456"
                    : "-std=c++20 -O2 -Wall -Wextra");

    private final String label;
    private final String description;
    private final String defaultFlags;

    FlagProfile(String label, String description, String defaultFlags) {
        this.label = label;
        this.description = description;
        this.defaultFlags = defaultFlags;
    }

    public String label() {
        return label;
    }

    public String description() {
        return description;
    }

    public String defaultFlags() {
        return defaultFlags;
    }

    public static FlagProfile parse(String name) {
        for (FlagProfile p : values()) {
            if (p.name().equalsIgnoreCase(name)) {
                return p;
            }
        }
        return DEFAULT;
    }
}
