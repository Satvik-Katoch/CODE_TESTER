package com.satvik.grader.ui.components;

import com.satvik.grader.ui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.LayoutManager;

/** Small factory helpers to assemble the glass UI consistently. */
public final class UiKit {

    private UiKit() {
    }

    public static JPanel clear(LayoutManager layout) {
        JPanel p = new JPanel(layout);
        p.setOpaque(false);
        return p;
    }

    public static JLabel label(String text, Font font, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(color);
        return l;
    }

    public static JPanel row(int gap, Component... comps) {
        JPanel p = clear(new FlowLayout(FlowLayout.LEFT, gap, 0));
        for (Component c : comps) {
            p.add(c);
        }
        return p;
    }

    public static Component hgap(int w) {
        return Box.createHorizontalStrut(w);
    }

    /** Transparent scroll pane with glass scrollbars. */
    public static JScrollPane scroll(JComponent view) {
        JScrollPane sp = new JScrollPane(view);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setViewportBorder(null);
        GlassScrollBarUI.apply(sp.getVerticalScrollBar());
        GlassScrollBarUI.apply(sp.getHorizontalScrollBar());
        sp.setCorner(ScrollPaneConstants.LOWER_RIGHT_CORNER, clear(null));
        sp.setCorner(ScrollPaneConstants.LOWER_LEFT_CORNER, clear(null));
        sp.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, clear(null));
        sp.setCorner(ScrollPaneConstants.UPPER_LEFT_CORNER, clear(null));
        return sp;
    }

    /** Scroll pane wrapped in a dark rounded surface. */
    public static SurfacePanel surface(JComponent view) {
        return new SurfacePanel(scroll(view), view);
    }

    public static GlassButton tool(String text, Runnable action) {
        GlassButton b = new GlassButton(text, GlassButton.Variant.TOOL);
        b.addActionListener(e -> action.run());
        return b;
    }

    /**
     * A glass card with a title row (title on the left, tool components on the right) and content below.
     */
    public static GlassPanel card(String title, JComponent content, JComponent... tools) {
        GlassPanel card = new GlassPanel(new BorderLayout(0, 10));
        card.padding(12, 14, 14, 14);
        JPanel header = clear(new BorderLayout(10, 0));
        JLabel t = label(title, Theme.uiSemibold(14.5f), Theme.TEXT);
        t.setBorder(new EmptyBorder(0, 4, 0, 0));
        header.add(t, BorderLayout.WEST);
        if (tools.length > 0) {
            header.add(row(6, tools), BorderLayout.EAST);
        }
        card.add(header, BorderLayout.NORTH);
        card.add(content, BorderLayout.CENTER);
        return card;
    }
}
