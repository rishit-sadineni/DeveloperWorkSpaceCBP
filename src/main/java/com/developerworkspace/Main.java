package com.developerworkspace;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                DatabaseInitializer.initialize();
                new LoginFrame().setVisible(true);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(
                        null,
                        e.getMessage(),
                        "Startup Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });
    }
}
