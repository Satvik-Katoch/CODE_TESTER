package com.satvik.grader.ui.components;

import com.satvik.grader.ui.theme.Theme;

import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicTextFieldUI;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.RoundRectangle2D;

/** Dark translucent rounded text field with an accent focus ring and optional placeholder. */
public class GlassTextField extends JTextField {

    private String placeholder;

    public GlassTextField() {
        this("", 0);
    }

    public GlassTextField(String text, int columns) {
        super(text, columns);
        setOpaque(false);
        setBorder(new EmptyBorder(7, 12, 7, 12));
        setFont(Theme.mono(13f));
        setForeground(Theme.TEXT);
        setCaretColor(Theme.TEXT);
        setSelectionColor(Theme.SELECTION);
        setSelectedTextColor(Theme.TEXT);
        setDisabledTextColor(Theme.TEXT_DIM);
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                repaint();
            }
        });
    }

    @Override
    public void updateUI() {
        setUI(new BasicTextFieldUI());
    }

    public GlassTextField placeholder(String text) {
        this.placeholder = text;
        repaint();
        return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.hints(g2);
        int w = getWidth();
        int h = getHeight();
        RoundRectangle2D r = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1f, h - 1f, 14, 14);
        g2.setColor(isEnabled() ? new Color(4, 6, 15, 150) : new Color(255, 255, 255, 8));
        g2.fill(r);
        if (hasFocus()) {
            g2.setStroke(new BasicStroke(3f));
            g2.setColor(Theme.alpha(Theme.ACCENT, 55));
            g2.draw(new RoundRectangle2D.Float(1.5f, 1.5f, w - 3f, h - 3f, 13, 13));
            g2.setStroke(new BasicStroke(1.2f));
            g2.setColor(Theme.alpha(Theme.ACCENT, 200));
        } else {
            g2.setStroke(new BasicStroke(1f));
            g2.setColor(new Color(255, 255, 255, isEnabled() ? 36 : 18));
        }
        g2.draw(r);
        g2.dispose();

        super.paintComponent(g);

        if (placeholder != null && getText().isEmpty() && !hasFocus()) {
            Graphics2D gp = (Graphics2D) g.create();
            Theme.hints(gp);
            gp.setFont(getFont());
            gp.setColor(Theme.TEXT_DIM);
            FontMetrics fm = gp.getFontMetrics();
            Insets in = getInsets();
            gp.drawString(placeholder, in.left, (h - fm.getHeight()) / 2 + fm.getAscent());
            gp.dispose();
        }
    }
}
