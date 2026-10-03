package com.satvik.grader.ui.editor;

import com.satvik.grader.ui.theme.Theme;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JTextPane;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.plaf.basic.BasicTextPaneUI;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoManager;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.util.function.Consumer;

/**
 * C++ code editor: syntax highlighting, current-line highlight, 4-space Tab / Shift+Tab (block indent),
 * auto-indent on Enter, auto-closing pairs with over-typing, and undo/redo.
 */
public class CodeEditor extends JTextPane {

    private static final String INDENT = "    ";

    private final UndoManager undo = new UndoManager();
    private final CppHighlighter highlighter;
    private CompoundEdit compound;

    public CodeEditor() {
        setOpaque(false);
        setFont(Theme.mono(14f));
        setForeground(Theme.CODE_TEXT);
        setCaretColor(Theme.TEXT);
        setSelectionColor(Theme.SELECTION);
        setSelectedTextColor(null);
        setBorder(new EmptyBorder(8, 10, 8, 10));
        putClientProperty("caretWidth", 2);

        highlighter = new CppHighlighter(this);

        getDocument().addUndoableEditListener(e -> {
            if (e.getEdit() instanceof AbstractDocument.DefaultDocumentEvent dde
                    && dde.getType() == DocumentEvent.EventType.CHANGE) {
                return; // styling changes are not user edits
            }
            if (compound != null) {
                compound.addEdit(e.getEdit());
            } else {
                undo.addEdit(e.getEdit());
            }
        });
        addCaretListener(e -> repaint());
        installKeys();
    }

    @Override
    public void updateUI() {
        setUI(new BasicTextPaneUI());
    }

    // ---- public API -------------------------------------------------------------------------

    public void setCode(String code) {
        setText(code == null ? "" : code.replace("\r\n", "\n").replace('\r', '\n'));
        setCaretPosition(0);
        undo.discardAllEdits();
        highlighter.highlightNow();
    }

    public String getCode() {
        return getText();
    }

    public void insertAtCaret(String s) {
        if (s != null && isEditable()) {
            replaceSelection(s.replace("\r\n", "\n").replace('\r', '\n'));
        }
    }

    /** Clears via a normal (undoable) edit. */
    public void clearAll() {
        if (isEditable()) {
            selectAll();
            replaceSelection("");
        }
    }

    // ---- no line wrap ---------------------------------------------------------------------

    @Override
    public boolean getScrollableTracksViewportWidth() {
        Component parent = getParent();
        if (parent instanceof JViewport) {
            return getUI().getPreferredSize(this).width <= parent.getWidth();
        }
        return false;
    }

    // ---- painting ---------------------------------------------------------------------------

    @Override
    protected void paintComponent(Graphics g) {
        if (isEditable()) {
            try {
                Rectangle2D r = modelToView2D(getCaretPosition());
                if (r != null) {
                    g.setColor(Theme.CURRENT_LINE);
                    g.fillRect(0, (int) r.getY(), getWidth(), (int) Math.ceil(r.getHeight()));
                }
            } catch (BadLocationException ignored) {
                // ignore
            }
        }
        super.paintComponent(g);
    }

    // ---- key behaviour ----------------------------------------------------------------------

    private void installKeys() {
        InputMap im = getInputMap();
        ActionMap am = getActionMap();
        bind(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), "cg-indent", e -> indent());
        bind(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK), "cg-dedent", e -> dedent());
        bind(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "cg-newline", e -> newline());
        bind(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "cg-undo", e -> undo());
        bind(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK), "cg-redo", e -> redo());
        bind(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK),
                "cg-redo2", e -> redo());

        String[][] pairs = {{"(", ")"}, {"[", "]"}, {"{", "}"}, {"\"", "\""}, {"'", "'"}};
        for (String[] p : pairs) {
            char open = p[0].charAt(0);
            char close = p[1].charAt(0);
            bind(im, am, KeyStroke.getKeyStroke(open), "cg-open-" + open, e -> openPair(open, close));
        }
        for (char close : new char[]{')', ']', '}'}) {
            bind(im, am, KeyStroke.getKeyStroke(close), "cg-close-" + close, e -> closeChar(close));
        }
    }

    private static void bind(InputMap im, ActionMap am, KeyStroke ks, String name, Consumer<ActionEvent> action) {
        im.put(ks, name);
        am.put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.accept(e);
            }
        });
    }

    private void undo() {
        try {
            if (undo.canUndo()) {
                undo.undo();
            }
        } catch (CannotUndoException ignored) {
            // ignore
        }
    }

    private void redo() {
        try {
            if (undo.canRedo()) {
                undo.redo();
            }
        } catch (CannotRedoException ignored) {
            // ignore
        }
    }

    private void beginCompound() {
        compound = new CompoundEdit();
    }

    private void endCompound() {
        if (compound != null) {
            compound.end();
            undo.addEdit(compound);
            compound = null;
        }
    }

    private char charAt(int pos) {
        try {
            Document d = getDocument();
            return (pos >= 0 && pos < d.getLength()) ? d.getText(pos, 1).charAt(0) : 0;
        } catch (BadLocationException e) {
            return 0;
        }
    }

    private String lineText(int lineIndex) {
        Element line = getDocument().getDefaultRootElement().getElement(lineIndex);
        try {
            return getDocument().getText(line.getStartOffset(), line.getEndOffset() - line.getStartOffset());
        } catch (BadLocationException e) {
            return "";
        }
    }

    private void indent() {
        if (!isEditable()) {
            return;
        }
        int s = getSelectionStart();
        int e = getSelectionEnd();
        Element root = getDocument().getDefaultRootElement();
        int first = root.getElementIndex(s);
        int last = root.getElementIndex(e);
        if (s == e || first == last) {
            replaceSelection(INDENT);
            return;
        }
        if (e == root.getElement(last).getStartOffset()) {
            last--;
        }
        beginCompound();
        try {
            for (int i = first; i <= last; i++) {
                getDocument().insertString(root.getElement(i).getStartOffset(), INDENT, null);
            }
        } catch (BadLocationException ignored) {
            // ignore
        } finally {
            endCompound();
        }
        select(root.getElement(first).getStartOffset(), root.getElement(last).getEndOffset() - 1);
    }

    private void dedent() {
        if (!isEditable()) {
            return;
        }
        int s = getSelectionStart();
        int e = getSelectionEnd();
        boolean hadSelection = s != e;
        Element root = getDocument().getDefaultRootElement();
        int first = root.getElementIndex(s);
        int last = root.getElementIndex(e);
        if (hadSelection && last > first && e == root.getElement(last).getStartOffset()) {
            last--;
        }
        beginCompound();
        int removedBeforeCaret = 0;
        try {
            for (int i = first; i <= last; i++) {
                String text = lineText(i);
                int n = 0;
                if (text.startsWith("\t")) {
                    n = 1;
                } else {
                    while (n < 4 && n < text.length() && text.charAt(n) == ' ') {
                        n++;
                    }
                }
                if (n > 0) {
                    getDocument().remove(root.getElement(i).getStartOffset(), n);
                    if (i == first) {
                        removedBeforeCaret = n;
                    }
                }
            }
        } catch (BadLocationException ignored) {
            // ignore
        } finally {
            endCompound();
        }
        if (hadSelection && last > first) {
            select(root.getElement(first).getStartOffset(), root.getElement(last).getEndOffset() - 1);
        } else {
            int lineStart = root.getElement(first).getStartOffset();
            setCaretPosition(Math.max(lineStart, s - removedBeforeCaret));
        }
    }

    private void newline() {
        if (!isEditable()) {
            return;
        }
        replaceSelection("");
        int pos = getCaretPosition();
        Element root = getDocument().getDefaultRootElement();
        int lineStart = root.getElement(root.getElementIndex(pos)).getStartOffset();
        String before;
        try {
            before = getDocument().getText(lineStart, pos - lineStart);
        } catch (BadLocationException e) {
            before = "";
        }
        int n = 0;
        while (n < before.length() && (before.charAt(n) == ' ' || before.charAt(n) == '\t')) {
            n++;
        }
        String indent = before.substring(0, n);
        String trimmed = before.stripTrailing();
        char last = trimmed.isEmpty() ? 0 : trimmed.charAt(trimmed.length() - 1);
        char next = charAt(pos);

        beginCompound();
        try {
            if (last == '{' && next == '}') {
                String ins = "\n" + indent + INDENT + "\n" + indent;
                getDocument().insertString(pos, ins, null);
                setCaretPosition(pos + 1 + indent.length() + INDENT.length());
            } else if (last == '{' || last == '(' || last == '[') {
                getDocument().insertString(pos, "\n" + indent + INDENT, null);
            } else {
                getDocument().insertString(pos, "\n" + indent, null);
            }
        } catch (BadLocationException ignored) {
            // ignore
        } finally {
            endCompound();
        }
    }

    private void openPair(char open, char close) {
        if (!isEditable()) {
            return;
        }
        int s = getSelectionStart();
        int e = getSelectionEnd();
        boolean quote = open == close;
        if (s != e) {
            String sel = getSelectedText();
            beginCompound();
            replaceSelection(open + sel + close);
            endCompound();
            select(s + 1, s + 1 + sel.length());
            return;
        }
        if (quote && charAt(s) == close) {
            setCaretPosition(s + 1);
            return;
        }
        beginCompound();
        replaceSelection("" + open + close);
        endCompound();
        setCaretPosition(s + 1);
    }

    private void closeChar(char close) {
        if (!isEditable()) {
            return;
        }
        int s = getSelectionStart();
        if (s == getSelectionEnd() && charAt(s) == close) {
            setCaretPosition(s + 1);
            return;
        }
        if (close == '}' && s == getSelectionEnd()) {
            // de-indent a "}" typed on an otherwise blank line
            Element root = getDocument().getDefaultRootElement();
            int lineStart = root.getElement(root.getElementIndex(s)).getStartOffset();
            try {
                String before = getDocument().getText(lineStart, s - lineStart);
                if (before.length() >= 4 && before.isBlank() && before.endsWith(INDENT)) {
                    beginCompound();
                    getDocument().remove(s - 4, 4);
                    getDocument().insertString(s - 4, "}", null);
                    endCompound();
                    return;
                }
            } catch (BadLocationException ignored) {
                // fall through
            }
        }
        replaceSelection(String.valueOf(close));
    }
}
