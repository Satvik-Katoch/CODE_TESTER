# Satvik's C++ Code Grader

A modern, liquid-glass Java Swing application to write, compile, run, and stress-test competitive programming C++ solutions.

## Features

- **Liquid Glass UI:** Modern dark translucent interface built entirely in Swing.
- **Integrated Editor:** Syntax highlighting, brackets auto-pairing, auto-indent, and line numbers.
- **Diff Viewer:** Myers diff algorithm highlighting missing/added lines to find bugs quickly.
- **Stress Tester:** Run a Java Generator against a Brute Force C++ and an Optimized C++ solution in parallel until a mismatch is found.
- **Compilation Flags:** Choose between Default, DEBUG, and Custom GCC flags directly from the navbar.

## Requirements

- **Java 17+** (Ensure `java` and `javac` are in your PATH).
- **MinGW GCC** (Ensure `g++` is in your PATH).

## How to run

Simply run `run.bat` in this folder:
```cmd
run.bat
```
This will compile the Java code (if needed) and launch the UI.

## Using the Generator

Use the `Generator.java` file in this folder as a template. It contains a lightweight port of `testlib.h` functionalities (`rnd.nextInt()`, `rnd.nextArray()`, `rnd.wnext()` for edge case testing, etc.). The GUI will automatically invoke it with a different seed for each iteration.
