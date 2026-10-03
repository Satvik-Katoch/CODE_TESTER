package com.satvik.grader.ui.components;

import com.satvik.grader.ui.theme.Theme;

import javax.swing.JComponent;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

/** Coloured verdict pill ("Accepted", "Wrong Answer", ...). Pulses while running. */
public class VerdictBadge extends JComponent {

    public enum State {
        READY("Ready", Theme.TEXT_DIM),
        RUNNING("Running", Theme.INFO),
        ACCEPTED("Accepted", Theme.SUCCESS),
        WRONG_ANSWER("Wrong Answer", Theme.ERROR),
        COMPILATION_ERROR("Compilation Error", Theme.WARNING),
        RUNTIME_ERROR("Runtime Error", Theme.ERROR),
        TIME_LIMIT("Time Limit Exceeded", Theme.WARNING),
        ERROR("Error", Theme.ERROR);

        final String label;
        final Color color;

        State(String label, Color color) {
            this.label = label;
            this.color = color;
        }
    }

    private State state = State.READY;
    private String detail;
    private float phase;
    private final Timer pulse;

    public VerdictBadge() {
        setFont(Theme.uiSemibold(12f));
        setOpaque(false);
        pulse = new Timer(40, e -> {
            phase += 0.12f;
            repaint();
        });
    }

    public void setState(State s) {
        setState(s, null);
    }

    public void setState(State s, String detail) {
        this.state = s;
        this.detail = detail;
        if (s == State.RUNNING) {
            pulse.start();
        } else {
            pulse.stop();
        }
        revalidate();
        repaint();
    }

    private String text() {
        return detail == null ? state.label : state.label + "  \u00B7  " + detail;
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        return new Dimension(fm.stringWidth(text()) + 36, 26);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.hints(g2);
        int w = getWidth();
        int h = getHeight();
        Color c = state.color;
        RoundRectangle2D pill = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1f, h - 1f, h - 1f, h - 1f);
        g2.setColor(Theme.alpha(c, 38));
        g2.fill(pill);
        g2.setColor(Theme.alpha(c, 130));
        g2.draw(pill);

        float glow = state == State.RUNNING ? (float) (0.5 + 0.5 * Math.sin(phase)) : 1f;
        float d = 8f;
        float cx = 12f;
        float cy = h / 2f;
        g2.setColor(Theme.alpha(c, (int) (70 * glow)));
        g2.fill(new Ellipse2D.Float(cx - d, cy - d, 2 * d, 2 * d));
        g2.setColor(Theme.alpha(c, (int) (120 + 135 * glow)));
        g2.fill(new Ellipse2D.Float(cx - d / 2, cy - d / 2, d, d));

        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(Theme.mix(c, Color.WHITE, 0.25f));
        g2.drawString(text(), 24, (h - fm.getHeight()) / 2 + fm.getAscent());
        g2.dispose();
    }
}
