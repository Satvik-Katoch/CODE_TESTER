package com.satvik.grader;

import com.satvik.grader.ui.pages.AppFrame;

import javax.swing.*;

public class App {
    public static void main(String[] args) {
        System.setProperty("sun.java2d.opengl", "true"); // Hardware acceleration for drawing
        SwingUtilities.invokeLater(() -> {
            AppFrame frame = new AppFrame();
            frame.setVisible(true);
        });
    }
}
