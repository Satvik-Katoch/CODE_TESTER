package com.satvik.grader.ui.pages;

import com.satvik.grader.ui.components.GlassButton;
import com.satvik.grader.ui.components.GlassSplitPane;
import com.satvik.grader.ui.components.GlassTextField;
import com.satvik.grader.ui.components.NumberStepper;
import com.satvik.grader.ui.components.SurfacePanel;
import com.satvik.grader.ui.editor.PlainTextArea;
import com.satvik.grader.ui.theme.Theme;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class StressPage extends JPanel {
    private final GlassTextField genPathField;
    private final GlassTextField brutePathField;
    private final NumberStepper iterationsStepper;
    
    private final GlassButton startButton;
    private final GlassButton stopButton;
    private final JLabel statusLabel;

    private final PlainTextArea inputArea;
    private final PlainTextArea expectedArea;
    private final PlainTextArea actualArea;

    public StressPage() {
        setOpaque(false);
        setLayout(new BorderLayout());

        // --- Top Configuration Panel ---
        JPanel configContainer = new JPanel(new GridBagLayout());
        configContainer.setOpaque(false);
        configContainer.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Generator Config
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        JLabel genLabel = new JLabel("Generator Script (Java/C++):");
        genLabel.setFont(Theme.UI_FONT);
        genLabel.setForeground(Theme.TEXT_PRIMARY);
        configContainer.add(genLabel, gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        genPathField = new GlassTextField();
        configContainer.add(genPathField, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        GlassButton genBrowseBtn = new GlassButton("Browse");
        genBrowseBtn.addActionListener(e -> browseFile(genPathField, "Java or C++ Files", "*.java", "*.cpp", "*.cxx", "*.cc"));
        configContainer.add(genBrowseBtn, gbc);

        // Brute Force Config
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        JLabel bruteLabel = new JLabel("Brute Force (C++):");
        bruteLabel.setFont(Theme.UI_FONT);
        bruteLabel.setForeground(Theme.TEXT_PRIMARY);
        configContainer.add(bruteLabel, gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        brutePathField = new GlassTextField();
        configContainer.add(brutePathField, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        GlassButton bruteBrowseBtn = new GlassButton("Browse");
        bruteBrowseBtn.addActionListener(e -> browseFile(brutePathField, "C++ Files", "*.cpp"));
        configContainer.add(bruteBrowseBtn, gbc);

        // Controls
        JPanel ctrlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        ctrlPanel.setOpaque(false);
        
        JLabel iterLabel = new JLabel("Iterations:");
        iterLabel.setFont(Theme.UI_FONT);
        iterLabel.setForeground(Theme.TEXT_PRIMARY);
        ctrlPanel.add(iterLabel);

        iterationsStepper = new NumberStepper(100, 1, 10000, 10);
        ctrlPanel.add(iterationsStepper);

        startButton = new GlassButton("Start Stress Test");
        startButton.setBackground(Theme.ACCENT);
        startButton.setForeground(Theme.TEXT_PRIMARY);
        ctrlPanel.add(startButton);

        stopButton = new GlassButton("Stop");
        stopButton.setBackground(Theme.ERROR);
        stopButton.setForeground(Theme.TEXT_PRIMARY);
        stopButton.setEnabled(false);
        ctrlPanel.add(stopButton);

        statusLabel = new JLabel("Ready");
        statusLabel.setFont(Theme.TITLE_FONT);
        statusLabel.setForeground(Theme.WARNING);
        ctrlPanel.add(statusLabel);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 3; gbc.insets = new Insets(15, 5, 0, 5);
        configContainer.add(ctrlPanel, gbc);

        add(new SurfacePanel(configContainer, null), BorderLayout.NORTH);

        // --- Bottom Results Panel ---
        inputArea = new PlainTextArea();
        expectedArea = new PlainTextArea();
        actualArea = new PlainTextArea();

        GlassSplitPane split1 = new GlassSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split1.setResizeWeight(0.5);
        split1.setLeftComponent(createPanel("1. Input (Test Case)", inputArea));
        split1.setRightComponent(createPanel("2. Expected Output (Brute)", expectedArea));

        GlassSplitPane split2 = new GlassSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split2.setResizeWeight(0.66);
        split2.setLeftComponent(split1);
        split2.setRightComponent(createPanel("3. Actual Output (Optimized)", actualArea));

        add(split2, BorderLayout.CENTER);
    }

    private SurfacePanel createPanel(String title, PlainTextArea textArea) {
        JPanel container = new JPanel(new BorderLayout());
        container.setOpaque(false);
        JLabel label = new JLabel(title);
        label.setFont(Theme.TITLE_FONT);
        label.setForeground(Theme.TEXT_PRIMARY);
        label.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        container.add(label, BorderLayout.NORTH);
        container.add(com.satvik.grader.ui.components.UiKit.scroll(textArea), BorderLayout.CENTER);
        return new SurfacePanel(container, null);
    }

    private void browseFile(GlassTextField field, String desc, String... exts) {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
        
        String[] cleanExts = new String[exts.length];
        for (int i = 0; i < exts.length; i++) {
            cleanExts[i] = exts[i].replace("*.", "");
        }
        
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(desc, cleanExts));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            field.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    public GlassTextField getGenPathField() { return genPathField; }
    public GlassTextField getBrutePathField() { return brutePathField; }
    public NumberStepper getIterationsStepper() { return iterationsStepper; }
    public GlassButton getStartButton() { return startButton; }
    public GlassButton getStopButton() { return stopButton; }
    public JLabel getStatusLabel() { return statusLabel; }
    public PlainTextArea getInputArea() { return inputArea; }
    public PlainTextArea getExpectedArea() { return expectedArea; }
    public PlainTextArea getActualArea() { return actualArea; }
}
