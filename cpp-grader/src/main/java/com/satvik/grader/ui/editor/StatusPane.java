package com.satvik.grader.ui.editor;

import com.satvik.grader.core.Tone;
import com.satvik.grader.ui.theme.Theme;

import javax.swing.JTextPane;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicTextPaneUI;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.Color;
import java.util.EnumMap;
import java.util.Map;

/** Read-only colour-coded log ("Execution Status"), mirroring the legacy SUCCESS/FAILURE/INFO/WARNING/ERROR tags. */
public class StatusPane extends JTextPane {

    private final Map<Tone, SimpleAttributeSet> styles = new EnumMap<>(Tone.class);

    public StatusPane() {
        setOpaque(false);
        setEditable(false);
        setFont(Theme.mono(13f));
        setForeground(Theme.TEXT_SECONDARY);
        setSelectionColor(Theme.SELECTION);
        setCaretColor(Theme.TEXT);
        setBorder(new EmptyBorder(8, 10, 8, 10));

        style(Tone.PLAIN, Theme.TEXT_SECONDARY, false);
        style(Tone.INFO, Theme.INFO, false);
        style(Tone.SUCCESS, Theme.SUCCESS, true);
        style(Tone.FAILURE, Theme.ERROR, true);
        style(Tone.WARNING, Theme.WARNING, false);
        style(Tone.ERROR, Theme.ERROR, false);
    }

    @Override
    public void updateUI() {
        setUI(new BasicTextPaneUI());
    }

    private void style(Tone t, Color c, boolean bold) {
        SimpleAttributeSet a = new SimpleAttributeSet();
        StyleConstants.setForeground(a, c);
        StyleConstants.setBold(a, bold);
        StyleConstants.setFontFamily(a, Theme.monoFamily());
        StyleConstants.setFontSize(a, 13);
        styles.put(t, a);
    }

    public void append(String message, Tone tone) {
        StyledDocument doc = getStyledDocument();
        try {
            doc.insertString(doc.getLength(), message + "\n", styles.get(tone));
        } catch (BadLocationException ignored) {
            // ignore
        }
        setCaretPosition(doc.getLength());
    }

    public void clear() {
        setText("");
    }
}
