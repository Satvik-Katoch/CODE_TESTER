package com.satvik.grader.ui.pages;

import com.satvik.grader.core.SingleTestRunner;
import com.satvik.grader.ui.components.GlassButton;
import com.satvik.grader.ui.components.GlassSplitPane;
import com.satvik.grader.ui.components.SurfacePanel;
import com.satvik.grader.ui.editor.CodeEditor;
import com.satvik.grader.ui.editor.PlainTextArea;
import com.satvik.grader.ui.editor.StatusPane;
import com.satvik.grader.ui.theme.Theme;

import javax.swing.*;
import java.awt.*;

public class EditorPage extends JPanel {
    private final CodeEditor codeEditor;
    private final PlainTextArea inputArea;
    private final PlainTextArea expectedArea;
    private final StatusPane statusPane;
    
    private final JCheckBox useFilePathCheckbox;
    private final com.satvik.grader.ui.components.GlassTextField filePathField;
    private final GlassButton browseBtn;
    private final JLabel statsLabel;

    public EditorPage() {
        setOpaque(false);
        setLayout(new BorderLayout());

        codeEditor = new CodeEditor();
        codeEditor.setText("#include <iostream>\n\nusing namespace std;\n\nint main() {\n    int n;\n    cin >> n;\n    cout << n * 2 << endl;\n    return 0;\n}");

        inputArea = new PlainTextArea();
        expectedArea = new PlainTextArea();
        statusPane = new StatusPane();

        GlassSplitPane mainSplit = new GlassSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setResizeWeight(0.6);

        // Left: Editor
        JPanel editorContainer = new JPanel(new BorderLayout());
        editorContainer.setOpaque(false);
        
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        
        JLabel editorLabel = new JLabel("C++ Code (Optimized)");
        editorLabel.setFont(Theme.TITLE_FONT);
        editorLabel.setForeground(Theme.TEXT_PRIMARY);
        editorLabel.setBorder(BorderFactory.createEmptyBorder(10, 15, 5, 15));
        headerPanel.add(editorLabel, BorderLayout.NORTH);
        
        JPanel fileInputPanel = new JPanel(new GridBagLayout());
        fileInputPanel.setOpaque(false);
        fileInputPanel.setBorder(BorderFactory.createEmptyBorder(0, 15, 10, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 10);
        
        useFilePathCheckbox = new JCheckBox("Use File Path:");
        useFilePathCheckbox.setOpaque(false);
        useFilePathCheckbox.setForeground(Theme.TEXT_PRIMARY);
        useFilePathCheckbox.setFont(Theme.UI_FONT);
        gbc.gridx = 0; gbc.weightx = 0;
        fileInputPanel.add(useFilePathCheckbox, gbc);
        
        filePathField = new com.satvik.grader.ui.components.GlassTextField();
        filePathField.setEnabled(false);
        gbc.gridx = 1; gbc.weightx = 1.0;
        fileInputPanel.add(filePathField, gbc);
        
        browseBtn = new GlassButton("Browse");
        browseBtn.setEnabled(false);
        gbc.gridx = 2; gbc.weightx = 0; gbc.insets = new Insets(0, 0, 0, 0);
        fileInputPanel.add(browseBtn, gbc);
        
        headerPanel.add(fileInputPanel, BorderLayout.SOUTH);
        
        editorContainer.add(headerPanel, BorderLayout.NORTH);
        editorContainer.add(codeEditor, BorderLayout.CENTER);
        
        useFilePathCheckbox.addActionListener(e -> {
            boolean useFile = useFilePathCheckbox.isSelected();
            filePathField.setEnabled(useFile);
            browseBtn.setEnabled(useFile);
            codeEditor.setEnabled(!useFile);
        });
        
        browseBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(new java.io.File(System.getProperty("user.dir")));
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("C++ Files", "cpp", "cxx", "cc"));
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                filePathField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });

        mainSplit.setLeftComponent(new SurfacePanel(editorContainer, null));

        // Right: I/O + Status
        GlassSplitPane rightSplit1 = new GlassSplitPane(JSplitPane.VERTICAL_SPLIT);
        rightSplit1.setResizeWeight(0.5);

        rightSplit1.setLeftComponent(createPanel("Input", inputArea));
        rightSplit1.setRightComponent(createPanel("Expected Output", expectedArea));

        GlassSplitPane rightSplit2 = new GlassSplitPane(JSplitPane.VERTICAL_SPLIT);
        rightSplit2.setResizeWeight(0.6);
        rightSplit2.setLeftComponent(rightSplit1);

        JPanel statusContainer = new JPanel(new BorderLayout());
        statusContainer.setOpaque(false);
        
        JPanel statusHeader = new JPanel(new BorderLayout());
        statusHeader.setOpaque(false);
        statusHeader.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        
        JLabel statusLabel = new JLabel("Execution Status");
        statusLabel.setFont(Theme.TITLE_FONT);
        statusLabel.setForeground(Theme.TEXT_PRIMARY);
        statusHeader.add(statusLabel, BorderLayout.WEST);
        
        statsLabel = new JLabel("");
        statsLabel.setFont(Theme.UI_FONT);
        statsLabel.setForeground(Theme.TEXT_DIM);
        statusHeader.add(statsLabel, BorderLayout.EAST);
        
        statusContainer.add(statusHeader, BorderLayout.NORTH);
        statusContainer.add(statusPane, BorderLayout.CENTER);

        rightSplit2.setRightComponent(new SurfacePanel(statusContainer, null));

        mainSplit.setRightComponent(rightSplit2);

        add(mainSplit, BorderLayout.CENTER);
    }

    private SurfacePanel createPanel(String title, PlainTextArea textArea) {
        JPanel container = new JPanel(new BorderLayout());
        container.setOpaque(false);
        JLabel label = new JLabel(title);
        label.setFont(Theme.TITLE_FONT);
        label.setForeground(Theme.TEXT_PRIMARY);
        label.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        container.add(label, BorderLayout.NORTH);
        container.add(textArea, BorderLayout.CENTER);
        return new SurfacePanel(container, null);
    }

    public String getCode() {
        return codeEditor.getText();
    }

    public void setCode(String text) {
        codeEditor.setText(text);
    }

    public String getInput() {
        return inputArea.getText();
    }

    public String getExpectedOutput() {
        return expectedArea.getText();
    }

    public StatusPane getStatusPane() {
        return statusPane;
    }

    public boolean isUsingFilePath() {
        return useFilePathCheckbox.isSelected();
    }
    
    public void setUsingFilePath(boolean useFile) {
        useFilePathCheckbox.setSelected(useFile);
        filePathField.setEnabled(useFile);
        browseBtn.setEnabled(useFile);
        codeEditor.setEnabled(!useFile);
    }
    
    public String getFilePath() {
        return filePathField.getText();
    }
    
    public void setFilePath(String path) {
        filePathField.setText(path);
    }
    
    public void setStats(long runMillis) {
        if (runMillis >= 0) {
            statsLabel.setText("Time: " + runMillis + " ms | Mem: N/A");
        } else {
            statsLabel.setText("");
        }
    }
}
