package com.satvik.grader.ui.editor;

import com.satvik.grader.ui.theme.Theme;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.Rectangle2D;

/** Line-number row header for a text component (current line is highlighted). */
public class LineNumberGutter extends JComponent {

    private final JTextComponent text;
    private int lastDigits = -1;

    public LineNumberGutter(JTextComponent text) {
        this.text = text;
        setOpaque(false);
        setFont(text.getFont());
        text.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                changed();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                changed();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                // attributes only
            }
        });
        text.addCaretListener(e -> repaint());
        text.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                revalidate();
                repaint();
            }
        });
    }

    private void changed() {
        SwingUtilities.invokeLater(() -> {
            int digits = digits();
            if (digits != lastDigits) {
                lastDigits = digits;
                revalidate();
            }
            repaint();
        });
    }

    private int digits() {
        int lines = text.getDocument().getDefaultRootElement().getElementCount();
        return Math.max(2, String.valueOf(lines).length());
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        int w = fm.charWidth('0') * digits() + 26;
        return new Dimension(w, Math.max(text.getHeight(), text.getPreferredSize().height));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.hints(g2);
        Rectangle clip = g2.getClipBounds();
        if (clip == null) {
            clip = new Rectangle(0, 0, getWidth(), getHeight());
        }
        g2.setColor(new Color(255, 255, 255, 14));
        g2.fillRect(getWidth() - 1, clip.y, 1, clip.height);

        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        Element root = text.getDocument().getDefaultRootElement();
        int startOffset = text.viewToModel2D(new Point(0, clip.y));
        int endOffset = text.viewToModel2D(new Point(0, clip.y + clip.height));
        int startLine = root.getElementIndex(Math.max(0, startOffset));
        int endLine = root.getElementIndex(Math.max(0, endOffset));
        int caretLine = root.getElementIndex(text.getCaretPosition());

        for (int i = startLine; i <= endLine; i++) {
            try {
                Rectangle2D r = text.modelToView2D(root.getElement(i).getStartOffset());
                if (r == null) {
                    continue;
                }
                String num = String.valueOf(i + 1);
                int baseline = (int) (r.getY() + (r.getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                g2.setColor(i == caretLine ? Theme.TEXT : Theme.TEXT_DIM);
                g2.drawString(num, getWidth() - 12 - fm.stringWidth(num), baseline);
            } catch (BadLocationException ignored) {
                // ignore
            }
        }
        g2.dispose();
    }
}
