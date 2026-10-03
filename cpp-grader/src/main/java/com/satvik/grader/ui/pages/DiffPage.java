package com.satvik.grader.ui.pages;

import com.satvik.grader.core.DiffEngine;
import com.satvik.grader.ui.components.GlassSplitPane;
import com.satvik.grader.ui.components.SurfacePanel;
import com.satvik.grader.ui.editor.DiffTextArea;
import com.satvik.grader.ui.theme.Theme;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class DiffPage extends JPanel {
    private final DiffTextArea leftDiff;
    private final DiffTextArea rightDiff;

    public DiffPage() {
        setOpaque(false);
        setLayout(new BorderLayout());

        leftDiff = new DiffTextArea();
        rightDiff = new DiffTextArea();

        // Share scrollbar
        JScrollPane leftScroll = new JScrollPane(leftDiff);
        leftScroll.setRowHeaderView(leftDiff.gutter());
        leftScroll.setBorder(null);
        leftScroll.setOpaque(false);
        leftScroll.getViewport().setOpaque(false);
        
        JScrollPane rightScroll = new JScrollPane(rightDiff);
        rightScroll.setRowHeaderView(rightDiff.gutter());
        rightScroll.setBorder(null);
        rightScroll.setOpaque(false);
        rightScroll.getViewport().setOpaque(false);
        
        rightScroll.getVerticalScrollBar().setModel(leftScroll.getVerticalScrollBar().getModel());

        GlassSplitPane split = new GlassSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setResizeWeight(0.5);
        
        split.setLeftComponent(createPanel("Expected Output", leftScroll));
        split.setRightComponent(createPanel("Your Output", rightScroll));

        add(split, BorderLayout.CENTER);
    }
    
    private SurfacePanel createPanel(String title, JScrollPane scroll) {
        JPanel container = new JPanel(new BorderLayout());
        container.setOpaque(false);
        
        JLabel label = new JLabel(title);
        label.setFont(Theme.TITLE_FONT);
        label.setForeground(Theme.TEXT_PRIMARY);
        label.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        
        container.add(label, BorderLayout.NORTH);
        container.add(scroll, BorderLayout.CENTER);
        
        return new SurfacePanel(container, null);
    }

    public void updateDiff(String expectedStr, String actualStr) {
        List<String> expected = expectedStr.lines().toList();
        List<String> actual = actualStr.lines().toList();
        
        List<DiffEngine.OpCode> ops = DiffEngine.opcodes(expected, actual);
        
        List<String> lLines = new ArrayList<>();
        List<DiffTextArea.RowKind> lKinds = new ArrayList<>();
        List<String> lLabels = new ArrayList<>();
        
        List<String> rLines = new ArrayList<>();
        List<DiffTextArea.RowKind> rKinds = new ArrayList<>();
        List<String> rLabels = new ArrayList<>();
        
        int lLineNum = 1;
        int rLineNum = 1;
        
        for (DiffEngine.OpCode op : ops) {
            switch (op.tag()) {
                case EQUAL -> {
                    for (int i = op.i1(); i < op.i2(); i++) {
                        lLines.add(expected.get(i));
                        lKinds.add(DiffTextArea.RowKind.NORMAL);
                        lLabels.add(String.valueOf(lLineNum++));
                        
                        rLines.add(actual.get(i - op.i1() + op.j1()));
                        rKinds.add(DiffTextArea.RowKind.NORMAL);
                        rLabels.add(String.valueOf(rLineNum++));
                    }
                }
                case DELETE -> {
                    for (int i = op.i1(); i < op.i2(); i++) {
                        lLines.add(expected.get(i));
                        lKinds.add(DiffTextArea.RowKind.REMOVED);
                        lLabels.add(String.valueOf(lLineNum++));
                        
                        rLines.add("");
                        rKinds.add(DiffTextArea.RowKind.EMPTY);
                        rLabels.add("");
                    }
                }
                case INSERT -> {
                    for (int j = op.j1(); j < op.j2(); j++) {
                        lLines.add("");
                        lKinds.add(DiffTextArea.RowKind.EMPTY);
                        lLabels.add("");
                        
                        rLines.add(actual.get(j));
                        rKinds.add(DiffTextArea.RowKind.ADDED);
                        rLabels.add(String.valueOf(rLineNum++));
                    }
                }
                case REPLACE -> {
                    int len1 = op.i2() - op.i1();
                    int len2 = op.j2() - op.j1();
                    int maxLen = Math.max(len1, len2);
                    
                    for (int k = 0; k < maxLen; k++) {
                        if (k < len1) {
                            lLines.add(expected.get(op.i1() + k));
                            lKinds.add(DiffTextArea.RowKind.REMOVED);
                            lLabels.add(String.valueOf(lLineNum++));
                        } else {
                            lLines.add("");
                            lKinds.add(DiffTextArea.RowKind.EMPTY);
                            lLabels.add("");
                        }
                        
                        if (k < len2) {
                            rLines.add(actual.get(op.j1() + k));
                            rKinds.add(DiffTextArea.RowKind.ADDED);
                            rLabels.add(String.valueOf(rLineNum++));
                        } else {
                            rLines.add("");
                            rKinds.add(DiffTextArea.RowKind.EMPTY);
                            rLabels.add("");
                        }
                    }
                }
            }
        }
        
        leftDiff.setRows(lLines, lKinds, lLabels);
        rightDiff.setRows(rLines, rKinds, rLabels);
    }
}
