package com.satvik.grader.ui.components;

import com.satvik.grader.ui.theme.Theme;

import javax.swing.JSplitPane;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/** Transparent split pane with a small glass grip (the legacy ttk PanedWindow). */
public class GlassSplitPane extends JSplitPane {

    public GlassSplitPane(int orientation) {
        super(orientation);
        setOpaque(false);
        setBorder(null);
        setDividerSize(10);
        setContinuousLayout(true);
    }

    public GlassSplitPane(int orientation, Component a, Component b, double weight) {
        super(orientation, true, a, b);
        setResizeWeight(weight);
        setOpaque(false);
        setBorder(null);
        setDividerSize(10);
        setContinuousLayout(true);
        initialProportion(weight);
    }

    @Override
    public void updateUI() {
        setUI(new GlassSplitUI());
        setBorder(null);
    }

    /** Applies a proportional divider location once the pane first gets a real size. */
    private void initialProportion(double p) {
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int size = getOrientation() == HORIZONTAL_SPLIT ? getWidth() : getHeight();
                if (size > 0) {
                    setDividerLocation(p);
                    removeComponentListener(this);
                }
            }
        });
    }

    private static final class GlassSplitUI extends BasicSplitPaneUI {
        @Override
        public BasicSplitPaneDivider createDefaultDivider() {
            return new GripDivider(this);
        }
    }

    private static final class GripDivider extends BasicSplitPaneDivider {
        private boolean hover;

        GripDivider(BasicSplitPaneUI ui) {
            super(ui);
            MouseAdapter ma = new MouseAdapter() {
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
            };
            addMouseListener(ma);
        }

        @Override
        public void setBorder(Border border) {
            // no border
        }

        @Override
        public void paint(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.hints(g2);
            int w = getWidth();
            int h = getHeight();
            g2.setColor(new Color(255, 255, 255, hover ? 120 : 45));
            if (splitPane.getOrientation() == JSplitPane.HORIZONTAL_SPLIT) {
                int gh = Math.min(44, h / 3);
                g2.fill(new RoundRectangle2D.Float(w / 2f - 2, h / 2f - gh / 2f, 4, gh, 4, 4));
            } else {
                int gw = Math.min(44, w / 3);
                g2.fill(new RoundRectangle2D.Float(w / 2f - gw / 2f, h / 2f - 2, gw, 4, 4, 4));
            }
            g2.dispose();
        }
    }
}
