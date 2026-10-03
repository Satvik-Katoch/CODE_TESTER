package com.satvik.grader.ui.components;

import com.satvik.grader.ui.theme.Theme;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.RoundRectangle2D;

/** Dark, slightly translucent rounded "well" that hosts editors and output panes inside a glass card. */
public class SurfacePanel extends JPanel {

    private static final int ARC = 16;
    private boolean focused;
    private boolean dimmed;

    public SurfacePanel() {
        super(new BorderLayout());
        setOpaque(false);
        setBorder(new EmptyBorder(5, 5, 5, 5));
    }

    public SurfacePanel(JComponent content, JComponent focusSource) {
        this();
        add(content, BorderLayout.CENTER);
        if (focusSource != null) {
            focusSource.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    focused = true;
                    repaint();
                }

                @Override
                public void focusLost(FocusEvent e) {
                    focused = false;
                    repaint();
                }
            });
        }
    }

    public void setDimmed(boolean dimmed) {
        this.dimmed = dimmed;
        repaint();
    }

    private Shape shape() {
        return new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f, ARC, ARC);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.hints(g2);
        Shape s = shape();
        g2.setColor(Theme.SURFACE);
        g2.fill(s);
        // inner top shadow for depth
        g2.setPaint(new java.awt.GradientPaint(0, 0, new Color(0, 0, 0, 60), 0, 14, new Color(0, 0, 0, 0)));
        g2.fill(s);
        if (focused && !dimmed) {
            g2.setStroke(new BasicStroke(1.3f));
            g2.setColor(Theme.alpha(Theme.ACCENT, 165));
        } else {
            g2.setStroke(new BasicStroke(1f));
            g2.setColor(Theme.SURFACE_EDGE);
        }
        g2.draw(s);
        g2.dispose();
    }

    @Override
    protected void paintChildren(Graphics g) {
        super.paintChildren(g);
        if (dimmed) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.hints(g2);
            g2.setColor(new Color(14, 16, 30, 140));
            g2.fill(shape());
            g2.dispose();
        }
    }
}
