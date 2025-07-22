import tkinter as tk
from tkinter import ttk, scrolledtext, font
import subprocess
import os
import tempfile
import threading
import difflib

class CppGraderApp(tk.Tk):
    """
    An advanced GUI to compile, run, and test C++ code, featuring grouped
    Paste/Clear buttons and an enhanced code editor.
    """
    def __init__(self):
        super().__init__()
        self.title("Satvik's C++ Code Grader")
        self.geometry("1200x800")
        self.configure(bg="#282C34")

        self.last_generated_output = ""
        self.last_desired_output = ""
        
        self._configure_styles()
        
        self.editor_frame = ttk.Frame(self)
        self.diff_frame = ttk.Frame(self)

        self._create_main_layout()
        self._create_diff_widgets()
        
        self._setup_code_editor_feel()

        self.show_editor_page()

    def _configure_styles(self):
        """Sets up all the ttk styles and fonts for the application."""
        self.style = ttk.Style(self)
        self.style.theme_use('clam')

        self.default_font = font.nametofont("TkDefaultFont")
        self.default_font.configure(family="Segoe UI", size=11)
        self.title_font = font.Font(family="Segoe UI", size=13, weight="bold")
        self.code_font = font.Font(family="Consolas", size=11)

        self.style.configure("TLabel", background="#282C34", foreground="white", font=self.default_font)
        self.style.configure("TFrame", background="#282C34")
        
        self.style.configure("Top.TButton", font=("Segoe UI", 10, "bold"), background="#007ACC", foreground="white", borderwidth=0, padding=(10, 5))
        self.style.map("Top.TButton", background=[('active', '#005f9e')])
        
        self.style.configure("Tool.TButton", font=("Segoe UI", 8), background="#4F5563", foreground="white", borderwidth=0, padding=(5, 2))
        self.style.map("Tool.TButton", background=[('active', '#6A7180')])

        nav_font = ("Segoe UI", 9)
        self.style.configure("Nav.TButton", font=nav_font, padding=(10, 4), borderwidth=1, relief=tk.FLAT)
        self.style.map("Nav.TButton",
            foreground=[('disabled', '#777'), ('!disabled', 'white')],
            background=[('disabled', '#333'), ('active', '#5c6370')]
        )
        self.style.configure("ActiveNav.TButton", background="#007ACC", foreground="white")
        self.style.configure("InactiveNav.TButton", background="#4F5563", foreground="white")

        self.style.configure("TPanedWindow", background="#282C34")
        self.style.configure("Sash", background="#4F5563", borderwidth=1, relief=tk.SOLID)

    def _create_main_layout(self):
        """Creates the top bar and the editor page widgets."""
        top_bar_frame = ttk.Frame(self, padding=(10, 10, 10, 5))
        top_bar_frame.pack(side=tk.TOP, fill=tk.X)
        
        self.run_button = ttk.Button(top_bar_frame, text="Compile & Run", command=self.start_test_thread, style="Top.TButton")
        self.run_button.pack(side=tk.LEFT, padx=(0, 20))

        nav_frame = ttk.Frame(top_bar_frame)
        nav_frame.pack(side=tk.RIGHT)
        self.editor_button = ttk.Button(nav_frame, text="Editor", command=self.show_editor_page, style="ActiveNav.TButton")
        self.editor_button.pack(side=tk.LEFT)
        self.compare_button = ttk.Button(nav_frame, text="Compare Output", command=self.show_diff_page, style="InactiveNav.TButton")
        self.compare_button.pack(side=tk.LEFT)
        self.compare_button.config(state=tk.DISABLED)

        self.editor_frame.pack(fill="both", expand=True)
        main_pane = ttk.PanedWindow(self.editor_frame, orient=tk.HORIZONTAL)
        main_pane.pack(expand=True, fill=tk.BOTH, padx=10, pady=(0, 10))

        # --- C++ Code Section ---
        left_frame = ttk.Frame(main_pane, padding=(0, 5, 5, 5))
        left_frame.grid_rowconfigure(1, weight=1)
        left_frame.grid_columnconfigure(0, weight=1)
        
        cpp_header_frame = ttk.Frame(left_frame)
        cpp_header_frame.grid(row=0, column=0, sticky="ew", pady=(0, 5))
        ttk.Label(cpp_header_frame, text="C++ Code", font=self.title_font).pack(side=tk.LEFT)
        
        # Button Group for C++ Code
        tool_btn_frame_cpp = ttk.Frame(cpp_header_frame)
        tool_btn_frame_cpp.pack(side=tk.RIGHT)
        ttk.Button(tool_btn_frame_cpp, text="Paste", command=self.paste_to_cpp_code, style="Tool.TButton").pack(side=tk.LEFT, padx=(0, 1))
        ttk.Button(tool_btn_frame_cpp, text="Clear", command=self.clear_cpp_code, style="Tool.TButton").pack(side=tk.LEFT)
        
        self.cpp_code_text = scrolledtext.ScrolledText(left_frame, wrap=tk.WORD, font=self.code_font, bg="#1E1E1E", fg="#D4D4D4", insertbackground="white", relief=tk.FLAT, borderwidth=0, undo=True)
        self.cpp_code_text.grid(row=1, column=0, sticky="nsew")
        self.cpp_code_text.insert(tk.END, "#include <iostream>\n\nint main() {\n    // Your code here\n    std::cout << \"Hello, Satvik!\";\n    return 0;\n}")
        main_pane.add(left_frame, weight=2)

        # --- Right Pane ---
        right_pane = ttk.PanedWindow(main_pane, orient=tk.VERTICAL)
        main_pane.add(right_pane, weight=1)
        
        input_frame, self.input_text = self._create_text_area_with_header(right_pane, "Input", self.paste_to_input, self.clear_input)
        right_pane.add(input_frame, weight=1)
        
        desired_output_frame, self.desired_output_text = self._create_text_area_with_header(right_pane, "Desired Output", self.paste_to_desired_output, self.clear_desired_output)
        right_pane.add(desired_output_frame, weight=1)
        
        status_frame, self.status_text = self._create_text_area_with_header(right_pane, "Execution Status", None, self.clear_status)
        self.status_text.configure(state='disabled')
        right_pane.add(status_frame, weight=2)

        self.status_text.tag_config("SUCCESS", foreground="#4CAF50", font=(self.code_font.cget("family"), self.code_font.cget("size"), "bold"))
        self.status_text.tag_config("FAILURE", foreground="#F44336", font=(self.code_font.cget("family"), self.code_font.cget("size"), "bold"))
        self.status_text.tag_config("INFO", foreground="#61AFEF")
        self.status_text.tag_config("WARNING", foreground="#E5C07B")
        self.status_text.tag_config("ERROR", foreground="#F44336")

    def _setup_code_editor_feel(self):
        """Binds keys to provide a better code editing experience."""
        self.cpp_code_text.bind("<Tab>", self._handle_tab)
        self.cpp_code_text.bind("<Shift-Tab>", self._handle_shift_tab)
        self.cpp_code_text.bind("(", self._handle_key_pair)
        self.cpp_code_text.bind("[", self._handle_key_pair)
        self.cpp_code_text.bind("{", self._handle_key_pair)
        self.cpp_code_text.bind("'", self._handle_key_pair)
        self.cpp_code_text.bind('"', self._handle_key_pair)

    def _handle_tab(self, event):
        self.cpp_code_text.insert(tk.INSERT, " " * 4)
        return "break"

    def _handle_shift_tab(self, event):
        line_start = self.cpp_code_text.index(f"{tk.INSERT} linestart")
        line_end = self.cpp_code_text.index(f"{tk.INSERT} lineend")
        line = self.cpp_code_text.get(line_start, line_end)
        if line.startswith(" " * 4):
            self.cpp_code_text.delete(line_start, f"{line_start}+4c")
        return "break"

    def _handle_key_pair(self, event):
        key_map = {'(': ')', '[': ']', '{': '}', "'": "'", '"': '"'}
        char = event.char
        self.cpp_code_text.insert(tk.INSERT, char + key_map[char])
        self.cpp_code_text.mark_set(tk.INSERT, f"{tk.INSERT}-1c")
        return "break"

    def _create_text_area_with_header(self, parent, title, paste_command, clear_command):
        frame = ttk.Frame(parent, padding=5)
        frame.grid_rowconfigure(1, weight=1)
        frame.grid_columnconfigure(0, weight=1)
        header = ttk.Frame(frame)
        header.grid(row=0, column=0, sticky="ew", pady=(0, 5))
        ttk.Label(header, text=title, font=self.title_font).pack(side=tk.LEFT)
        
        # Button Group
        if paste_command or clear_command:
            tool_btn_frame = ttk.Frame(header)
            tool_btn_frame.pack(side=tk.RIGHT)
            if paste_command:
                ttk.Button(tool_btn_frame, text="Paste", command=paste_command, style="Tool.TButton").pack(side=tk.LEFT, padx=(0, 1))
            if clear_command:
                ttk.Button(tool_btn_frame, text="Clear", command=clear_command, style="Tool.TButton").pack(side=tk.LEFT)

        text_widget = scrolledtext.ScrolledText(frame, wrap=tk.WORD, font=self.code_font, bg="#1E1E1E", fg="#D4D4D4", insertbackground="white", relief=tk.FLAT, borderwidth=0, undo=True)
        text_widget.grid(row=1, column=0, sticky="nsew")
        return frame, text_widget

    def _create_diff_widgets(self):
        self.diff_frame.columnconfigure(0, weight=1)
        self.diff_frame.rowconfigure(1, weight=1)
        ttk.Label(self.diff_frame, text="Output Comparison", font=self.title_font, padding=(10, 10)).grid(row=0, column=0, sticky="w")
        self.diff_text = scrolledtext.ScrolledText(self.diff_frame, wrap=tk.WORD, font=self.code_font, bg="#1E1E1E", fg="#D4D4D4", relief=tk.FLAT, borderwidth=0, padx=10)
        self.diff_text.grid(row=1, column=0, sticky="nsew", padx=10, pady=(0,10))
        self.diff_text.configure(state='disabled')
        self.diff_text.tag_config("added", background="#284028", foreground="#79B77D")
        self.diff_text.tag_config("removed", background="#4B2828", foreground="#E57373")
        self.diff_text.tag_config("equal", foreground="#9DA5B4")

    # --- Page Switching ---
    def show_diff_page(self):
        self._update_diff_text()
        self.editor_frame.pack_forget()
        self.diff_frame.pack(fill="both", expand=True)
        self.editor_button.config(style="InactiveNav.TButton")
        self.compare_button.config(style="ActiveNav.TButton")

    def show_editor_page(self):
        self.diff_frame.pack_forget()
        self.editor_frame.pack(fill="both", expand=True)
        self.editor_button.config(style="ActiveNav.TButton")
        if self.compare_button['state'] != tk.DISABLED:
            self.compare_button.config(style="InactiveNav.TButton")

    def _update_diff_text(self):
        self.diff_text.configure(state='normal')
        self.diff_text.delete('1.0', tk.END)
        from_lines = self.last_desired_output.strip().splitlines()
        to_lines = self.last_generated_output.strip().splitlines()
        matcher = difflib.SequenceMatcher(None, from_lines, to_lines)
        if not from_lines and not to_lines:
            self.diff_text.insert(tk.END, "Both desired and actual outputs were empty.", "equal")
        elif from_lines == to_lines:
            self.diff_text.insert(tk.END, "Outputs match perfectly.\n\n", "added")
            for line in to_lines: self.diff_text.insert(tk.END, f"{line}\n", "equal")
        else:
            for tag, i1, i2, j1, j2 in matcher.get_opcodes():
                if tag == 'equal':
                    for line in from_lines[i1:i2]: self.diff_text.insert(tk.END, f"  {line}\n", "equal")
                if tag in ('delete', 'replace'):
                    for line in from_lines[i1:i2]: self.diff_text.insert(tk.END, f"- {line}\n", "removed")
                if tag in ('insert', 'replace'):
                    for line in to_lines[j1:j2]: self.diff_text.insert(tk.END, f"+ {line}\n", "added")
        self.diff_text.configure(state='disabled')

    # --- Paste and Clear Methods ---
    def paste_to_cpp_code(self):
        try: self.cpp_code_text.insert(tk.INSERT, self.clipboard_get())
        except tk.TclError: self.log_status("Clipboard is empty.", "WARNING")
    def clear_cpp_code(self): self.cpp_code_text.delete('1.0', tk.END)

    def paste_to_input(self):
        try: self.input_text.insert(tk.INSERT, self.clipboard_get())
        except tk.TclError: self.log_status("Clipboard is empty.", "WARNING")
    def clear_input(self): self.input_text.delete('1.0', tk.END)

    def paste_to_desired_output(self):
        try: self.desired_output_text.insert(tk.INSERT, self.clipboard_get())
        except tk.TclError: self.log_status("Clipboard is empty.", "WARNING")
    def clear_desired_output(self): self.desired_output_text.delete('1.0', tk.END)
    
    def clear_status(self):
        self.status_text.configure(state='normal')
        self.status_text.delete('1.0', tk.END)
        self.status_text.configure(state='disabled')

    def log_status(self, message, tag=""):
        self.status_text.configure(state='normal')
        self.status_text.insert(tk.END, message + "\n", tag)
        self.status_text.see(tk.END)
        self.status_text.configure(state='disabled')

    # --- Main Execution Logic ---
    def start_test_thread(self):
        self.run_button.config(state=tk.DISABLED, text="Running...")
        self.compare_button.config(state=tk.DISABLED)
        self.clear_status()
        thread = threading.Thread(target=self.run_test)
        thread.daemon = True
        thread.start()

    def run_test(self):
        cpp_code = self.cpp_code_text.get("1.0", tk.END)
        input_data = self.input_text.get("1.0", tk.END)
        self.last_desired_output = self.desired_output_text.get("1.0", tk.END)
        self.last_generated_output = ""
        creation_flags = subprocess.CREATE_NO_WINDOW if os.name == 'nt' else 0

        with tempfile.TemporaryDirectory() as temp_dir:
            cpp_source_path = os.path.join(temp_dir, "main.cpp")
            executable_path = os.path.join(temp_dir, "main_executable")
            with open(cpp_source_path, "w", encoding='utf-8') as f: f.write(cpp_code)
            
            self.log_status("--- Starting Test ---", "INFO")
            self.log_status("1. Compiling...", "INFO")
            compile_command = ["g++", "-O2", "-Wall", cpp_source_path, "-o", executable_path]

            try:
                compile_result = subprocess.run(compile_command, capture_output=True, text=True, check=True, timeout=10, creationflags=creation_flags)
                if compile_result.stderr: self.log_status("\n--- Compiler Warnings ---\n" + compile_result.stderr, "WARNING")
                self.log_status("   Compilation successful.", "INFO")
            except (FileNotFoundError, subprocess.TimeoutExpired, subprocess.CalledProcessError) as e:
                if isinstance(e, FileNotFoundError): msg = "Error: 'g++' not found. Is it in your PATH?"
                elif isinstance(e, subprocess.TimeoutExpired): msg = "Error: Compilation timed out."
                else: msg = f"Error: Compilation failed.\n\n--- Compiler Error ---\n{e.stderr}"
                self.log_status(msg, "ERROR")
                self.finalize_ui()
                return

            self.log_status("\n2. Running executable...", "INFO")
            try:
                run_result = subprocess.run([executable_path], input=input_data, capture_output=True, text=True, check=True, timeout=5, creationflags=creation_flags)
                self.last_generated_output = run_result.stdout
                if run_result.stderr: self.log_status("\n--- Runtime Messages (stderr) ---\n" + run_result.stderr, "WARNING")
            except (subprocess.TimeoutExpired, subprocess.CalledProcessError) as e:
                if isinstance(e, subprocess.TimeoutExpired): msg = "Error: Execution timed out (> 5s)."
                else: msg = f"Error: Program crashed (exit code {e.returncode}).\n\n--- Runtime Error ---\n{e.stderr}"
                self.log_status(msg, "ERROR")
                self.finalize_ui()
                return
            
            self.log_status("\n3. Verifying output...", "INFO")
            norm_gen = self.last_generated_output.strip().replace('\r\n', '\n')
            norm_des = self.last_desired_output.strip().replace('\r\n', '\n')
            if norm_gen == norm_des:
                self.log_status("\n✅ --- Result: Good Job Satvik --- ✅", "SUCCESS")
            else:
                self.log_status("\n❌ --- Result: Try again Satvik --- ❌", "FAILURE")
                self.log_status("Outputs do not match. Click 'Compare Output' for details.", "INFO")

        self.finalize_ui(enable_diff=True)

    def finalize_ui(self, enable_diff=False):
        def _update():
            self.run_button.config(state=tk.NORMAL, text="Compile & Run")
            if enable_diff:
                self.compare_button.config(state=tk.NORMAL)
                if self.editor_frame.winfo_ismapped():
                    self.compare_button.config(style="InactiveNav.TButton")
                else:
                    self.compare_button.config(style="ActiveNav.TButton")

        self.after(0, _update)

if __name__ == "__main__":
    app = CppGraderApp()
    app.mainloop()