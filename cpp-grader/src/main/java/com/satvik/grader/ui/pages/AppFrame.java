package com.satvik.grader.ui.pages;

import com.satvik.grader.core.FlagProfile;
import com.satvik.grader.core.Settings;
import com.satvik.grader.core.SingleTestRunner;
import com.satvik.grader.core.SourceSpec;
import com.satvik.grader.core.StressTester;
import com.satvik.grader.core.Tone;
import com.satvik.grader.ui.components.GlassBackground;
import com.satvik.grader.ui.components.GlassButton;
import com.satvik.grader.ui.components.GlassDropdown;
import com.satvik.grader.ui.components.GlassTextField;
import com.satvik.grader.ui.components.SegmentedControl;
import com.satvik.grader.ui.theme.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public class AppFrame extends JFrame {
    private final EditorPage editorPage;
    private final DiffPage diffPage;
    private final StressPage stressPage;

    private final JPanel cards;
    private final CardLayout cardLayout;
    
    private final Settings settings;
    
    private FlagProfile currentProfile = FlagProfile.DEFAULT;
    private String customFlags = "";

    private SingleTestRunner runner;
    private StressTester stressTester;

    private String lastDesiredOutput = "";
    private String lastGeneratedOutput = "";
    
    private GlassButton runButton;
    private GlassTextField customFlagsField;

    public AppFrame() {
        super("Satvik's C++ Code Grader");
        settings = Settings.load();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1300, 850);
        setLocationRelativeTo(null);

        GlassBackground background = new GlassBackground(new BorderLayout());
        setContentPane(background);

        cardLayout = new CardLayout();
        cards = new JPanel(cardLayout);
        cards.setOpaque(false);

        editorPage = new EditorPage();
        diffPage = new DiffPage();
        stressPage = new StressPage();

        cards.add(editorPage, "EDITOR");
        cards.add(diffPage, "DIFF");
        cards.add(stressPage, "STRESS");
        
        loadSettings();

        JPanel headerPanel = createHeader();
        background.add(headerPanel, BorderLayout.NORTH);
        background.add(cards, BorderLayout.CENTER);
        
        setupWiring();
        
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveSettings();
            }
        });
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        leftPanel.setOpaque(false);

        runButton = new GlassButton("Compile & Run");
        runButton.setBackground(Theme.ACCENT);
        runButton.setForeground(Theme.TEXT_PRIMARY);
        leftPanel.add(runButton);

        JLabel flagLabel = new JLabel("Flags:");
        flagLabel.setFont(Theme.UI_FONT);
        flagLabel.setForeground(Theme.TEXT_SECONDARY);
        leftPanel.add(flagLabel);

        GlassDropdown<FlagProfile> flagDropdown = new GlassDropdown<>(
            Arrays.asList(FlagProfile.values()), 
            currentProfile, 
            FlagProfile::label, 
            FlagProfile::description
        );
        leftPanel.add(flagDropdown);

        customFlagsField = new GlassTextField();
        customFlagsField.setPreferredSize(new Dimension(200, 32));
        customFlagsField.setText(customFlags);
        customFlagsField.setVisible(currentProfile == FlagProfile.CUSTOM);
        leftPanel.add(customFlagsField);

        flagDropdown.setOnChange(profile -> {
            currentProfile = profile;
            customFlagsField.setVisible(currentProfile == FlagProfile.CUSTOM);
            leftPanel.revalidate();
        });

        customFlagsField.addActionListener(e -> customFlags = customFlagsField.getText());

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightPanel.setOpaque(false);

        SegmentedControl navControl = new SegmentedControl("Editor", "Compare Output", "Stress Test");
        navControl.setOnSelect(index -> {
            switch (index) {
                case 0 -> cardLayout.show(cards, "EDITOR");
                case 1 -> {
                    cardLayout.show(cards, "DIFF");
                    diffPage.updateDiff(lastDesiredOutput, lastGeneratedOutput);
                }
                case 2 -> cardLayout.show(cards, "STRESS");
            }
        });
        rightPanel.add(navControl);

        header.add(leftPanel, BorderLayout.WEST);
        header.add(rightPanel, BorderLayout.EAST);

        return header;
    }

    private java.util.List<String> getActiveFlags() {
        String flagsStr = currentProfile == FlagProfile.CUSTOM ? customFlagsField.getText() : currentProfile.defaultFlags();
        return Arrays.stream(flagsStr.split("\\s+")).filter(s -> !s.isBlank()).toList();
    }

    private void setupWiring() {
        runButton.addActionListener(e -> {
            if (customFlagsField.isVisible()) {
                customFlags = customFlagsField.getText();
            }

            runButton.setEnabled(false);
            runButton.setText("Running...");
            editorPage.getStatusPane().clear();
            editorPage.setStats(-1);

            String code = editorPage.getCode();
            String input = editorPage.getInput();
            lastDesiredOutput = editorPage.getExpectedOutput();
            
            runner = new SingleTestRunner();
            
            SourceSpec spec = editorPage.isUsingFilePath() 
                ? SourceSpec.ofFile(Paths.get(editorPage.getFilePath())) 
                : SourceSpec.ofCode(code);
            
            SingleTestRunner.Request req = new SingleTestRunner.Request(
                spec,
                "g++",
                getActiveFlags(),
                currentProfile.label(),
                input,
                lastDesiredOutput,
                5000L,
                10000L
            );
            
            CompletableFuture.supplyAsync(() -> runner.run(req, (text, tone) -> {
                SwingUtilities.invokeLater(() -> editorPage.getStatusPane().append(text + "\n", tone));
            })).thenAccept(result -> SwingUtilities.invokeLater(() -> {
                lastGeneratedOutput = result.output();
                editorPage.setStats(result.runMillis());
                runButton.setEnabled(true);
                runButton.setText("Compile & Run");
            }));
        });

        stressPage.getStartButton().addActionListener(e -> {
            String genPath = stressPage.getGenPathField().getText();
            String brutePath = stressPage.getBrutePathField().getText();
            int iterations = stressPage.getIterationsStepper().getValue();
            String mainCode = editorPage.getCode();

            stressPage.getInputArea().setText("");
            stressPage.getExpectedArea().setText("");
            stressPage.getActualArea().setText("");

            stressPage.getStartButton().setEnabled(false);
            stressPage.getStopButton().setEnabled(true);
            stressPage.getStatusLabel().setText("Compiling...");
            stressPage.getStatusLabel().setForeground(Theme.INFO);

            SourceSpec mainSpec = editorPage.isUsingFilePath() 
                ? SourceSpec.ofFile(Paths.get(editorPage.getFilePath())) 
                : SourceSpec.ofCode(mainCode);

            StressTester.Config config = new StressTester.Config(
                Paths.get(genPath),
                Paths.get(brutePath),
                mainSpec,
                "g++",
                getActiveFlags(),
                iterations,
                5000L
            );

            stressTester = new StressTester(config, new StressTester.Listener() {
                @Override
                public void onStatus(String text, Tone tone) {
                    SwingUtilities.invokeLater(() -> {
                        stressPage.getStatusLabel().setText(text);
                        stressPage.getStatusLabel().setForeground(getColor(tone));
                    });
                }

                @Override
                public void onProgress(int done, int total) {
                    // Update progress if needed
                }

                @Override
                public void onFinished(StressTester.Outcome outcome) {
                    SwingUtilities.invokeLater(() -> {
                        stressPage.getStatusLabel().setText(outcome.message());
                        if (outcome.kind() == StressTester.Kind.PASSED) {
                            stressPage.getStatusLabel().setForeground(Theme.SUCCESS);
                        } else if (outcome.kind() == StressTester.Kind.STOPPED) {
                            stressPage.getStatusLabel().setForeground(Theme.WARNING);
                        } else {
                            stressPage.getStatusLabel().setForeground(Theme.ERROR);
                        }
                        
                        stressPage.getInputArea().setText(outcome.input());
                        stressPage.getExpectedArea().setText(outcome.expected());
                        stressPage.getActualArea().setText(outcome.actual());
                        
                        stressPage.getStartButton().setEnabled(true);
                        stressPage.getStopButton().setEnabled(false);
                    });
                }
            });
            
            new Thread(stressTester).start();
        });

        stressPage.getStopButton().addActionListener(e -> {
            if (stressTester != null) {
                stressTester.stop();
                stressPage.getStatusLabel().setText("Stopping...");
                stressPage.getStatusLabel().setForeground(Theme.WARNING);
            }
        });
    }

    private Color getColor(Tone tone) {
        return switch (tone) {
            case INFO -> Theme.INFO;
            case SUCCESS -> Theme.SUCCESS;
            case FAILURE, ERROR -> Theme.ERROR;
            case WARNING -> Theme.WARNING;
            default -> Theme.TEXT_PRIMARY;
        };
    }

    private void loadSettings() {
        stressPage.getGenPathField().setText(settings.get("stress.gen_path", ""));
        stressPage.getBrutePathField().setText(settings.get("stress.brute_path", ""));
        stressPage.getIterationsStepper().setValue(Integer.parseInt(settings.get("stress.iterations", "100")));
        
        currentProfile = FlagProfile.parse(settings.get("flags.profile", "Default"));
        customFlags = settings.get("flags.custom", FlagProfile.CUSTOM.defaultFlags());
        
        editorPage.setUsingFilePath(settings.getBool("editor.use_file", false));
        editorPage.setFilePath(settings.get("editor.file_path", ""));
        
        String savedCode = settings.readText("saved_code.cpp");
        if (savedCode != null && !savedCode.isEmpty()) {
            editorPage.setCode(savedCode);
        }
    }

    private void saveSettings() {
        settings.put("stress.gen_path", stressPage.getGenPathField().getText());
        settings.put("stress.brute_path", stressPage.getBrutePathField().getText());
        settings.put("stress.iterations", String.valueOf(stressPage.getIterationsStepper().getValue()));
        
        settings.put("flags.profile", currentProfile.label());
        settings.put("flags.custom", customFlagsField.isVisible() ? customFlagsField.getText() : customFlags);
        
        settings.put("editor.use_file", String.valueOf(editorPage.isUsingFilePath()));
        settings.put("editor.file_path", editorPage.getFilePath());
        
        settings.save();
        
        settings.writeText("saved_code.cpp", editorPage.getCode());
    }
    

}
