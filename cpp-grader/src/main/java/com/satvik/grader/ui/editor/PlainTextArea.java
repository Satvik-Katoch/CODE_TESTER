package com.satvik.grader.ui.editor;

import com.satvik.grader.ui.theme.Theme;

import javax.swing.AbstractAction;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicTextAreaUI;
import javax.swing.undo.UndoManager;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

/** Monospaced transparent text area used for Input / Desired Output / stress panes. */
public class PlainTextArea extends JTextArea {

    private final UndoManager undo = new UndoManager();

    public PlainTextArea() {
        setOpaque(false);
        setFont(Theme.mono(13.5f));
        setForeground(Theme.CODE_TEXT);
        setCaretColor(Theme.TEXT);
        setSelectionColor(Theme.SELECTION);
        setSelectedTextColor(Theme.TEXT);
        setDisabledTextColor(Theme.TEXT_DIM);
        setTabSize(4);
        setBorder(new EmptyBorder(8, 10, 8, 10));
        putClientProperty("caretWidth", 2);

        getDocument().addUndoableEditListener(e -> undo.addEdit(e.getEdit()));
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "pt-undo");
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK), "pt-redo");
        getActionMap().put("pt-undo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (undo.canUndo()) {
                    undo.undo();
                }
            }
        });
        getActionMap().put("pt-redo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (undo.canRedo()) {
                    undo.redo();
                }
            }
        });
    }

    @Override
    public void updateUI() {
        setUI(new BasicTextAreaUI());
    }

    /** Replace all text (not undoable - used for programmatic results). */
    public void setContent(String text) {
        setText(text == null ? "" : text.replace("\r\n", "\n"));
        setCaretPosition(0);
        undo.discardAllEdits();
    }

    public void insertAtCaret(String s) {
        if (s != null && isEditable()) {
            replaceSelection(s.replace("\r\n", "\n"));
        }
    }

    /** Clears via a normal (undoable) edit. */
    public void clearAll() {
        selectAll();
        replaceSelection("");
    }
}
