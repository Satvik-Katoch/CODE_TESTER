import tkinter as tk
from tkinter import ttk, scrolledtext, font, filedialog
import subprocess
import os
import tempfile
import threading
import difflib

class CppGraderApp(tk.Tk):
    """
    An advanced GUI to compile, run, and test C++ code, featuring a
    professional, side-by-side, GitHub-style diff viewer and
    support for running from a file path or the text editor.
    """
    def __init__(self):
        super().__init__()
        self.title("Satvik's C++ Code Grader")
        self.geometry("1200x800")
        self.configure(bg="#282C34")

        self.last_generated_output = ""
        self.last_desired_output = ""
        
        self._configure_styles()
        
        # --- NEW: Variables to control code source ---
        self.cpp_file_path_var = tk.StringVar()
        self.use_file_path_var = tk.IntVar(value=0) # 0 = use text, 1 = use file
        
        self.editor_frame = ttk.Frame(self)
        self.diff_frame = ttk.Frame(self)

        self._create_main_layout()
        self._create_diff_widgets()
        
        self._setup_code_editor_feel()
        self._toggle_code_source() # Set initial widget states

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
        
        # --- NEW: Style for Checkbutton ---
        self.style.configure("TCheckbutton", 
                             background="#282C34", 
                             foreground="white", 
                             font=self.default_font,
                             indicatorcolor="black") # Fix for dark mode indicator
        self.style.map("TCheckbutton",
                       indicatorcolor=[('selected', '#007ACC'), ('!selected', '#555')],
                       foreground=[('active', 'white')])

        
        self.style.configure("Top.TButton", font=("Segoe UI", 10, "bold"), background="#007ACC", foreground="white", borderwidth=0, padding=(10, 5))
        self.style.map("Top.TButton", background=[('active', '#005f9e')])
        
        self.style.configure("Tool.TButton", font=("Segoe UI", 8), background="#4F5563", foreground="white", borderwidth=0, padding=(5, 2))
        self.style.map("Tool.TButton", background=[('active', '#6A7180'), ('disabled', '#333')])

        nav_font = ("Segoe UI", 9)
        self.style.configure("Nav.TButton", font=nav_font, padding=(10, 4), borderwidth=1, relief=tk.FLAT)
        self.style.map("Nav.TButton", foreground=[('disabled', '#777'), ('!disabled', 'white')], background=[('disabled', '#333'), ('active', '#5c6370')])
        self.style.configure("ActiveNav.TButton", background="#007ACC", foreground="white")
        self.style.configure("InactiveNav.TButton", background="#4F5563", foreground="white")

        self.style.configure("TPanedWindow", background="#282C34")
        self.style.configure("Sash", background="#4F5563", borderwidth=1, relief=tk.SOLID)
        
        self.style.configure("TEntry", 
                             fieldbackground="#1E1E1E", 
                             foreground="#D4D4D4", 
                             insertbackground="white",
                             borderwidth=0,
                             relief=tk.FLAT,
                             padding=3)
        self.style.map("TEntry",
                       foreground=[('disabled', '#555')],
                       fieldbackground=[('disabled', '#2a2d32')])
        
        self.style.configure("LineNumbers.TLabel", background="#2a2d32", foreground="#6c727d", padding=(5,0), font=self.code_font)

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

        # --- UPDATED: Create C++ pane manually to add custom widgets ---
        left_frame = self._create_cpp_pane(main_pane)
        main_pane.add(left_frame, weight=2)
        # --- End of update ---

        right_pane = ttk.PanedWindow(main_pane, orient=tk.VERTICAL)
        main_pane.add(right_pane, weight=1)
        
        input_frame, self.input_text = self._create_editor_pane(right_pane, "Input", self.paste_to_input, self.clear_input)
        right_pane.add(input_frame, weight=1)
        
        desired_output_frame, self.desired_output_text = self._create_editor_pane(right_pane, "Desired Output", self.paste_to_desired_output, self.clear_desired_output)
        right_pane.add(desired_output_frame, weight=1)
        
        status_frame, self.status_text = self._create_editor_pane(right_pane, "Execution Status", None, self.clear_status)
        self.status_text.configure(state='disabled')
        right_pane.add(status_frame, weight=2)
        
        self._configure_status_tags()

    # --- NEW: Method to create the specialized C++ pane ---
    def _create_cpp_pane(self, parent):
        frame = ttk.Frame(parent, padding=5)
        frame.grid_rowconfigure(2, weight=1) # Row 2 (text editor) expands
        frame.grid_columnconfigure(0, weight=1)
        
        # Row 0: Title and Paste/Clear buttons
        header = ttk.Frame(frame)
        header.grid(row=0, column=0, sticky="ew", pady=(0, 5))
        ttk.Label(header, text="C++ Code", font=self.title_font).pack(side=tk.LEFT, padx=(0, 10))
        
        tool_btn_frame = ttk.Frame(header)
        tool_btn_frame.pack(side=tk.RIGHT)
        ttk.Button(tool_btn_frame, text="Paste", command=self.paste_to_cpp_code, style="Tool.TButton").pack(side=tk.LEFT, padx=(0, 1))
        ttk.Button(tool_btn_frame, text="Clear", command=self.clear_cpp_code, style="Tool.TButton").pack(side=tk.LEFT)

        # Row 1: File Path Input
        file_input_frame = ttk.Frame(frame)
        file_input_frame.grid(row=1, column=0, sticky="ew", pady=5)
        
        self.check_button = ttk.Checkbutton(file_input_frame, 
                                            text="Use File Path:", 
                                            variable=self.use_file_path_var, 
                                            command=self._toggle_code_source)
        self.check_button.pack(side=tk.LEFT, padx=(0, 5))
        
        self.file_path_entry = ttk.Entry(file_input_frame, 
                                         textvariable=self.cpp_file_path_var, 
                                         font=self.code_font)
        self.file_path_entry.pack(side=tk.LEFT, fill=tk.X, expand=True, padx=5)
        
        self.browse_button = ttk.Button(file_input_frame, 
                                        text="Browse...", 
                                        command=self._browse_for_cpp_file, 
                                        style="Tool.TButton")
        self.browse_button.pack(side=tk.LEFT)

        # Row 2: Text Editor
        self.cpp_code_text = scrolledtext.ScrolledText(frame, wrap=tk.WORD, font=self.code_font, bg="#1E1E1E", fg="#D4D4D4", insertbackground="white", relief=tk.FLAT, borderwidth=0, undo=True, padx=5, pady=5)
        self.cpp_code_text.grid(row=2, column=0, sticky="nsew")
        self.cpp_code_text.insert(tk.END, "#include <iostream>\n\nint main() {\n    // Your code here\n    std::cout << \"Hello, Satvik!\";\n    return 0;\n}")

        return frame
    # --- End of new method ---

    # --- NEW: Method to toggle between file and text input ---
    def _toggle_code_source(self):
        if self.use_file_path_var.get() == 1: # Use File Path
            self.file_path_entry.config(state=tk.NORMAL)
            self.browse_button.config(state=tk.NORMAL)
            self.cpp_code_text.config(state=tk.DISABLED, bg="#2a2d32") # Darker bg when disabled
        else: # Use Text Editor
            self.file_path_entry.config(state=tk.DISABLED)
            self.browse_button.config(state=tk.DISABLED)
            self.cpp_code_text.config(state=tk.NORMAL, bg="#1E1E1E")
    # --- End of new method ---

    # --- UPDATED: Browse method now only sets the path variable ---
    def _browse_for_cpp_file(self):
        file_path = filedialog.askopenfilename(
            title="Select C++ File",
            filetypes=[("C++ Files", "*.cpp"), ("Header Files", "*.h *.hpp"), ("All Files", "*.*")]
        )
        if not file_path:
            return
        
        self.cpp_file_path_var.set(file_path)
        self.log_status(f"File selected: {file_path}", "INFO")
    # --- End of update ---

    def _configure_status_tags(self):
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
        current_chars = self.cpp_code_text.get(line_start, f"{line_start} + 4 chars")
        spaces_to_remove = 0
        for char in current_chars:
            if char == " ": spaces_to_remove += 1
            else: break
        if spaces_to_remove > 0: self.cpp_code_text.delete(line_start, f"{line_start} + {spaces_to_remove} chars")
        return "break"

    def _handle_key_pair(self, event):
        key_map = {'(': ')', '[': ']', '{': '}', "'": "'", '"': '"'}
        char = event.char
        self.cpp_code_text.insert(tk.INSERT, char + key_map[char])
        self.cpp_code_text.mark_set(tk.INSERT, f"{tk.INSERT}-1c")
        return "break"

    def _create_editor_pane(self, parent, title, paste_command, clear_command):
        """Generic helper for creating Input, Output, and Status panes."""
        frame = ttk.Frame(parent, padding=5)
        frame.grid_rowconfigure(1, weight=1)
        frame.grid_columnconfigure(0, weight=1)
        
        header = ttk.Frame(frame)
        header.grid(row=0, column=0, sticky="ew", pady=(0, 5))
        ttk.Label(header, text=title, font=self.title_font).pack(side=tk.LEFT)
        
        if paste_command or clear_command:
            tool_btn_frame = ttk.Frame(header)
            tool_btn_frame.pack(side=tk.RIGHT)
            if paste_command: ttk.Button(tool_btn_frame, text="Paste", command=paste_command, style="Tool.TButton").pack(side=tk.LEFT, padx=(0, 1))
            if clear_command: ttk.Button(tool_btn_frame, text="Clear", command=clear_command, style="Tool.TButton").pack(side=tk.LEFT)

        text_widget = scrolledtext.ScrolledText(frame, wrap=tk.WORD, font=self.code_font, bg="#1E1E1E", fg="#D4D4D4", insertbackground="white", relief=tk.FLAT, borderwidth=0, undo=True, padx=5, pady=5)
        text_widget.grid(row=1, column=0, sticky="nsew")
        return frame, text_widget

    def _create_diff_widgets(self):
        """Creates the side-by-side diff view components."""
        self.diff_frame.grid_rowconfigure(1, weight=1)
        self.diff_frame.grid_columnconfigure(0, weight=1, uniform='group1')
        self.diff_frame.grid_columnconfigure(1, weight=1, uniform='group1')

        ttk.Label(self.diff_frame, text="Expected Output", font=self.title_font, padding=10).grid(row=0, column=0, sticky="w")
        ttk.Label(self.diff_frame, text="Your Output", font=self.title_font, padding=10).grid(row=0, column=1, sticky="w")

        left_pane = ttk.Frame(self.diff_frame)
        left_pane.grid(row=1, column=0, sticky='nsew', padx=(10,0), pady=(0,10))
        left_pane.grid_rowconfigure(0, weight=1)
        left_pane.grid_columnconfigure(1, weight=1)

        right_pane = ttk.Frame(self.diff_frame)
        right_pane.grid(row=1, column=1, sticky='nsew', padx=(5,10), pady=(0,10))
        right_pane.grid_rowconfigure(0, weight=1)
        right_pane.grid_columnconfigure(1, weight=1)

        self.line_nums_left = tk.Text(left_pane, width=4, padx=5, wrap=tk.NONE, font=self.code_font, bg='#2a2d32', fg='#6c727d', relief=tk.FLAT, bd=0)
        self.line_nums_left.grid(row=0, column=0, sticky='ns')
        self.line_nums_right = tk.Text(right_pane, width=4, padx=5, wrap=tk.NONE, font=self.code_font, bg='#2a2d32', fg='#6c727d', relief=tk.FLAT, bd=0)
        self.line_nums_right.grid(row=0, column=0, sticky='ns')

        self.diff_left = tk.Text(left_pane, wrap=tk.WORD, font=self.code_font, bg="#1E1E1E", fg="#D4D4D4", relief=tk.FLAT, bd=0, padx=5)
        self.diff_left.grid(row=0, column=1, sticky='nsew')
        self.diff_right = tk.Text(right_pane, wrap=tk.WORD, font=self.code_font, bg="#1E1E1E", fg="#D4D4D4", relief=tk.FLAT, bd=0, padx=5)
        self.diff_right.grid(row=0, column=1, sticky='nsew')
        
        scrollbar = ttk.Scrollbar(right_pane, command=self._on_scroll)
        scrollbar.grid(row=0, column=2, sticky='ns')
        self.diff_left.config(yscrollcommand=scrollbar.set)
        self.diff_right.config(yscrollcommand=scrollbar.set)
        
        self.diff_left.tag_config("removed", background="#5c2c2c")
        self.diff_right.tag_config("added", background="#2d572d")
        self.diff_left.tag_config("empty", background="#2c2f33")
        self.diff_right.tag_config("empty", background="#2c2f33")


    def _on_scroll(self, *args):
        """Synchronizes scrolling across all four text widgets."""
        self.diff_left.yview(*args)
        self.diff_right.yview(*args)
        self.line_nums_left.yview(*args)
        self.line_nums_right.yview(*args)

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
        if self.compare_button['state'] != tk.DISABLED: self.compare_button.config(style="InactiveNav.TButton")

    def _update_diff_text(self):
        """FIX: Complete rewrite of diff logic for proper side-by-side alignment."""
        widgets = [self.diff_left, self.diff_right, self.line_nums_left, self.line_nums_right]
        for w in widgets: w.config(state=tk.NORMAL)
        for w in widgets: w.delete('1.0', tk.END)

        from_lines = self.last_desired_output.strip().splitlines()
        to_lines = self.last_generated_output.strip().splitlines()
        matcher = difflib.SequenceMatcher(None, from_lines, to_lines)
        
        left_num, right_num = 1, 1
        for tag, i1, i2, j1, j2 in matcher.get_opcodes():
            if tag == 'equal':
                for line in from_lines[i1:i2]:
                    self.line_nums_left.insert(tk.END, f"{left_num}\n")
                    self.line_nums_right.insert(tk.END, f"{right_num}\n")
                    self.diff_left.insert(tk.END, f"{line}\n")
                    self.diff_right.insert(tk.END, f"{line}\n")
                    left_num += 1
                    right_num += 1
            elif tag == 'delete':
                for line in from_lines[i1:i2]:
                    self.line_nums_left.insert(tk.END, f"{left_num}\n")
                    self.line_nums_right.insert(tk.END, "\n")
                    self.diff_left.insert(tk.END, f"{line}\n", "removed")
                    self.diff_right.insert(tk.END, "\n", "empty")
                    left_num += 1
            elif tag == 'insert':
                for line in to_lines[j1:j2]:
                    self.line_nums_left.insert(tk.END, "\n")
                    self.line_nums_right.insert(tk.END, f"{right_num}\n")
                    self.diff_left.insert(tk.END, "\n", "empty")
                    self.diff_right.insert(tk.END, f"{line}\n", "added")
                    right_num += 1
            elif tag == 'replace':
                deleted_lines = from_lines[i1:i2]
                added_lines = to_lines[j1:j2]
                max_len = max(len(deleted_lines), len(added_lines))
                for i in range(max_len):
                    if i < len(deleted_lines):
                        self.line_nums_left.insert(tk.END, f"{left_num}\n")
                        self.diff_left.insert(tk.END, f"{deleted_lines[i]}\n", "removed")
                        left_num += 1
                    else:
                        self.line_nums_left.insert(tk.END, "\n")
                        self.diff_left.insert(tk.END, "\n", "empty")

                    if i < len(added_lines):
                        self.line_nums_right.insert(tk.END, f"{right_num}\n")
                        self.diff_right.insert(tk.END, f"{added_lines[i]}\n", "added")
                        right_num += 1
                    else:
                        self.line_nums_right.insert(tk.END, "\n")
                        self.diff_right.insert(tk.END, "\n", "empty")

        for w in widgets: w.config(state=tk.DISABLED)

    def paste_to_cpp_code(self):
        try: self.cpp_code_text.insert(tk.INSERT, self.clipboard_get())
        except tk.TclError: self.log_status("Clipboard is empty.", "WARNING")
    
    def clear_cpp_code(self): 
        self.cpp_code_text.delete('1.0', tk.END)
        # Note: We don't clear the file path, as it's a separate input

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

    def start_test_thread(self):
        self.run_button.config(state=tk.DISABLED, text="Running...")
        self.compare_button.config(state=tk.DISABLED)
        self.clear_status()
        thread = threading.Thread(target=self.run_test)
        thread.daemon = True
        thread.start()

    # --- CRITICAL UPDATE: run_test now decides which code source to use ---
    def run_test(self):
        input_data = self.input_text.get("1.0", tk.END)
        self.last_desired_output = self.desired_output_text.get("1.0", tk.END)
        self.last_generated_output = ""
        creation_flags = subprocess.CREATE_NO_WINDOW if os.name == 'nt' else 0

        with tempfile.TemporaryDirectory() as temp_dir:
            executable_path = os.path.join(temp_dir, "main_executable")
            compile_command = []
            
            self.log_status("--- Starting Test ---", "INFO")

            # --- DECISION LOGIC ---
            if self.use_file_path_var.get() == 1:
                # --- 1. Use code from FILE PATH ---
                cpp_source_path = self.cpp_file_path_var.get().strip()

                if not cpp_source_path:
                    self.log_status("Error: 'Use File Path' is checked, but no file path is provided.", "ERROR")
                    self.finalize_ui()
                    return
                
                if not os.path.exists(cpp_source_path):
                    self.log_status(f"Error: File not found at path:\n{cpp_source_path}", "ERROR")
                    self.finalize_ui()
                    return

                self.log_status(f"Using code from file: {os.path.basename(cpp_source_path)}", "INFO")
                compile_command = ["g++", "-O2", "-Wall", cpp_source_path, "-o", executable_path]

            else:
                # --- 2. Use code from TEXT EDITOR ---
                cpp_code = self.cpp_code_text.get("1.0", tk.END)
                if not cpp_code.strip():
                    self.log_status("Error: Code editor is empty. Nothing to compile.", "ERROR")
                    self.finalize_ui()
                    return

                cpp_source_path = os.path.join(temp_dir, "main.cpp")
                with open(cpp_source_path, "w", encoding='utf-8') as f: 
                    f.write(cpp_code)
                
                self.log_status("Using code from text editor.", "INFO")
                compile_command = ["g++", "-O2", "-Wall", cpp_source_path, "-o", executable_path]
            # --- END DECISION LOGIC ---
            
            
            self.log_status("1. Compiling...", "INFO")
            try:
                compile_result = subprocess.run(compile_command, capture_output=True, text=True, check=True, timeout=10, creationflags=creation_flags)
                if compile_result.stderr: self.log_status("\n--- Compiler Warnings ---\n" + compile_result.stderr, "WARNING")
                self.log_status("   Compilation successful.", "INFO")
            except (FileNotFoundError, subprocess.TimeoutExpired, subprocess.CalledProcessError) as e:
                msg = ""
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
                if isinstance(e, subprocess.TimeoutExpired): 
                    msg = "Error: Execution timed out (> 5s)."
                    self.last_generated_output = e.stdout if e.stdout else ""
                else: 
                    msg = f"Error: Program crashed (exit code {e.returncode}).\n\n--- Runtime Error ---\n{e.stderr}"
                    self.last_generated_output = e.stdout
                self.log_status(msg, "ERROR")
                self.finalize_ui(enable_diff=True) 
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