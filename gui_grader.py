import tkinter as tk
from tkinter import ttk, scrolledtext, font, filedialog
import subprocess
import os
import tempfile
import threading
import difflib
import time

class CppGraderApp(tk.Tk):
    """
    An advanced GUI to compile, run, and test C++ code, featuring:
    1. Main Editor & Single Case Runner
    2. Professional Diff Viewer (Smart Sync & Raw Mode)
    3. Stress Testing (Generator vs Brute Force vs Optimized)
    """
    def __init__(self):
        super().__init__()
        self.title("Satvik's C++ Code Grader")
        self.geometry("1300x850")
        self.configure(bg="#282C34")

        self.last_generated_output = ""
        self.last_desired_output = ""
        
        # --- State Variables ---
        self.stop_stress_event = threading.Event()
        self.is_stress_running = False

        self._configure_styles()
        
        # --- Variables to control code source (Main Tab) ---
        self.cpp_file_path_var = tk.StringVar()
        self.use_file_path_var = tk.IntVar(value=0) # 0 = use text, 1 = use file
        
        # --- Variables for Stress Test ---
        self.gen_file_path_var = tk.StringVar()
        self.brute_file_path_var = tk.StringVar()
        self.stress_iterations_var = tk.IntVar(value=100)

        # --- Variables for Diff Viewer ---
        self.diff_smart_sync_var = tk.BooleanVar(value=True) # Defaults to Smart Sync (Git style)

        # --- Frames ---
        self.editor_frame = ttk.Frame(self)
        self.diff_frame = ttk.Frame(self)
        self.stress_frame = ttk.Frame(self)

        self._create_main_layout()
        self._create_diff_widgets()
        self._create_stress_widgets()
        
        self._setup_code_editor_feel()
        self._toggle_code_source()

        self.show_editor_page()

    def _configure_styles(self):
        """Sets up all the ttk styles and fonts for the application."""
        self.style = ttk.Style(self)
        self.style.theme_use('clam')

        self.default_font = font.nametofont("TkDefaultFont")
        self.default_font.configure(family="Segoe UI", size=11)
        self.title_font = font.Font(family="Segoe UI", size=13, weight="bold")
        self.code_font = font.Font(family="Consolas", size=11)
        self.status_font = font.Font(family="Segoe UI", size=10)

        self.style.configure("TLabel", background="#282C34", foreground="white", font=self.default_font)
        self.style.configure("TFrame", background="#282C34")
        
        self.style.configure("TCheckbutton", 
                             background="#282C34", 
                             foreground="white", 
                             font=self.default_font,
                             indicatorcolor="black")
        self.style.map("TCheckbutton",
                       indicatorcolor=[('selected', '#007ACC'), ('!selected', '#555')],
                       foreground=[('active', 'white')])
        
        self.style.configure("Top.TButton", font=("Segoe UI", 10, "bold"), background="#007ACC", foreground="white", borderwidth=0, padding=(10, 5))
        self.style.map("Top.TButton", background=[('active', '#005f9e'), ('disabled', '#333')])
        
        self.style.configure("Stop.TButton", font=("Segoe UI", 10, "bold"), background="#E06C75", foreground="white", borderwidth=0, padding=(10, 5))
        self.style.map("Stop.TButton", background=[('active', '#c94f59'), ('disabled', '#333')])

        self.style.configure("Tool.TButton", font=("Segoe UI", 8), background="#4F5563", foreground="white", borderwidth=0, padding=(5, 2))
        self.style.map("Tool.TButton", background=[('active', '#6A7180'), ('disabled', '#333')])

        nav_font = ("Segoe UI", 9)
        self.style.configure("Nav.TButton", font=nav_font, padding=(10, 4), borderwidth=1, relief=tk.FLAT)
        self.style.map("Nav.TButton", foreground=[('disabled', '#777'), ('!disabled', 'white')], background=[('disabled', '#333'), ('active', '#5c6370')])
        self.style.configure("ActiveNav.TButton", background="#007ACC", foreground="white")
        self.style.configure("InactiveNav.TButton", background="#4F5563", foreground="white")

        self.style.configure("TPanedWindow", background="#282C34")
        self.style.configure("Sash", background="#4F5563", borderwidth=1, relief=tk.SOLID)
        
        self.style.configure("TEntry", fieldbackground="#1E1E1E", foreground="#D4D4D4", insertbackground="white", borderwidth=0, relief=tk.FLAT, padding=3)
        self.style.map("TEntry", foreground=[('disabled', '#555')], fieldbackground=[('disabled', '#2a2d32')])
        
        self.style.configure("TSpinbox", fieldbackground="#1E1E1E", foreground="#D4D4D4", arrowcolor="white", borderwidth=0)

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

        # --- NEW: Stress Test Button ---
        self.stress_button = ttk.Button(nav_frame, text="Stress Test", command=self.show_stress_page, style="InactiveNav.TButton")
        self.stress_button.pack(side=tk.LEFT)

        self.editor_frame.pack(fill="both", expand=True)
        main_pane = ttk.PanedWindow(self.editor_frame, orient=tk.HORIZONTAL)
        main_pane.pack(expand=True, fill=tk.BOTH, padx=10, pady=(0, 10))

        left_frame = self._create_cpp_pane(main_pane)
        main_pane.add(left_frame, weight=2)

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

    def _create_cpp_pane(self, parent):
        frame = ttk.Frame(parent, padding=5)
        frame.grid_rowconfigure(2, weight=1)
        frame.grid_columnconfigure(0, weight=1)
        
        header = ttk.Frame(frame)
        header.grid(row=0, column=0, sticky="ew", pady=(0, 5))
        ttk.Label(header, text="C++ Code (Optimized)", font=self.title_font).pack(side=tk.LEFT, padx=(0, 10))
        
        tool_btn_frame = ttk.Frame(header)
        tool_btn_frame.pack(side=tk.RIGHT)
        ttk.Button(tool_btn_frame, text="Paste", command=self.paste_to_cpp_code, style="Tool.TButton").pack(side=tk.LEFT, padx=(0, 1))
        ttk.Button(tool_btn_frame, text="Clear", command=self.clear_cpp_code, style="Tool.TButton").pack(side=tk.LEFT)

        file_input_frame = ttk.Frame(frame)
        file_input_frame.grid(row=1, column=0, sticky="ew", pady=5)
        
        self.check_button = ttk.Checkbutton(file_input_frame, text="Use File Path:", variable=self.use_file_path_var, command=self._toggle_code_source)
        self.check_button.pack(side=tk.LEFT, padx=(0, 5))
        
        self.file_path_entry = ttk.Entry(file_input_frame, textvariable=self.cpp_file_path_var, font=self.code_font)
        self.file_path_entry.pack(side=tk.LEFT, fill=tk.X, expand=True, padx=5)
        
        self.browse_button = ttk.Button(file_input_frame, text="Browse...", command=self._browse_for_cpp_file, style="Tool.TButton")
        self.browse_button.pack(side=tk.LEFT)

        self.cpp_code_text = scrolledtext.ScrolledText(frame, wrap=tk.WORD, font=self.code_font, bg="#1E1E1E", fg="#D4D4D4", insertbackground="white", relief=tk.FLAT, borderwidth=0, undo=True, padx=5, pady=5)
        self.cpp_code_text.grid(row=2, column=0, sticky="nsew")
        self.cpp_code_text.insert(tk.END, "#include <iostream>\n\nint main() {\n    // Your optimized code here\n    int n;\n    std::cin >> n;\n    std::cout << n * 2;\n    return 0;\n}")

        return frame

    # --- NEW: Stress Test Widgets ---
    def _create_stress_widgets(self):
        self.stress_frame.pack_forget() # Initially hidden
        
        # --- Top Section: Configuration ---
        config_frame = ttk.Frame(self.stress_frame, padding=15)
        config_frame.pack(side=tk.TOP, fill=tk.X)
        
        # Grid layout for config
        config_frame.columnconfigure(1, weight=1)
        
        # Generator Selection
        ttk.Label(config_frame, text="Generator Script (C++):", width=20).grid(row=0, column=0, sticky="w", pady=5)
        ttk.Entry(config_frame, textvariable=self.gen_file_path_var, font=self.code_font).grid(row=0, column=1, sticky="ew", padx=5)
        ttk.Button(config_frame, text="Browse", style="Tool.TButton", command=lambda: self._browse_file(self.gen_file_path_var)).grid(row=0, column=2, padx=5)

        # Brute Force Selection
        ttk.Label(config_frame, text="Brute Force (C++):", width=20).grid(row=1, column=0, sticky="w", pady=5)
        ttk.Entry(config_frame, textvariable=self.brute_file_path_var, font=self.code_font).grid(row=1, column=1, sticky="ew", padx=5)
        ttk.Button(config_frame, text="Browse", style="Tool.TButton", command=lambda: self._browse_file(self.brute_file_path_var)).grid(row=1, column=2, padx=5)

        # Controls
        ctrl_frame = ttk.Frame(config_frame, padding=(0, 10, 0, 0))
        ctrl_frame.grid(row=2, column=0, columnspan=3, sticky="ew")
        
        ttk.Label(ctrl_frame, text="Iterations:").pack(side=tk.LEFT)
        ttk.Spinbox(ctrl_frame, from_=1, to=10000, textvariable=self.stress_iterations_var, width=10).pack(side=tk.LEFT, padx=5)
        
        self.stress_run_btn = ttk.Button(ctrl_frame, text="Start Stress Test", style="Top.TButton", command=self.start_stress_thread)
        self.stress_run_btn.pack(side=tk.LEFT, padx=20)
        
        self.stress_stop_btn = ttk.Button(ctrl_frame, text="Stop", style="Stop.TButton", command=self.stop_stress_test, state=tk.DISABLED)
        self.stress_stop_btn.pack(side=tk.LEFT)

        self.stress_status_lbl = ttk.Label(ctrl_frame, text="Ready", font=self.status_font, foreground="#E5C07B")
        self.stress_status_lbl.pack(side=tk.LEFT, padx=20)

        # --- Bottom Section: Results (3 Columns) ---
        results_pane = ttk.PanedWindow(self.stress_frame, orient=tk.HORIZONTAL)
        results_pane.pack(side=tk.TOP, expand=True, fill=tk.BOTH, padx=10, pady=10)

        # 1. Input
        self.stress_input_frame, self.stress_input_text = self._create_editor_pane(results_pane, "1. Input (Test Case)", None, None)
        results_pane.add(self.stress_input_frame, weight=1)

        # 2. Expected Output
        self.stress_expected_frame, self.stress_expected_text = self._create_editor_pane(results_pane, "2. Expected Output (Brute)", None, None)
        results_pane.add(self.stress_expected_frame, weight=1)

        # 3. Actual Output
        self.stress_actual_frame, self.stress_actual_text = self._create_editor_pane(results_pane, "3. Actual Output (Optimized)", None, None)
        results_pane.add(self.stress_actual_frame, weight=1)


    def _browse_file(self, var_store):
        file_path = filedialog.askopenfilename(filetypes=[("C++ Files", "*.cpp"), ("All Files", "*.*")])
        if file_path:
            var_store.set(file_path)

    # --- Page Navigation ---
    def show_editor_page(self):
        self.stress_frame.pack_forget()
        self.diff_frame.pack_forget()
        self.editor_frame.pack(fill="both", expand=True)
        self._update_nav_styles("editor")

    def show_diff_page(self):
        self._update_diff_text()
        self.stress_frame.pack_forget()
        self.editor_frame.pack_forget()
        self.diff_frame.pack(fill="both", expand=True)
        self._update_nav_styles("diff")

    def show_stress_page(self):
        self.editor_frame.pack_forget()
        self.diff_frame.pack_forget()
        self.stress_frame.pack(fill="both", expand=True)
        self._update_nav_styles("stress")

    def _update_nav_styles(self, active):
        bs = "InactiveNav.TButton"
        self.editor_button.config(style="ActiveNav.TButton" if active == "editor" else bs)
        self.compare_button.config(style="ActiveNav.TButton" if active == "diff" else bs)
        self.stress_button.config(style="ActiveNav.TButton" if active == "stress" else bs)

    # --- Common UI Methods ---
    def _toggle_code_source(self):
        if self.use_file_path_var.get() == 1:
            self.file_path_entry.config(state=tk.NORMAL)
            self.browse_button.config(state=tk.NORMAL)
            self.cpp_code_text.config(state=tk.DISABLED, bg="#2a2d32")
        else:
            self.file_path_entry.config(state=tk.DISABLED)
            self.browse_button.config(state=tk.DISABLED)
            self.cpp_code_text.config(state=tk.NORMAL, bg="#1E1E1E")

    def _browse_for_cpp_file(self):
        file_path = filedialog.askopenfilename(title="Select C++ File", filetypes=[("C++ Files", "*.cpp"), ("All Files", "*.*")])
        if file_path:
            self.cpp_file_path_var.set(file_path)

    def _configure_status_tags(self):
        self.status_text.tag_config("SUCCESS", foreground="#4CAF50", font=(self.code_font.cget("family"), self.code_font.cget("size"), "bold"))
        self.status_text.tag_config("FAILURE", foreground="#F44336", font=(self.code_font.cget("family"), self.code_font.cget("size"), "bold"))
        self.status_text.tag_config("INFO", foreground="#61AFEF")
        self.status_text.tag_config("WARNING", foreground="#E5C07B")
        self.status_text.tag_config("ERROR", foreground="#F44336")

    def _setup_code_editor_feel(self):
        self.cpp_code_text.bind("<Tab>", self._handle_tab)
        self.cpp_code_text.bind("<Shift-Tab>", self._handle_shift_tab)
        pairs = {'(': ')', '[': ']', '{': '}', "'": "'", '"': '"'}
        for start in pairs: self.cpp_code_text.bind(start, self._handle_key_pair)

    def _handle_tab(self, event):
        self.cpp_code_text.insert(tk.INSERT, " " * 4)
        return "break"
    def _handle_shift_tab(self, event): return "break"
    def _handle_key_pair(self, event):
        key_map = {'(': ')', '[': ']', '{': '}', "'": "'", '"': '"'}
        char = event.char
        self.cpp_code_text.insert(tk.INSERT, char + key_map[char])
        self.cpp_code_text.mark_set(tk.INSERT, f"{tk.INSERT}-1c")
        return "break"

    def _create_editor_pane(self, parent, title, paste_command, clear_command):
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

    # --- UPDATED: DIFF WIDGETS WITH TOGGLE ---
    def _create_diff_widgets(self):
        self.diff_frame.grid_rowconfigure(1, weight=1)
        self.diff_frame.grid_columnconfigure(0, weight=1, uniform='group1')
        self.diff_frame.grid_columnconfigure(1, weight=1, uniform='group1')
        
        # Header with Checkbox
        header_frame = ttk.Frame(self.diff_frame)
        header_frame.grid(row=0, column=0, columnspan=2, sticky="ew", padx=10, pady=(10, 5))
        
        ttk.Label(header_frame, text="Expected Output", font=self.title_font).pack(side=tk.LEFT)
        
        # Spacer
        ttk.Frame(header_frame, width=30).pack(side=tk.LEFT)
        
        # Toggle Checkbox
        self.sync_check = ttk.Checkbutton(header_frame, text="Smart Sync Lines (Git-style)", variable=self.diff_smart_sync_var, command=self._update_diff_text)
        self.sync_check.pack(side=tk.LEFT)
        
        ttk.Label(header_frame, text="Your Output", font=self.title_font).pack(side=tk.RIGHT)

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
        self.diff_left.tag_config("mismatch", background="#5c2c2c") # Used in simple mode
        self.diff_right.tag_config("mismatch", background="#5c2c2c") # Used in simple mode

    def _on_scroll(self, *args):
        self.diff_left.yview(*args)
        self.diff_right.yview(*args)
        self.line_nums_left.yview(*args)
        self.line_nums_right.yview(*args)

    # --- UPDATED: DIFF LOGIC TO HANDLE BOTH MODES ---
    def _update_diff_text(self):
        widgets = [self.diff_left, self.diff_right, self.line_nums_left, self.line_nums_right]
        for w in widgets: w.config(state=tk.NORMAL)
        for w in widgets: w.delete('1.0', tk.END)

        # Standardize newlines and split, but preserve empty lines if needed
        # splitlines() removes the final newline, so we handle lists carefully
        from_raw = self.last_desired_output.replace('\r\n', '\n')
        to_raw = self.last_generated_output.replace('\r\n', '\n')
        
        from_lines = from_raw.splitlines()
        to_lines = to_raw.splitlines()

        # MODE 1: SMART SYNC (Git Style)
        if self.diff_smart_sync_var.get():
            matcher = difflib.SequenceMatcher(None, from_lines, to_lines)
            left_num, right_num = 1, 1
            for tag, i1, i2, j1, j2 in matcher.get_opcodes():
                if tag == 'equal':
                    for line in from_lines[i1:i2]:
                        self.line_nums_left.insert(tk.END, f"{left_num}\n")
                        self.line_nums_right.insert(tk.END, f"{right_num}\n")
                        self.diff_left.insert(tk.END, f"{line}\n")
                        self.diff_right.insert(tk.END, f"{line}\n")
                        left_num += 1; right_num += 1
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
                    deleted_lines, added_lines = from_lines[i1:i2], to_lines[j1:j2]
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
        
        # MODE 2: SIMPLE LINE-BY-LINE (User Request)
        else:
            max_lines = max(len(from_lines), len(to_lines))
            for i in range(max_lines):
                # Handle Left Side
                if i < len(from_lines):
                    line_content = from_lines[i]
                    self.line_nums_left.insert(tk.END, f"{i+1}\n")
                    # Check if mismatch
                    tag = "mismatch" if (i >= len(to_lines) or from_lines[i] != to_lines[i]) else ""
                    self.diff_left.insert(tk.END, f"{line_content}\n", tag)
                else:
                    self.line_nums_left.insert(tk.END, "\n")
                    self.diff_left.insert(tk.END, "\n")

                # Handle Right Side
                if i < len(to_lines):
                    line_content = to_lines[i]
                    self.line_nums_right.insert(tk.END, f"{i+1}\n")
                    # Check if mismatch
                    tag = "mismatch" if (i >= len(from_lines) or from_lines[i] != to_lines[i]) else ""
                    self.diff_right.insert(tk.END, f"{line_content}\n", tag)
                else:
                    self.line_nums_right.insert(tk.END, "\n")
                    self.diff_right.insert(tk.END, "\n")

        for w in widgets: w.config(state=tk.DISABLED)

    def paste_to_cpp_code(self):
        try: self.cpp_code_text.insert(tk.INSERT, self.clipboard_get())
        except tk.TclError: pass
    def clear_cpp_code(self): self.cpp_code_text.delete('1.0', tk.END)
    def paste_to_input(self):
        try: self.input_text.insert(tk.INSERT, self.clipboard_get())
        except tk.TclError: pass
    def clear_input(self): self.input_text.delete('1.0', tk.END)
    def paste_to_desired_output(self):
        try: self.desired_output_text.insert(tk.INSERT, self.clipboard_get())
        except tk.TclError: pass
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

    # --- MAIN RUN LOGIC ---
    def start_test_thread(self):
        self.run_button.config(state=tk.DISABLED, text="Running...")
        self.compare_button.config(state=tk.DISABLED)
        self.clear_status()
        thread = threading.Thread(target=self.run_test)
        thread.daemon = True
        thread.start()

    def run_test(self):
        input_data = self.input_text.get("1.0", tk.END)
        self.last_desired_output = self.desired_output_text.get("1.0", tk.END)
        self.last_generated_output = ""
        creation_flags = subprocess.CREATE_NO_WINDOW if os.name == 'nt' else 0

        with tempfile.TemporaryDirectory() as temp_dir:
            executable_path = os.path.join(temp_dir, "main_executable")
            compile_command = []
            
            self.log_status("--- Starting Test ---", "INFO")
            if self.use_file_path_var.get() == 1:
                cpp_source_path = self.cpp_file_path_var.get().strip()
                if not cpp_source_path or not os.path.exists(cpp_source_path):
                    self.log_status("Error: File not found.", "ERROR")
                    self.finalize_ui()
                    return
                self.log_status(f"Using file: {os.path.basename(cpp_source_path)}", "INFO")
                compile_command = ["g++", "-O2", "-Wall", cpp_source_path, "-o", executable_path]
            else:
                cpp_code = self.cpp_code_text.get("1.0", tk.END)
                if not cpp_code.strip():
                    self.log_status("Error: Editor empty.", "ERROR")
                    self.finalize_ui()
                    return
                cpp_source_path = os.path.join(temp_dir, "main.cpp")
                with open(cpp_source_path, "w", encoding='utf-8') as f: f.write(cpp_code)
                self.log_status("Using editor code.", "INFO")
                compile_command = ["g++", "-O2", "-Wall", cpp_source_path, "-o", executable_path]
            
            try:
                subprocess.run(compile_command, capture_output=True, text=True, check=True, timeout=10, creationflags=creation_flags)
                self.log_status("Compilation successful.", "INFO")
            except subprocess.CalledProcessError as e:
                self.log_status(f"Compilation Failed:\n{e.stderr}", "ERROR")
                self.finalize_ui(); return

            try:
                run_result = subprocess.run([executable_path], input=input_data, capture_output=True, text=True, check=True, timeout=5, creationflags=creation_flags)
                self.last_generated_output = run_result.stdout
                if run_result.stderr: self.log_status(f"Runtime Warning:\n{run_result.stderr}", "WARNING")
            except Exception as e:
                self.log_status(f"Runtime Error: {str(e)}", "ERROR")
                self.finalize_ui(enable_diff=True); return
            
            norm_gen = self.last_generated_output.strip().replace('\r\n', '\n')
            norm_des = self.last_desired_output.strip().replace('\r\n', '\n')
            if norm_gen == norm_des:
                self.log_status("\n✅ --- Result: PASSED --- ✅", "SUCCESS")
            else:
                self.log_status("\n❌ --- Result: FAILED --- ❌", "FAILURE")
                self.log_status("Outputs do not match. Click 'Compare Output' for details.", "INFO")

        self.finalize_ui(enable_diff=True)

    def finalize_ui(self, enable_diff=False):
        def _update():
            self.run_button.config(state=tk.NORMAL, text="Compile & Run")
            if enable_diff:
                self.compare_button.config(state=tk.NORMAL)
                if self.editor_frame.winfo_ismapped(): self.compare_button.config(style="InactiveNav.TButton")
                else: self.compare_button.config(style="ActiveNav.TButton")
        self.after(0, _update)

    # --- STRESS TESTING LOGIC ---
    def start_stress_thread(self):
        if self.is_stress_running: return
        
        gen_path = self.gen_file_path_var.get().strip()
        brute_path = self.brute_file_path_var.get().strip()
        
        if not gen_path or not os.path.exists(gen_path):
            self.stress_status_lbl.config(text="Error: Generator path invalid", foreground="#F44336")
            return
        if not brute_path or not os.path.exists(brute_path):
            self.stress_status_lbl.config(text="Error: Brute Force path invalid", foreground="#F44336")
            return

        self.stop_stress_event.clear()
        self.is_stress_running = True
        self.stress_run_btn.config(state=tk.DISABLED)
        self.stress_stop_btn.config(state=tk.NORMAL)
        
        # Clear previous results
        self.stress_input_text.delete('1.0', tk.END)
        self.stress_expected_text.delete('1.0', tk.END)
        self.stress_actual_text.delete('1.0', tk.END)

        thread = threading.Thread(target=self.run_stress_test, args=(gen_path, brute_path))
        thread.daemon = True
        thread.start()

    def stop_stress_test(self):
        self.stop_stress_event.set()
        self.stress_status_lbl.config(text="Stopping...", foreground="#E5C07B")

    def run_stress_test(self, gen_path, brute_path):
        creation_flags = subprocess.CREATE_NO_WINDOW if os.name == 'nt' else 0
        iterations = self.stress_iterations_var.get()
        
        def update_lbl(text, color="white"):
            self.after(0, lambda: self.stress_status_lbl.config(text=text, foreground=color))

        with tempfile.TemporaryDirectory() as temp_dir:
            update_lbl("Compiling files...", "#61AFEF")
            
            gen_exe = os.path.join(temp_dir, "gen")
            brute_exe = os.path.join(temp_dir, "brute")
            opt_exe = os.path.join(temp_dir, "opt")
            
            # 1. Compile Generator
            try:
                subprocess.run(["g++", "-O2", gen_path, "-o", gen_exe], check=True, capture_output=True, creationflags=creation_flags)
            except:
                update_lbl("Error compiling Generator", "#F44336"); self._end_stress(); return

            # 2. Compile Brute
            try:
                subprocess.run(["g++", "-O2", brute_path, "-o", brute_exe], check=True, capture_output=True, creationflags=creation_flags)
            except:
                update_lbl("Error compiling Brute Force", "#F44336"); self._end_stress(); return

            # 3. Compile Optimized (Main)
            try:
                if self.use_file_path_var.get() == 1:
                    src = self.cpp_file_path_var.get().strip()
                    if not src: raise Exception
                    subprocess.run(["g++", "-O2", src, "-o", opt_exe], check=True, capture_output=True, creationflags=creation_flags)
                else:
                    src = os.path.join(temp_dir, "opt_src.cpp")
                    with open(src, "w", encoding='utf-8') as f: f.write(self.cpp_code_text.get("1.0", tk.END))
                    subprocess.run(["g++", "-O2", src, "-o", opt_exe], check=True, capture_output=True, creationflags=creation_flags)
            except:
                update_lbl("Error compiling Main Solution", "#F44336"); self._end_stress(); return

            # Loop
            update_lbl(f"Running {iterations} tests...", "#61AFEF")
            for i in range(1, iterations + 1):
                if self.stop_stress_event.is_set():
                    update_lbl("Stopped by user", "#E5C07B"); self._end_stress(); return
                
                update_lbl(f"Running Test {i}/{iterations}...", "#61AFEF")

                # Generate Input
                try:
                    proc_gen = subprocess.run([gen_exe, str(i)], capture_output=True, text=True, check=True, creationflags=creation_flags)
                    current_input = proc_gen.stdout
                except:
                    update_lbl(f"Generator crashed on test {i}", "#F44336"); self._end_stress(); return
                
                # Run Brute
                try:
                    proc_brute = subprocess.run([brute_exe], input=current_input, capture_output=True, text=True, check=True, creationflags=creation_flags)
                    out_brute = proc_brute.stdout.strip()
                except:
                    update_lbl(f"Brute Force crashed on test {i}", "#F44336"); self._end_stress(); return

                # Run Optimized
                try:
                    proc_opt = subprocess.run([opt_exe], input=current_input, capture_output=True, text=True, check=True, creationflags=creation_flags)
                    out_opt = proc_opt.stdout.strip()
                except:
                    update_lbl(f"Optimized Code crashed on test {i}", "#F44336"); self._populate_stress_results(current_input, out_brute, "CRASHED"); self._end_stress(); return

                # Compare
                if out_brute != out_opt:
                    update_lbl(f"Mismatch found on test {i}!", "#F44336")
                    self._populate_stress_results(current_input, out_brute, out_opt)
                    self._end_stress()
                    return

            update_lbl(f"Passed all {iterations} tests!", "#4CAF50")
            self._end_stress()

    def _populate_stress_results(self, inp, exp, act):
        def _do():
            self.stress_input_text.insert(tk.END, inp)
            self.stress_expected_text.insert(tk.END, exp)
            self.stress_actual_text.insert(tk.END, act)
        self.after(0, _do)

    def _end_stress(self):
        self.is_stress_running = False
        def _do():
            self.stress_run_btn.config(state=tk.NORMAL)
            self.stress_stop_btn.config(state=tk.DISABLED)
        self.after(0, _do)

if __name__ == "__main__":
    app = CppGraderApp()
    app.mainloop()