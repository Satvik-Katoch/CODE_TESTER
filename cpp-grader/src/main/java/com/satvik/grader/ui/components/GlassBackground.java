package com.satvik.grader.ui.components;

import com.satvik.grader.ui.theme.Theme;

import javax.swing.JPanel;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.LayoutManager;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.Transparency;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

/**
 * The opaque root of the window: a deep gradient with large, soft colour "blobs" that the
 * translucent glass panels sit on top of.
 */
public class GlassBackground extends JPanel {

    private BufferedImage cache;

    public GlassBackground(LayoutManager layout) {
        super(layout);
        setOpaque(true);
        setBackground(Theme.BG_TOP);
    }

    @Override
    protected void paintComponent(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        if (cache == null || cache.getWidth() != w || cache.getHeight() != h) {
            cache = render(w, h);
        }
        g.drawImage(cache, 0, 0, null);
    }

    private BufferedImage render(int w, int h) {
        GraphicsConfiguration gc = getGraphicsConfiguration();
        BufferedImage img = gc != null
                ? gc.createCompatibleImage(w, h, Transparency.OPAQUE)
                : new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        Theme.hints(g);

        g.setPaint(new GradientPaint(0, 0, Theme.BG_TOP, w, h, Theme.BG_BOTTOM));
        g.fillRect(0, 0, w, h);

        float m = Math.max(w, h);
        blob(g, w, h, w * 0.10f, h * 0.05f, m * 0.55f, Theme.BLOB_VIOLET, 150);
        blob(g, w, h, w * 0.92f, h * 0.18f, m * 0.45f, Theme.BLOB_CYAN, 105);
        blob(g, w, h, w * 0.62f, h * 1.02f, m * 0.50f, Theme.BLOB_PINK, 85);
        blob(g, w, h, w * 0.05f, h * 0.95f, m * 0.35f, Theme.BLOB_TEAL, 70);
        blob(g, w, h, w * 0.45f, h * 0.40f, m * 0.30f, Theme.BLOB_VIOLET, 40);

        // vignette
        g.setComposite(AlphaComposite.SrcOver);
        g.setPaint(new RadialGradientPaint(new Point2D.Float(w / 2f, h / 2f), m * 0.75f,
                new float[]{0.55f, 1f}, new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 120)},
                MultipleGradientPaint.CycleMethod.NO_CYCLE));
        g.fillRect(0, 0, w, h);
        g.dispose();
        return img;
    }

    private static void blob(Graphics2D g, int w, int h, float cx, float cy, float radius, Color c, int alpha) {
        g.setPaint(new RadialGradientPaint(new Point2D.Float(cx, cy), radius,
                new float[]{0f, 0.45f, 1f},
                new Color[]{Theme.alpha(c, alpha), Theme.alpha(c, alpha / 3), Theme.alpha(c, 0)},
                MultipleGradientPaint.CycleMethod.NO_CYCLE));
        g.fillRect(0, 0, w, h);
    }
}
