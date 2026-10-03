package com.satvik.grader.ui.components;

import com.satvik.grader.ui.theme.Theme;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;

/** Thin rounded progress bar with a gradient fill. */
public class GlassProgressBar extends JComponent {

    private double progress;
    private Color from = Theme.ACCENT;
    private Color to = Theme.ACCENT_2;

    public GlassProgressBar() {
        setOpaque(false);
    }

    public void setProgress(double p) {
        progress = Math.max(0, Math.min(1, p));
        repaint();
    }

    public void setColors(Color from, Color to) {
        this.from = from;
        this.to = to;
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(200, 8);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.hints(g2);
        int w = getWidth();
        int bh = 8;
        int y = (getHeight() - bh) / 2;
        g2.setColor(new Color(255, 255, 255, 24));
        g2.fill(new RoundRectangle2D.Float(0, y, w, bh, bh, bh));
        float fw = (float) (w * progress);
        if (fw > 0) {
            g2.setPaint(new GradientPaint(0, 0, from, Math.max(fw, 1), 0, to));
            g2.fill(new RoundRectangle2D.Float(0, y, Math.max(fw, bh), bh, bh, bh));
            g2.setColor(new Color(255, 255, 255, 70));
            g2.fill(new RoundRectangle2D.Float(2, y + 1, Math.max(fw - 4, 0), bh / 2.5f, bh / 2.5f, bh / 2.5f));
        }
        g2.dispose();
    }
}
