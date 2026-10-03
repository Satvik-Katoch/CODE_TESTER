package com.satvik.grader.ui.editor;

import com.satvik.grader.ui.theme.Theme;

import javax.swing.JComponent;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicTextAreaUI;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.util.List;

/**
 * Read-only, non-wrapping text area that paints a full-width background per row
 * (removed / added / mismatch / empty filler), plus a matching line-number gutter.
 */
public class DiffTextArea extends JTextArea {

    public enum RowKind { NORMAL, REMOVED, ADDED, MISMATCH, EMPTY }

    private RowKind[] kinds = new RowKind[0];
    private String[] labels = new String[0];
    private final Gutter gutter = new Gutter();

    public DiffTextArea() {
        setOpaque(false);
        setEditable(false);
        setFont(Theme.mono(13.5f));
        setForeground(Theme.CODE_TEXT);
        setSelectionColor(Theme.SELECTION);
        setSelectedTextColor(Theme.TEXT);
        setCaretColor(Theme.TEXT);
        setBorder(new EmptyBorder(6, 10, 6, 10));
        setLineWrap(false);
    }

    @Override
    public void updateUI() {
        setUI(new BasicTextAreaUI());
    }

    public JComponent gutter() {
        return gutter;
    }

    public void setRows(List<String> lines, List<RowKind> rowKinds, List<String> rowLabels) {
        kinds = rowKinds.toArray(new RowKind[0]);
        labels = rowLabels.toArray(new String[0]);
        setText(String.join("\n", lines));
        setCaretPosition(0);
        gutter.revalidate();
        gutter.repaint();
        repaint();
    }

    int rowHeightPx() {
        return getRowHeight();
    }

    private static Color colorFor(RowKind k) {
        return switch (k) {
            case REMOVED, MISMATCH -> Theme.DIFF_REMOVED;
            case ADDED -> Theme.DIFF_ADDED;
            case EMPTY -> Theme.DIFF_EMPTY;
            default -> null;
        };
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Rectangle clip = g2.getClipBounds();
        Insets in = getInsets();
        int rh = getRowHeight();
        int w = getWidth();
        if (clip != null && rh > 0) {
            int first = Math.max(0, (clip.y - in.top) / rh);
            int last = Math.min(kinds.length - 1, (clip.y + clip.height - in.top) / rh);
            for (int i = first; i <= last; i++) {
                Color c = colorFor(kinds[i]);
                if (c == null) {
                    continue;
                }
                int y = in.top + i * rh;
                g2.setColor(c);
                g2.fillRect(0, y, w, rh);
                if (kinds[i] == RowKind.EMPTY) {
                    // diagonal hatch for filler rows
                    Graphics2D gh = (Graphics2D) g2.create();
                    gh.clipRect(0, y, w, rh);
                    gh.setColor(new Color(255, 255, 255, 14));
                    gh.setStroke(new BasicStroke(1f));
                    for (int x = clip.x - rh - (clip.x % 8); x < clip.x + clip.width + rh; x += 8) {
                        gh.drawLine(x, y + rh, x + rh, y);
                    }
                    gh.dispose();
                } else {
                    Color edge = kinds[i] == RowKind.ADDED ? Theme.SUCCESS : Theme.ERROR;
                    g2.setColor(edge);
                    g2.fillRect(0, y, 3, rh);
                }
            }
        }
        g2.dispose();
        super.paintComponent(g);
    }

    /** Line-number gutter showing per-row labels (blank for filler rows). */
    private final class Gutter extends JComponent {
        Gutter() {
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(DiffTextArea.this.getFont());
            int maxLen = 2;
            for (String l : labels) {
                maxLen = Math.max(maxLen, l.length());
            }
            return new Dimension(fm.charWidth('0') * maxLen + 24, DiffTextArea.this.getPreferredSize().height);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.hints(g2);
            g2.setFont(DiffTextArea.this.getFont());
            FontMetrics fm = g2.getFontMetrics();
            Rectangle clip = g2.getClipBounds();
            int rh = rowHeightPx();
            int top = DiffTextArea.this.getInsets().top;
            g2.setColor(new Color(255, 255, 255, 14));
            g2.fillRect(getWidth() - 1, clip.y, 1, clip.height);
            if (rh > 0) {
                int first = Math.max(0, (clip.y - top) / rh);
                int last = Math.min(labels.length - 1, (clip.y + clip.height - top) / rh);
                for (int i = first; i <= last; i++) {
                    int y = top + i * rh;
                    Color c = colorFor(kinds[i]);
                    if (c != null && kinds[i] != RowKind.EMPTY) {
                        g2.setColor(c);
                        g2.fillRect(0, y, getWidth() - 1, rh);
                    }
                    String s = labels[i];
                    if (!s.isEmpty()) {
                        g2.setColor(kinds[i] == RowKind.NORMAL ? Theme.TEXT_DIM : Theme.TEXT);
                        g2.drawString(s, getWidth() - 12 - fm.stringWidth(s), y + (rh - fm.getHeight()) / 2 + fm.getAscent());
                    }
                }
            }
            g2.dispose();
        }
    }
}
