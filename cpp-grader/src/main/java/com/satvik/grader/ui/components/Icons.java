package com.satvik.grader.ui.components;

import com.satvik.grader.ui.theme.Theme;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.function.BiConsumer;

/** Small vector icons drawn with Java2D (no font-glyph dependencies). */
public final class Icons {

    private Icons() {
    }

    public static Icon play(int size) {
        return new ShapeIcon(size, (g, s) -> {
            Path2D p = new Path2D.Float();
            p.moveTo(s * 0.26, s * 0.16);
            p.lineTo(s * 0.86, s * 0.50);
            p.lineTo(s * 0.26, s * 0.84);
            p.closePath();
            g.fill(p);
        });
    }

    public static Icon stop(int size) {
        return new ShapeIcon(size, (g, s) -> g.fill(new RoundRectangle2D.Float(s * 0.2f, s * 0.2f, s * 0.6f, s * 0.6f, s * 0.2f, s * 0.2f)));
    }

    public static Icon chevronDown(int size) {
        return new ShapeIcon(size, (g, s) -> {
            Path2D p = new Path2D.Float();
            p.moveTo(s * 0.2, s * 0.38);
            p.lineTo(s * 0.5, s * 0.66);
            p.lineTo(s * 0.8, s * 0.38);
            g.draw(p);
        });
    }

    public static Icon reset(int size) {
        return new ShapeIcon(size, (g, s) -> {
            g.draw(new Arc2D.Float(s * 0.16f, s * 0.16f, s * 0.68f, s * 0.68f, 90, 280, Arc2D.OPEN));
            Path2D p = new Path2D.Float();
            p.moveTo(s * 0.50, s * 0.02);
            p.lineTo(s * 0.66, s * 0.16);
            p.lineTo(s * 0.50, s * 0.30);
            g.draw(p);
        });
    }

    public static Icon folder(int size) {
        return new ShapeIcon(size, (g, s) -> {
            Path2D p = new Path2D.Float();
            p.moveTo(s * 0.08, s * 0.26);
            p.lineTo(s * 0.38, s * 0.26);
            p.lineTo(s * 0.48, s * 0.36);
            p.lineTo(s * 0.92, s * 0.36);
            p.lineTo(s * 0.92, s * 0.80);
            p.lineTo(s * 0.08, s * 0.80);
            p.closePath();
            g.draw(p);
        });
    }

    public static Icon copy(int size) {
        return new ShapeIcon(size, (g, s) -> {
            g.draw(new RoundRectangle2D.Float(s * 0.30f, s * 0.30f, s * 0.56f, s * 0.56f, s * 0.14f, s * 0.14f));
            Path2D p = new Path2D.Float();
            p.moveTo(s * 0.16, s * 0.66);
            p.lineTo(s * 0.16, s * 0.16);
            p.lineTo(s * 0.66, s * 0.16);
            g.draw(p);
        });
    }

    public static Icon arrowRight(int size) {
        return new ShapeIcon(size, (g, s) -> {
            Path2D p = new Path2D.Float();
            p.moveTo(s * 0.14, s * 0.5);
            p.lineTo(s * 0.84, s * 0.5);
            p.moveTo(s * 0.58, s * 0.24);
            p.lineTo(s * 0.86, s * 0.5);
            p.lineTo(s * 0.58, s * 0.76);
            g.draw(p);
        });
    }

    /** Paints with the component's foreground colour. */
    private static final class ShapeIcon implements Icon {
        private final int size;
        private final BiConsumer<Graphics2D, Float> painter;

        ShapeIcon(int size, BiConsumer<Graphics2D, Float> painter) {
            this.size = size;
            this.painter = painter;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.hints(g2);
            g2.translate(x, y);
            Color col = c != null ? c.getForeground() : Theme.TEXT;
            g2.setColor(col);
            g2.setStroke(new BasicStroke(Math.max(1.4f, size / 9f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            painter.accept(g2, (float) size);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }
}
