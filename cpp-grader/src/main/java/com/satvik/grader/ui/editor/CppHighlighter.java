package com.satvik.grader.ui.editor;

import com.satvik.grader.ui.theme.Theme;

import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.TabSet;
import javax.swing.text.TabStop;
import java.awt.Color;
import java.awt.Font;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Lightweight, debounced regex-based C++ syntax highlighter for a {@link CodeEditor}. */
public final class CppHighlighter implements DocumentListener {

    private static final Pattern TOKENS = Pattern.compile(
            "(?<comment>//[^\\n]*|/\\*[\\s\\S]*?(?:\\*/|\\z))"
                    + "|(?<pre>^[ \\t]*#[ \\t]*\\w+)"
                    + "|(?<header>(?<=include)[ \\t]*<[^>\\n]*>)"
                    + "|(?<str>\"(?:\\\\.|[^\"\\\\\\n])*\"?)"
                    + "|(?<chr>'(?:\\\\.|[^'\\\\\\n])*'?)"
                    + "|(?<num>\\b(?:0[xX][0-9a-fA-F']+|\\d[\\d']*(?:\\.\\d*)?(?:[eE][+-]?\\d+)?)[uUlLfF]*\\b)"
                    + "|(?<id>\\b[A-Za-z_]\\w*\\b)",
            Pattern.MULTILINE);

    private static final Set<String> KEYWORDS = Set.of(
            "alignas", "alignof", "and", "asm", "break", "case", "catch", "class", "const", "consteval",
            "constexpr", "constinit", "const_cast", "continue", "co_await", "co_return", "co_yield", "decltype",
            "default", "delete", "do", "dynamic_cast", "else", "enum", "explicit", "export", "extern", "for",
            "friend", "goto", "if", "inline", "mutable", "namespace", "new", "noexcept", "not", "operator", "or",
            "private", "protected", "public", "register", "reinterpret_cast", "requires", "return", "sizeof",
            "static", "static_assert", "static_cast", "struct", "switch", "template", "this", "thread_local",
            "throw", "try", "typedef", "typeid", "typename", "union", "using", "virtual", "volatile", "while",
            "xor", "concept", "override", "final");

    private static final Set<String> TYPES = Set.of(
            "auto", "bool", "char", "char8_t", "char16_t", "char32_t", "double", "float", "int", "long", "short",
            "signed", "unsigned", "void", "wchar_t", "size_t", "int8_t", "int16_t", "int32_t", "int64_t",
            "uint8_t", "uint16_t", "uint32_t", "uint64_t", "string", "vector", "map", "set", "unordered_map",
            "unordered_set", "multiset", "multimap", "pair", "tuple", "array", "deque", "queue", "priority_queue",
            "stack", "list", "bitset", "std", "ll", "ull", "ld", "vi", "vll", "pii", "pll", "vvi", "vpii",
            "string_view", "optional", "function", "greater", "less", "__int128");

    private static final Set<String> CONSTANTS = Set.of(
            "true", "false", "nullptr", "NULL", "INT_MAX", "INT_MIN", "LLONG_MAX", "LLONG_MIN", "MOD", "INF",
            "endl", "cin", "cout", "cerr");

    private final CodeEditor editor;
    private final Timer timer;
    private final SimpleAttributeSet base = new SimpleAttributeSet();
    private final SimpleAttributeSet keyword;
    private final SimpleAttributeSet type;
    private final SimpleAttributeSet string;
    private final SimpleAttributeSet number;
    private final SimpleAttributeSet comment;
    private final SimpleAttributeSet preprocessor;
    private final SimpleAttributeSet function;
    private final SimpleAttributeSet constant;
    private final SimpleAttributeSet tabs = new SimpleAttributeSet();

    CppHighlighter(CodeEditor editor) {
        this.editor = editor;
        Font f = editor.getFont();
        StyleConstants.setFontFamily(base, f.getFamily());
        StyleConstants.setFontSize(base, f.getSize());
        StyleConstants.setForeground(base, Theme.CODE_TEXT);
        keyword = color(Theme.SYN_KEYWORD, false);
        type = color(Theme.SYN_TYPE, false);
        string = color(Theme.SYN_STRING, false);
        number = color(Theme.SYN_NUMBER, false);
        comment = color(Theme.SYN_COMMENT, true);
        preprocessor = color(Theme.SYN_PREPROCESSOR, false);
        function = color(Theme.SYN_FUNCTION, false);
        constant = color(Theme.SYN_CONSTANT, false);

        int cw = editor.getFontMetrics(f).charWidth(' ') * 4;
        TabStop[] stops = new TabStop[60];
        for (int i = 0; i < stops.length; i++) {
            stops[i] = new TabStop(cw * (i + 1));
        }
        StyleConstants.setTabSet(tabs, new TabSet(stops));

        timer = new Timer(140, e -> highlightNow());
        timer.setRepeats(false);
        editor.getDocument().addDocumentListener(this);
    }

    private SimpleAttributeSet color(Color c, boolean italic) {
        SimpleAttributeSet a = new SimpleAttributeSet();
        StyleConstants.setForeground(a, c);
        StyleConstants.setItalic(a, italic);
        return a;
    }

    public void highlightNow() {
        timer.stop();
        StyledDocument doc = editor.getStyledDocument();
        int len = doc.getLength();
        String text;
        try {
            text = doc.getText(0, len);
        } catch (BadLocationException e) {
            return;
        }
        doc.setParagraphAttributes(0, len, tabs, false);
        doc.setCharacterAttributes(0, len, base, true);
        Matcher m = TOKENS.matcher(text);
        while (m.find()) {
            SimpleAttributeSet attr = null;
            if (m.group("comment") != null) {
                attr = comment;
            } else if (m.group("pre") != null) {
                attr = preprocessor;
            } else if (m.group("header") != null || m.group("str") != null || m.group("chr") != null) {
                attr = string;
            } else if (m.group("num") != null) {
                attr = number;
            } else {
                String id = m.group("id");
                if (KEYWORDS.contains(id)) {
                    attr = keyword;
                } else if (TYPES.contains(id)) {
                    attr = type;
                } else if (CONSTANTS.contains(id)) {
                    attr = constant;
                } else if (followedByParen(text, m.end())) {
                    attr = function;
                }
            }
            if (attr != null) {
                doc.setCharacterAttributes(m.start(), m.end() - m.start(), attr, false);
            }
        }
    }

    private static boolean followedByParen(String text, int i) {
        while (i < text.length() && (text.charAt(i) == ' ' || text.charAt(i) == '\t')) {
            i++;
        }
        return i < text.length() && text.charAt(i) == '(';
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        timer.restart();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        timer.restart();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        // attribute changes (including our own) - ignore
    }
}
