# Satvik's C++ Code Grader

A professional, feature-rich GUI application for compiling, running, stress-testing, and verifying C++ code. Designed specifically for competitive programmers who need robust debugging tools and counter-case discovery.

## 📋 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
- [Quick Start](#quick-start)
- [Stress Testing (New!)](#stress-testing)
- [User Interface](#user-interface)
- [Usage Guide](#usage-guide)
- [Keyboard Shortcuts](#keyboard-shortcuts)
- [Technical Details](#technical-details)
- [Troubleshooting](#troubleshooting)

## 🎯 Overview

The C++ Code Grader is a desktop application designed for competitive programming (CP). It goes beyond simple compilation by offering an integrated **Stress Testing Suite**—allowing you to automatically find edge cases where your optimized solution fails compared to a brute-force approach. It also features a customizable **Diff Viewer** to pinpoint exact output mismatches.

## ✨ Features

### 🚀 Core Functionality
- **Real-time Compilation**: Compile C++ code with G++ compiler integration.
- **Input/Output Testing**: Run custom inputs and check against desired outputs.
- **Automated Stress Testing**: Automatically generate thousands of random test cases to find "Wrong Answer" (WA) bugs.
- **Smart Diff Viewer**: Toggle between "Smart Sync" (Git-style) and "Raw Line-by-Line" comparison.
- **Multi-threaded Execution**: UI remains responsive during long stress tests or infinite loops.

### 📝 Code Editor Features
- **Auto-pairing**: Automatic closing of brackets, parentheses, and quotes.
- **Smart Indentation**: Tab support with 4-space indentation.
- **Syntax Highlighting Colors**: Dark theme optimized for long coding sessions.
- **File or Text Mode**: Run code directly from the editor OR select an external `.cpp` file.

### 🎨 Visual Features
- **Professional Dark Theme**: Easy on the eyes (#282C34 background).
- **Three-Column Error View**: When stress testing fails, see Input, Expected Output, and Your Output side-by-side.
- **Color-coded Status**: Clear indicators for Success (PASSED), Failure (FAILED), and Crashes.

## 📦 Requirements

- **Operating System**: Windows, Linux, or macOS.
- **Python**: Version 3.6 or higher.
- **G++ Compiler**: Must be installed and accessible via PATH.

## 🚀 Installation

### Step 1: Install Python
Download and install Python from [python.org](https://www.python.org/downloads/).

### Step 2: Install G++ Compiler
Ensure `g++ --version` works in your terminal.
- **Windows**: Install MinGW-w64.
- **Linux**: `sudo apt-get install g++`
- **macOS**: `xcode-select --install`

### Step 3: Run the Application
```bash python gui_grader.py```

## 🔨 Stress Testing (New Feature)

This is the most powerful feature for debugging "Wrong Answer" verdicts.

### How it works
The tool runs three programs in a loop:
1.  **Generator**: Creates a random test case.
2.  **Brute Force**: Solves the test case correctly (but slowly).
3.  **Optimized**: Your main solution that you want to test.

If the outputs of (2) and (3) differ, the tool stops and shows you the counter-case.

### Step-by-Step Guide
1.  Click the **"Stress Test"** button in the top navigation bar.
2.  **Generator Script**: Select a C++ file that prints a random test case to stdout. 
    (See `generator_template cpp` for a robust start).
3.  **Brute Force**: Select a C++ file containing a naive, slow, but 100% correct solution.
4.  **Optimized Code**: The tool automatically uses the code from the main "Editor" tab (or the file selected there).
5.  **Iterations**: Set how many random tests to run (e.g., 100 or 1000).
6.  Click **Start Stress Test**.

## 🖥️ User Interface

### 1. Editor View (Main)
- **Left**: Code Editor (or File Path selector).
- **Right**: Input, Desired Output, and Execution Status.
- **Compile & Run**: Runs a single test case.

### 2. Compare Output View (Diff)
- **Smart Sync Checkbox**: 
    - **Checked**: Aligns matching lines (like GitHub). Good for finding missing lines.
    - **Unchecked**: Strict line-by-line comparison. Perfect for YES/NO problems where alignment matters.
- **Colors**: Red (Removed/Mismatch), Green (Added).

### 3. Stress Test View
- **Top**: File selectors for Generator and Brute Force.
- **Controls**: Iteration counter, Start/Stop buttons.
- **Bottom**: Three-pane results showing **Input**, **Expected**, and **Actual** text.

## 📖 Usage Guide

### Running a Standard Test
1.  Write your code in the left editor.
2.  Paste a sample input from the problem statement into the "Input" box.
3.  Paste the sample output into "Desired Output".
4.  Click **Compile & Run**.
5.  Check the status:
    - `✅ --- Result: PASSED --- ✅`
    - `❌ --- Result: FAILED --- ❌`

### Using "Smart Sync" vs "Raw Diff"
- If your output is `YES` and Expected is `NO`, **Uncheck** "Smart Sync Lines". This will show them side-by-side as a direct mismatch.
- If you are debugging a large array and missed one number in the middle, **Check** "Smart Sync Lines". The viewer will align the rest of the numbers so you can see exactly which one is missing.

## ⌨️ Keyboard Shortcuts

- `Tab` `/` `Shift+Tab`: Indent / Dedent
- `(`, `[`, `{`, `'`, `"`: Auto-close pairs
- `Ctrl+C` / `V`: Standard copy/paste (Context aware)

## 🔧 Technical Details

**Compilation Flags**:
The tool compiles all C++ files using:
`g++ -O2 -Wall source.cpp -o executable`
- `-O2`: Optimization Level 2 (Standard for CP).
- `-Wall`: Enable all warnings (Helps catch uninitialized variables).

**Generator Seeding**:
The tool passes the current iteration number as a command-line argument (`argv[1]`) to your generator. This ensures that **Test Case #5 is always the same**, making debugging reproducible.

## 🐛 Troubleshooting

**"Error: File not found" during Stress Test**
Ensure you have selected valid paths for both the Generator and Brute Force scripts.

**"Generator crashed"**
Check your generator code. Ensure it doesn't divide by zero or access out-of-bounds memory.

**"Execution timed out"**
The default timeout is 5 seconds per test case. If your brute force is too slow ($O(N^3)$), try reducing the constraints in your generator (e.g., $N=100$ instead of $N=100000$).

---
**Happy Coding! 🚀**