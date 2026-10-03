package com.satvik.grader.ui.components;

import com.satvik.grader.ui.theme.Theme;

import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;

/**
 * A translucent "liquid glass" card: soft shadow, frosted white tint with a top sheen,
 * subtle grain and a specular gradient rim.
 */
public class GlassPanel extends JPanel {

    public static final int SHADOW = 4;

    private final int arc;

    public GlassPanel(LayoutManager layout) {
        this(layout, 22);
    }

    public GlassPanel(LayoutManager layout, int arc) {
        super(layout);
        this.arc = arc;
        setOpaque(false);
        padding(12, 14, 12, 14);
    }

    public GlassPanel padding(int top, int left, int bottom, int right) {
        setBorder(new EmptyBorder(SHADOW + top, SHADOW + left, SHADOW + bottom, SHADOW + right));
        return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.hints(g2);
        float x = SHADOW;
        float y = SHADOW;
        float w = getWidth() - 2f * SHADOW;
        float h = getHeight() - 2f * SHADOW;
        if (w <= 0 || h <= 0) {
            g2.dispose();
            return;
        }

        // soft drop shadow
        for (int i = SHADOW; i >= 1; i--) {
            g2.setColor(new Color(0, 0, 0, 9));
            g2.fill(new RoundRectangle2D.Float(x - i, y - i + 2, w + 2 * i, h + 2 * i, arc + 2 * i, arc + 2 * i));
        }

        Shape body = new RoundRectangle2D.Float(x, y, w, h, arc, arc);

        // frosted tint
        g2.setPaint(new GradientPaint(0, y, Theme.GLASS_TOP, 0, y + h, Theme.GLASS_BOTTOM));
        g2.fill(body);

        // top sheen (light caught by the curved glass edge)
        float sheen = Math.min(h * 0.5f, 70f);
        g2.setPaint(new GradientPaint(0, y, new Color(255, 255, 255, 26), 0, y + sheen, new Color(255, 255, 255, 0)));
        g2.fill(body);

        // grain
        Graphics2D gn = (Graphics2D) g2.create();
        gn.clip(body);
        gn.setPaint(Theme.noise());
        gn.fill(body);
        gn.dispose();

        // specular rim
        g2.setStroke(new BasicStroke(1f));
        g2.setPaint(new GradientPaint(0, y, Theme.GLASS_EDGE_TOP, 0, y + h, Theme.GLASS_EDGE_BOTTOM));
        g2.draw(new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, w - 1f, h - 1f, arc, arc));

        // inner highlight line just below the top edge
        g2.setPaint(new GradientPaint(x, 0, new Color(255, 255, 255, 0), x + w / 2f, 0, new Color(255, 255, 255, 60), true));
        g2.draw(new RoundRectangle2D.Float(x + 1.5f, y + 1.5f, w - 3f, h - 3f, arc - 2, arc - 2));
        g2.dispose();
    }
}
