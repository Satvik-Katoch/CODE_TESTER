package com.satvik.grader.ui.components;

import com.satvik.grader.ui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.plaf.basic.BasicMenuItemUI;
import javax.swing.plaf.basic.BasicPopupMenuUI;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/** Glass button that opens a dark popup list; each entry shows a title and a subtitle line. */
public class GlassDropdown<T> extends GlassButton {

    private final List<T> items;
    private final Function<T, String> title;
    private final Function<T, String> subtitle;
    private T selected;
    private Consumer<T> onChange = t -> { };

    public GlassDropdown(List<T> items, T initial, Function<T, String> title, Function<T, String> subtitle) {
        super("", Icons.chevronDown(12), Variant.GLASS);
        this.items = items;
        this.title = title;
        this.subtitle = subtitle;
        setIconOnRight(true);
        setFont(Theme.uiSemibold(13f));
        setSelectedItem(initial, false);
        addActionListener(e -> showPopup());
    }

    public void setOnChange(Consumer<T> onChange) {
        this.onChange = onChange;
    }

    public T getSelectedItem() {
        return selected;
    }

    public void setSelectedItem(T item, boolean fire) {
        T old = selected;
        selected = item;
        setText(title.apply(item));
        if (fire && old != item) {
            onChange.accept(item);
        }
    }

    private void showPopup() {
        JPopupMenu menu = new JPopupMenu();
        menu.setUI(new BasicPopupMenuUI());
        menu.setBackground(Theme.POPUP_BG);
        menu.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 45)),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        for (T item : items) {
            Item mi = new Item(title.apply(item), subtitle.apply(item), item == selected);
            mi.addActionListener(e -> setSelectedItem(item, true));
            menu.add(mi);
        }
        menu.show(this, 0, getHeight() + 4);
    }

    private static final class Item extends JMenuItem {
        private final String sub;
        private final boolean current;

        Item(String title, String sub, boolean current) {
            super(title);
            this.sub = sub;
            this.current = current;
            setOpaque(true);
            setBackground(Theme.POPUP_BG);
        }

        @Override
        public void updateUI() {
            setUI(new BasicMenuItemUI());
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics f1 = getFontMetrics(Theme.uiSemibold(13f));
            FontMetrics f2 = getFontMetrics(Theme.mono(11f));
            int w = Math.max(f1.stringWidth(getText()) + 60, Math.min(f2.stringWidth(sub) + 46, 560));
            return new Dimension(Math.max(w, 300), 50);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.hints(g2);
            int w = getWidth();
            int h = getHeight();
            g2.setColor(Theme.POPUP_BG);
            g2.fillRect(0, 0, w, h);
            if (getModel().isArmed() || getModel().isSelected()) {
                g2.setColor(Theme.alpha(Theme.ACCENT, 70));
                g2.fill(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, 12, 12));
            }
            if (current) {
                g2.setColor(Theme.ACCENT);
                g2.setStroke(new java.awt.BasicStroke(2f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
                Path2D check = new Path2D.Float();
                check.moveTo(12, h / 2f - 6);
                check.moveTo(11, h / 2f - 1);
                check.lineTo(15, h / 2f + 3);
                check.lineTo(22, h / 2f - 5);
                g2.draw(check);
            }
            g2.setFont(Theme.uiSemibold(13f));
            FontMetrics f1 = g2.getFontMetrics();
            g2.setColor(Theme.TEXT);
            g2.drawString(getText(), 32, 8 + f1.getAscent());
            g2.setFont(Theme.mono(11f));
            FontMetrics f2 = g2.getFontMetrics();
            g2.setColor(Theme.TEXT_DIM);
            String s = sub;
            int max = w - 44;
            if (f2.stringWidth(s) > max) {
                while (s.length() > 1 && f2.stringWidth(s + "...") > max) {
                    s = s.substring(0, s.length() - 1);
                }
                s += "...";
            }
            g2.drawString(s, 32, h - 9 - f2.getDescent());
            g2.dispose();
        }
    }
}
