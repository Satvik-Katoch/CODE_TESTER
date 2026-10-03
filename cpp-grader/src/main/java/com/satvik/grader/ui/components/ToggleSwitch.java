package com.satvik.grader.ui.components;

import com.satvik.grader.ui.theme.Theme;

import javax.swing.JComponent;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Animated on/off switch with a label (replaces the legacy ttk checkbuttons). */
public class ToggleSwitch extends JComponent {

    private static final int TW = 38;
    private static final int TH = 22;

    private final String text;
    private final List<Consumer<Boolean>> listeners = new ArrayList<>();
    private final Timer anim;
    private boolean selected;
    private boolean hover;
    private float knob;

    public ToggleSwitch(String text, boolean selected) {
        this.text = text == null ? "" : text;
        this.selected = selected;
        this.knob = selected ? 1f : 0f;
        setFont(Theme.ui(13f));
        setForeground(Theme.TEXT_SECONDARY);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setOpaque(false);

        anim = new Timer(15, e -> {
            float t = this.selected ? 1f : 0f;
            knob += (t - knob) * 0.3f;
            if (Math.abs(t - knob) < 0.02f) {
                knob = t;
                ((Timer) e.getSource()).stop();
            }
            repaint();
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (isEnabled() && contains(e.getPoint())) {
                    setSelected(!ToggleSwitch.this.selected, true);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                hover = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = false;
                repaint();
            }
        });
    }

    public void addChangeListener(Consumer<Boolean> l) {
        listeners.add(l);
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean on, boolean fire) {
        if (on == selected) {
            return;
        }
        selected = on;
        if (!anim.isRunning()) {
            anim.start();
        }
        if (fire) {
            listeners.forEach(l -> l.accept(on));
        }
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        int w = TW + (text.isEmpty() ? 0 : 10 + fm.stringWidth(text));
        return new Dimension(w + 2, Math.max(TH, fm.getHeight()) + 4);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.hints(g2);
        if (!isEnabled()) {
            g2.setComposite(AlphaComposite.SrcOver.derive(0.45f));
        }
        int h = getHeight();
        float y0 = (h - TH) / 2f;
        RoundRectangle2D track = new RoundRectangle2D.Float(1, y0, TW, TH, TH, TH);

        g2.setColor(new Color(255, 255, 255, hover ? 40 : 28));
        g2.fill(track);
        if (knob > 0f) {
            Graphics2D gk = (Graphics2D) g2.create();
            gk.setComposite(AlphaComposite.SrcOver.derive(knob * (isEnabled() ? 1f : 0.45f)));
            gk.setPaint(new GradientPaint(1, 0, Theme.ACCENT, 1 + TW, 0, Theme.ACCENT_2));
            gk.fill(track);
            gk.dispose();
        }
        g2.setColor(new Color(255, 255, 255, 70));
        g2.draw(track);

        float d = TH - 6f;
        float kx = 1 + 3 + knob * (TW - TH);
        g2.setColor(new Color(0, 0, 0, 60));
        g2.fill(new Ellipse2D.Float(kx, y0 + 4, d, d));
        g2.setPaint(new GradientPaint(0, y0, Color.WHITE, 0, y0 + TH, new Color(0xDCE3F5)));
        g2.fill(new Ellipse2D.Float(kx, y0 + 3, d, d));

        if (!text.isEmpty()) {
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(selected || hover ? Theme.TEXT : getForeground());
            g2.drawString(text, TW + 11, (h - fm.getHeight()) / 2 + fm.getAscent());
        }
        g2.dispose();
    }
}
