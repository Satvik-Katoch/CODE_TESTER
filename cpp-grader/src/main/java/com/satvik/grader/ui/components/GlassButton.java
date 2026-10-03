package com.satvik.grader.ui.components;

import com.satvik.grader.ui.theme.Theme;

import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;

/** Pill-shaped glass / gradient button with a smooth hover animation. */
public class GlassButton extends JButton {

    public enum Variant { PRIMARY, DANGER, GLASS, TOOL }

    private final Variant variant;
    private final Timer hoverTimer;
    private float hover;
    private boolean iconOnRight;

    public GlassButton(String text) {
        this(text, null, Variant.GLASS);
    }

    public GlassButton(String text, Variant variant) {
        this(text, null, variant);
    }

    public GlassButton(String text, Icon icon, Variant variant) {
        super(text, icon);
        this.variant = variant;
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setForeground(Theme.TEXT);
        boolean tool = variant == Variant.TOOL;
        setFont(tool ? Theme.ui(12.5f) : Theme.uiSemibold(13.5f));
        setBorder(tool ? new EmptyBorder(4, 12, 4, 12) : new EmptyBorder(8, 18, 8, 18));
        setIconTextGap(7);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        hoverTimer = new Timer(15, e -> {
            float target = getModel().isRollover() && isEnabled() ? 1f : 0f;
            hover += (target - hover) * 0.25f;
            if (Math.abs(target - hover) < 0.02f) {
                hover = target;
                ((Timer) e.getSource()).stop();
            }
            repaint();
        });
        getModel().addChangeListener(e -> {
            if (!hoverTimer.isRunning()) {
                hoverTimer.start();
            }
        });
    }

    @Override
    public void updateUI() {
        setUI(new BasicButtonUI());
    }

    public void setIconOnRight(boolean right) {
        this.iconOnRight = right;
        repaint();
    }

    public Variant variant() {
        return variant;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.hints(g2);
        int w = getWidth();
        int h = getHeight();
        if (!isEnabled()) {
            g2.setComposite(AlphaComposite.SrcOver.derive(0.42f));
        }
        ButtonModel m = getModel();
        boolean pressed = m.isArmed() && m.isPressed();
        Shape shape = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1f, h - 1f, h - 1f, h - 1f);

        if (variant == Variant.PRIMARY || variant == Variant.DANGER) {
            Color a = variant == Variant.PRIMARY ? Theme.ACCENT : Theme.DANGER;
            Color b = variant == Variant.PRIMARY ? Theme.ACCENT_2 : Theme.DANGER_2;
            g2.setPaint(new GradientPaint(0, 0, a, w, h, b));
            g2.fill(shape);
            g2.setPaint(new GradientPaint(0, 0, new Color(255, 255, 255, (int) (75 + 45 * hover)),
                    0, h * 0.62f, new Color(255, 255, 255, 0)));
            g2.fill(shape);
            if (pressed) {
                g2.setColor(new Color(0, 0, 0, 55));
                g2.fill(shape);
            }
            g2.setColor(new Color(255, 255, 255, (int) (70 + 50 * hover)));
            g2.draw(shape);
        } else {
            g2.setPaint(new GradientPaint(0, 0, new Color(255, 255, 255, (int) (30 + 30 * hover)),
                    0, h, new Color(255, 255, 255, (int) (10 + 18 * hover))));
            g2.fill(shape);
            if (pressed) {
                g2.setColor(new Color(0, 0, 0, 45));
                g2.fill(shape);
            }
            g2.setPaint(new GradientPaint(0, 0, new Color(255, 255, 255, (int) (85 + 60 * hover)),
                    0, h, new Color(255, 255, 255, 22)));
            g2.draw(shape);
        }
        paintContent(g2, w, h);
        g2.dispose();
    }

    private void paintContent(Graphics2D g2, int w, int h) {
        g2.setFont(getFont());
        g2.setColor(getForeground());
        FontMetrics fm = g2.getFontMetrics();
        String text = getText();
        int tw = (text == null || text.isEmpty()) ? 0 : fm.stringWidth(text);
        Icon icon = getIcon();
        int iw = icon == null ? 0 : icon.getIconWidth();
        int gap = (tw > 0 && iw > 0) ? getIconTextGap() : 0;
        int x = (w - (tw + iw + gap)) / 2;
        int baseline = (h - fm.getHeight()) / 2 + fm.getAscent();
        if (iconOnRight) {
            if (tw > 0) {
                g2.drawString(text, x, baseline);
            }
            if (icon != null) {
                icon.paintIcon(this, g2, x + tw + gap, (h - icon.getIconHeight()) / 2);
            }
        } else {
            if (icon != null) {
                icon.paintIcon(this, g2, x, (h - icon.getIconHeight()) / 2);
            }
            if (tw > 0) {
                g2.drawString(text, x + iw + gap, baseline);
            }
        }
    }
}
