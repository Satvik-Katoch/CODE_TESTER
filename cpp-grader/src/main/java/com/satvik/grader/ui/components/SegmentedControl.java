package com.satvik.grader.ui.components;

import com.satvik.grader.ui.theme.Theme;

import javax.swing.JComponent;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.function.IntConsumer;

/** iOS-style segmented navigation with a gliding glass indicator. */
public class SegmentedControl extends JComponent {

    private static final int PAD = 4;
    private static final int SEG_PAD_X = 18;
    private static final int HEIGHT = 40;

    private final String[] labels;
    private final boolean[] enabled;
    private int selected;
    private int hoverIndex = -1;
    private float indicatorX = -1;
    private float indicatorW;
    private final Timer anim;
    private IntConsumer onSelect = i -> { };

    public SegmentedControl(String... labels) {
        this.labels = labels;
        this.enabled = new boolean[labels.length];
        java.util.Arrays.fill(enabled, true);
        setFont(Theme.uiSemibold(13f));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setOpaque(false);

        anim = new Timer(15, e -> {
            float[] t = target(selected);
            indicatorX += (t[0] - indicatorX) * 0.28f;
            indicatorW += (t[1] - indicatorW) * 0.28f;
            if (Math.abs(t[0] - indicatorX) < 0.5f && Math.abs(t[1] - indicatorW) < 0.5f) {
                indicatorX = t[0];
                indicatorW = t[1];
                ((Timer) e.getSource()).stop();
            }
            repaint();
        });

        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int i = indexAt(e.getX());
                if (i >= 0 && enabled[i] && i != selected) {
                    setSelected(i);
                    onSelect.accept(i);
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                int i = indexAt(e.getX());
                if (i != hoverIndex) {
                    hoverIndex = i;
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hoverIndex = -1;
                repaint();
            }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
    }

    public void setOnSelect(IntConsumer onSelect) {
        this.onSelect = onSelect;
    }

    public int getSelected() {
        return selected;
    }

    /** Programmatic selection (does not fire the listener). */
    public void setSelected(int i) {
        selected = i;
        if (indicatorX < 0) {
            float[] t = target(i);
            indicatorX = t[0];
            indicatorW = t[1];
            repaint();
        } else if (!anim.isRunning()) {
            anim.start();
        }
    }

    public void setSegmentEnabled(int i, boolean on) {
        enabled[i] = on;
        repaint();
    }

    public boolean isSegmentEnabled(int i) {
        return enabled[i];
    }

    private int segWidth(int i) {
        FontMetrics fm = getFontMetrics(getFont());
        return fm.stringWidth(labels[i]) + 2 * SEG_PAD_X;
    }

    private float[] target(int i) {
        float x = PAD;
        for (int k = 0; k < i; k++) {
            x += segWidth(k);
        }
        return new float[]{x, segWidth(i)};
    }

    private int indexAt(int px) {
        int x = PAD;
        for (int i = 0; i < labels.length; i++) {
            int w = segWidth(i);
            if (px >= x && px < x + w) {
                return i;
            }
            x += w;
        }
        return -1;
    }

    @Override
    public Dimension getPreferredSize() {
        int w = 2 * PAD;
        for (int i = 0; i < labels.length; i++) {
            w += segWidth(i);
        }
        return new Dimension(w, HEIGHT);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.hints(g2);
        int w = getWidth();
        int h = getHeight();
        if (indicatorX < 0) {
            float[] t = target(selected);
            indicatorX = t[0];
            indicatorW = t[1];
        }

        RoundRectangle2D track = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1f, h - 1f, h - 1f, h - 1f);
        g2.setPaint(new GradientPaint(0, 0, new Color(0, 0, 0, 70), 0, h, new Color(0, 0, 0, 40)));
        g2.fill(track);
        g2.setColor(new Color(255, 255, 255, 40));
        g2.draw(track);

        float ih = h - 2f * PAD;
        RoundRectangle2D ind = new RoundRectangle2D.Float(indicatorX, PAD, indicatorW, ih, ih, ih);
        g2.setPaint(new GradientPaint(indicatorX, 0, Theme.ACCENT, indicatorX + indicatorW, 0, Theme.ACCENT_2));
        g2.fill(ind);
        g2.setPaint(new GradientPaint(0, PAD, new Color(255, 255, 255, 90), 0, PAD + ih * 0.6f, new Color(255, 255, 255, 0)));
        g2.fill(ind);
        g2.setColor(new Color(255, 255, 255, 90));
        g2.draw(ind);

        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        int x = PAD;
        for (int i = 0; i < labels.length; i++) {
            int sw = segWidth(i);
            Color c;
            if (!enabled[i]) {
                c = Theme.alpha(Theme.TEXT_DIM, 150);
            } else if (i == selected) {
                c = Color.WHITE;
            } else if (i == hoverIndex) {
                c = Theme.TEXT;
            } else {
                c = Theme.TEXT_SECONDARY;
            }
            g2.setColor(c);
            g2.drawString(labels[i], x + (sw - fm.stringWidth(labels[i])) / 2, (h - fm.getHeight()) / 2 + fm.getAscent());
            x += sw;
        }
        g2.dispose();
    }
}
