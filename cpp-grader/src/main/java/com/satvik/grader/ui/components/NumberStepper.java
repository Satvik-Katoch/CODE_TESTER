package com.satvik.grader.ui.components;

import com.satvik.grader.ui.theme.Theme;

import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.BorderLayout;
import java.awt.Dimension;

/** Integer input with -/+ glass buttons (replaces the ttk Spinbox). */
public class NumberStepper extends JPanel {

    private final GlassTextField field;
    private final int min;
    private final int max;
    private final int step;

    public NumberStepper(int value, int min, int max, int step) {
        super(new BorderLayout(4, 0));
        this.min = min;
        this.max = max;
        this.step = step;
        setOpaque(false);

        field = new GlassTextField(String.valueOf(value), 5);
        field.setHorizontalAlignment(SwingConstants.CENTER);
        field.setPreferredSize(new Dimension(76, field.getPreferredSize().height));
        ((AbstractDocument) field.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String s, AttributeSet a) throws BadLocationException {
                super.insertString(fb, offset, digits(s), a);
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String s, AttributeSet a) throws BadLocationException {
                super.replace(fb, offset, length, digits(s), a);
            }
        });
        field.addActionListener(e -> setValue(getValue()));
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                setValue(getValue());
            }
        });

        GlassButton minus = new GlassButton("\u2212", GlassButton.Variant.TOOL);
        GlassButton plus = new GlassButton("+", GlassButton.Variant.TOOL);
        minus.setFont(Theme.uiSemibold(14f));
        plus.setFont(Theme.uiSemibold(14f));
        minus.addActionListener(e -> setValue(getValue() - this.step));
        plus.addActionListener(e -> setValue(getValue() + this.step));

        add(minus, BorderLayout.WEST);
        add(field, BorderLayout.CENTER);
        add(plus, BorderLayout.EAST);
    }

    private static String digits(String s) {
        return s == null ? "" : s.replaceAll("[^0-9]", "");
    }

    public int getValue() {
        try {
            long v = Long.parseLong(field.getText().trim());
            return (int) Math.max(min, Math.min(max, v));
        } catch (NumberFormatException e) {
            return min;
        }
    }

    public void setValue(int v) {
        field.setText(String.valueOf(Math.max(min, Math.min(max, v))));
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        for (java.awt.Component c : getComponents()) {
            c.setEnabled(enabled);
        }
    }
}
