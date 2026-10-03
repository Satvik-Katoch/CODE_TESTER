package com.satvik.grader.ui.components;

import com.satvik.grader.ui.theme.Theme;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.RoundRectangle2D;

/** Minimal translucent overlay-style scrollbar: no arrows, no track, rounded thumb. */
public class GlassScrollBarUI extends BasicScrollBarUI {

    public static final int THICKNESS = 10;

    public static void apply(JScrollBar sb) {
        sb.setUI(new GlassScrollBarUI());
        sb.setOpaque(false);
        sb.setUnitIncrement(18);
        if (sb.getOrientation() == JScrollBar.VERTICAL) {
            sb.setPreferredSize(new Dimension(THICKNESS, 0));
        } else {
            sb.setPreferredSize(new Dimension(0, THICKNESS));
        }
    }

    @Override
    protected void configureScrollBarColors() {
        // colours are painted directly
    }

    @Override
    protected JButton createDecreaseButton(int orientation) {
        return zeroButton();
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        return zeroButton();
    }

    private static JButton zeroButton() {
        JButton b = new JButton();
        Dimension zero = new Dimension(0, 0);
        b.setPreferredSize(zero);
        b.setMinimumSize(zero);
        b.setMaximumSize(zero);
        return b;
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
        // transparent track
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
        if (r.isEmpty() || !scrollbar.isEnabled()) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.hints(g2);
        boolean vertical = scrollbar.getOrientation() == JScrollBar.VERTICAL;
        int pad = 2;
        float x = r.x + pad;
        float y = r.y + pad;
        float w = r.width - 2f * pad;
        float h = r.height - 2f * pad;
        float arc = vertical ? w : h;
        g2.setColor(new Color(255, 255, 255, isThumbRollover() || isDragging ? 105 : 55));
        g2.fill(new RoundRectangle2D.Float(x, y, w, h, arc, arc));
        g2.dispose();
    }

    @Override
    protected Dimension getMinimumThumbSize() {
        return new Dimension(THICKNESS, 28);
    }
}
