# Satvik's C++ Code Grader

A professional, feature-rich GUI application for compiling, running, and testing C++ code with a GitHub-style diff viewer. Perfect for competitive programmers who need quick feedback on their solutions.

## 📋 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
- [Quick Start](#quick-start)
- [User Interface](#user-interface)
- [Usage Guide](#usage-guide)
- [Keyboard Shortcuts](#keyboard-shortcuts)
- [Technical Details](#technical-details)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)

## 🎯 Overview

The C++ Code Grader is a desktop application designed specifically for competitive programmers and students learning C++. It provides an integrated environment to write, compile, test, and compare C++ code outputs against expected results, all within a sleek dark-themed interface.

## ✨ Features

### Core Functionality
- **Real-time Compilation**: Compile C++ code with G++ compiler integration
- **Input/Output Testing**: Provide custom inputs and compare against expected outputs
- **Side-by-Side Diff Viewer**: GitHub-style diff comparison for mismatched outputs
- **Execution Status Logging**: Detailed compilation and runtime feedback
- **Multi-threaded Execution**: Non-blocking UI during compilation and execution

### Code Editor Features
- **Auto-pairing**: Automatic closing of brackets, parentheses, and quotes
- **Smart Indentation**: Tab support with 4-space indentation
- **Syntax Highlighting Colors**: Dark theme optimized for long coding sessions
- **Undo/Redo Support**: Built-in edit history
- **Paste & Clear Shortcuts**: Quick clipboard integration

### Visual Features
- **Professional Dark Theme**: Easy on the eyes (#282C34 background)
- **Color-coded Status Messages**: Success (green), Failure (red), Info (blue), Warning (yellow), Error (red)
- **Responsive Layout**: Resizable panes with proportional weights
- **Line Numbers**: Displayed in diff viewer for easy navigation

## 📦 Requirements

### System Requirements
- **Operating System**: Windows, Linux, or macOS
- **Python**: Version 3.6 or higher
- **G++ Compiler**: Must be installed and accessible via PATH

### Python Dependencies
- `tkinter` (usually included with Python)
- Standard library modules: `subprocess`, `os`, `tempfile`, `threading`, `difflib`

## 🚀 Installation

### Step 1: Install Python
Download and install Python from [python.org](https://www.python.org/downloads/)

### Step 2: Install G++ Compiler

**Windows:**
```bash
# Install MinGW-w64 or install via MSYS2
# Add G++ to your system PATH
```

**Linux:**
```bash
sudo apt-get update
sudo apt-get install g++
```

**macOS:**
```bash
# Install Xcode Command Line Tools
xcode-select --install
```

### Step 3: Verify G++ Installation
```bash
g++ --version
```

### Step 4: Run the Application
```bash
python cpp_grader.py
```

### Step 5: Create Standalone Executable (Optional)

You can create a standalone `.exe` file (Windows) or executable (Linux/macOS) that doesn't require Python to be installed.

#### Using PyInstaller

**Install PyInstaller:**
```bash
pip install pyinstaller
```

**Create Executable:**

**For Windows (.exe):**
```bash
pyinstaller --onefile --windowed --name "CppGrader" cpp_grader.py
```

**For Linux/macOS:**
```bash
pyinstaller --onefile --windowed --name "CppGrader" cpp_grader.py
```

**Options Explained:**
- `--onefile`: Packages everything into a single executable
- `--windowed`: Hides the console window (GUI mode)
- `--name`: Sets the name of the output executable

**Find Your Executable:**
- The `.exe` (or executable) will be in the `dist/` folder
- You can distribute this file to others
- **Important**: Users still need G++ installed and in PATH

**Advanced Options:**
```bash
# Add an icon (Windows)
pyinstaller --onefile --windowed --icon=icon.ico --name "CppGrader" cpp_grader.py

# Specify output directory
pyinstaller --onefile --windowed --distpath ./build_output --name "CppGrader" cpp_grader.py

# Clean build (removes old build files)
pyinstaller --onefile --windowed --clean --name "CppGrader" cpp_grader.py
```

## 🏁 Quick Start

1. **Launch the application**: Run the Python script
2. **Write your C++ code**: Enter code in the "C++ Code" editor (left pane)
3. **Add test input**: Enter input data in the "Input" section (top right)
4. **Specify expected output**: Enter the desired output in "Desired Output" (middle right)
5. **Click "Compile & Run"**: The application will compile, run, and compare outputs
6. **View results**: Check the "Execution Status" for success/failure
7. **Compare differences**: If outputs don't match, click "Compare Output" to see a side-by-side diff

## 🖥️ User Interface

### Main Window Layout

The application consists of two main views:

#### 1. Editor View (Default)

**Left Panel - C++ Code Editor:**
- Large code editing area with syntax highlighting
- Paste and Clear buttons for quick operations
- Default template code included

**Right Panel (Split into 3 sections):**
- **Input**: Test case input data
- **Desired Output**: Expected program output
- **Execution Status**: Real-time compilation and execution feedback

#### 2. Compare Output View

**Split-screen diff viewer showing:**
- **Left Side**: Expected output (your desired output)
- **Right Side**: Actual output (generated by your program)
- **Color Coding**:
  - Red background: Lines removed/missing from output
  - Green background: Lines added/extra in output
  - Gray background: Empty placeholder lines for alignment
- **Line Numbers**: Both sides show corresponding line numbers

### Top Bar Controls

- **Compile & Run Button**: Triggers compilation and execution (left side)
- **Editor Button**: Switch to code editing view (right side)
- **Compare Output Button**: Switch to diff viewer (enabled after running tests)

## 📖 Usage Guide

### Writing Code

1. The editor starts with a template:
```cpp
#include <iostream>

int main() {
    // Your code here
    std::cout << "Hello, Satvik!";
    return 0;
}
```

2. Replace or modify the template with your solution
3. Use the auto-pairing feature for brackets and quotes
4. Press Tab for 4-space indentation, Shift+Tab to dedent

### Testing Your Code

1. **Add Input Data**: Enter test case inputs in the "Input" section
   - Multi-line inputs supported
   - Paste directly from problem statements

2. **Specify Expected Output**: Enter the exact expected output
   - Include all newlines and spaces
   - Should match the problem's expected format

3. **Run Tests**: Click "Compile & Run"
   - Button changes to "Running..." during execution
   - Compilation timeout: 10 seconds
   - Execution timeout: 5 seconds

### Understanding Results

#### Success Case
```
✅ --- Result: Good Job Satvik --- ✅
```
Your output exactly matches the expected output!

#### Failure Case
```
❌ --- Result: Try again Satvik --- ❌
Outputs do not match. Click 'Compare Output' for details.
```
Use the "Compare Output" button to see differences.

### Status Messages

The "Execution Status" panel shows:
- **INFO** (Blue): Compilation and execution progress
- **WARNING** (Yellow): Compiler warnings, stderr messages
- **ERROR** (Red): Compilation errors, runtime crashes
- **SUCCESS** (Green): Test passed message
- **FAILURE** (Red): Test failed message

## ⌨️ Keyboard Shortcuts

### In C++ Code Editor:
- `Tab`: Insert 4 spaces
- `Shift+Tab`: Remove up to 4 leading spaces
- `(`: Auto-insert `()` and place cursor between
- `[`: Auto-insert `[]` and place cursor between
- `{`: Auto-insert `{}` and place cursor between
- `'`: Auto-insert `''` and place cursor between
- `"`: Auto-insert `""` and place cursor between

### Standard Text Editing:
- `Ctrl+Z` / `Cmd+Z`: Undo
- `Ctrl+Y` / `Cmd+Y`: Redo
- `Ctrl+C` / `Cmd+C`: Copy
- `Ctrl+V` / `Cmd+V`: Paste
- `Ctrl+X` / `Cmd+X`: Cut

## 🔧 Technical Details

### Architecture

**Language**: Python 3.6+  
**GUI Framework**: Tkinter with ttk (themed widgets)  
**Compilation**: G++ compiler with `-O2 -Wall` flags  
**Execution**: Subprocess module with timeout protection

### Compilation Process

1. Code is written to a temporary file (`main.cpp`)
2. G++ compiles with optimization level 2 and all warnings enabled
3. Executable is created in a temporary directory
4. Temporary files are automatically cleaned up

### Output Comparison

- Normalization: Strips whitespace and converts line endings
- Algorithm: Uses `difflib.SequenceMatcher` for line-by-line comparison
- Tags: `equal`, `delete`, `insert`, `replace` operations tracked

### Safety Features

- **Timeouts**: 
  - Compilation: 10 seconds
  - Execution: 5 seconds
- **Sandboxing**: Uses temporary directories for compilation
- **Error Handling**: Comprehensive exception catching
- **Thread Safety**: UI updates scheduled on main thread

### File Structure

```
cpp_grader.py
├── CppGraderApp (Main Class)
│   ├── __init__: Initialize UI
│   ├── _configure_styles: Setup visual theme
│   ├── _create_main_layout: Build editor view
│   ├── _create_diff_widgets: Build comparison view
│   ├── _setup_code_editor_feel: Configure editor bindings
│   ├── run_test: Execute compilation and testing
│   └── _update_diff_text: Generate side-by-side diff
```

## 🐛 Troubleshooting

### "g++ not found" Error
**Problem**: G++ compiler is not in your system PATH  
**Solution**:
- Windows: Add MinGW bin folder to PATH environment variable
- Linux: Install `g++` package via package manager
- macOS: Install Xcode Command Line Tools

### Compilation Timeout
**Problem**: Code takes too long to compile  
**Solution**:
- Check for excessive template instantiations
- Simplify #include directives
- Remove unnecessary preprocessor directives

### Execution Timeout (>5s)
**Problem**: Program runs longer than 5 seconds  
**Solution**:
- Check for infinite loops
- Optimize algorithm complexity
- Verify input data is correct

### Output Mismatch (But Looks Correct)
**Problem**: Visual output matches but test fails  
**Solution**:
- Check for trailing spaces or newlines
- Verify line ending format (Windows vs Unix)
- Copy expected output directly from problem statement

### Font Display Issues
**Problem**: Fonts appear too small or incorrect  
**Solution**: Modify font configuration in `_configure_styles()`:
```python
self.code_font = font.Font(family="Consolas", size=12)  # Increase size
```

### Window Too Small/Large
**Problem**: Initial window size is uncomfortable  
**Solution**: Modify in `__init__()`:
```python
self.geometry("1600x900")  # Adjust dimensions
```

## 🤝 Contributing

### Potential Enhancements

- [ ] Add syntax highlighting for C++ keywords
- [ ] Support for multiple test cases
- [ ] Save/load code and test cases
- [ ] Custom compiler flags configuration
- [ ] Theme customization options
- [ ] Line-by-line step debugger
- [ ] Performance benchmarking
- [ ] Export diff to HTML/PDF

### Code Style
- Follow PEP 8 guidelines
- Use docstrings for functions
- Maintain existing naming conventions

## 📄 License

This project is provided as-is for educational and competitive programming purposes.

## 👤 Author

Created by Satvik for the competitive programming community.

---

**Version**: 1.0  
**Last Updated**: 2025  
**Python Version**: 3.6+  
**Platform**: Cross-platform (Windows, Linux, macOS)

---

## 🎓 Tips for Competitive Programmers

1. **Keep Test Cases Handy**: Copy input/output from problem statements
2. **Test Edge Cases**: Empty input, maximum constraints, single elements
3. **Use Compiler Warnings**: The `-Wall` flag catches many common errors
4. **Check Time Complexity**: 5-second timeout is generous but optimize anyway
5. **Verify Output Format**: Extra spaces or newlines are common mistakes
6. **Use the Diff Viewer**: Quickly spot where your output diverges

## ⚙️ Support

For issues or questions:
- Check the Troubleshooting section above
- Verify G++ installation and PATH configuration
- Ensure Python 3.6+ is installed correctly

---

**Happy Coding! 🚀**
