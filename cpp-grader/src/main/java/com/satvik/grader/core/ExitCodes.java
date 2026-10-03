package com.satvik.grader.core;

/** Turns raw process exit codes (incl. Windows NTSTATUS crash codes) into human-readable text. */
public final class ExitCodes {

    private ExitCodes() {
    }

    public static String describe(int code) {
        String meaning = switch (code) {
            case 0xC0000005 -> "Access violation (segfault: bad pointer / out-of-bounds index)";
            case 0xC00000FD -> "Stack overflow (recursion too deep?)";
            case 0xC0000094 -> "Integer division by zero";
            case 0xC0000095 -> "Integer overflow";
            case 0xC000008E, 0xC000008F, 0xC0000090, 0xC0000091, 0xC0000092, 0xC0000093 -> "Floating point exception";
            case 0xC0000409 -> "Fast-fail / abort (failed assertion or _GLIBCXX_DEBUG check?)";
            case 0xC0000374 -> "Heap corruption";
            case 0xC000001D -> "Illegal instruction";
            case 0xC0000017 -> "Out of memory";
            case 3 -> "abort() called (failed assert / std::terminate / debug-mode STL check)";
            case 134 -> "SIGABRT (abort)";
            case 136 -> "SIGFPE (arithmetic error)";
            case 139 -> "SIGSEGV (segmentation fault)";
            default -> null;
        };
        String number = (code < 0 || code > 255) ? code + String.format(" (0x%08X)", code) : String.valueOf(code);
        return meaning == null ? "exit code " + number : "exit code " + number + " - " + meaning;
    }
}
