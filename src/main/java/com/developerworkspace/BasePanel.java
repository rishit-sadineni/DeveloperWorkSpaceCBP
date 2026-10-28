package com.developerworkspace;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public abstract class BasePanel extends JPanel {
    protected final MainFrame frame;

    protected BasePanel(MainFrame frame) {
        this.frame = frame;
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        setBackground(new Color(245, 247, 250));
    }

    protected JButton button(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        return b;
    }

    protected void error(String message) {
        JOptionPane.showMessageDialog(this, message, "Error",
                JOptionPane.ERROR_MESSAGE);
    }

    protected void info(String message) {
        JOptionPane.showMessageDialog(this, message, "Developer Workspace",
                JOptionPane.INFORMATION_MESSAGE);
    }

    protected JPanel titleBar(String title, JComponent action) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        JLabel label = new JLabel(title);
        label.setFont(new Font("SansSerif", Font.BOLD, 24));
        p.add(label, BorderLayout.WEST);
        if (action != null) p.add(action, BorderLayout.EAST);
        return p;
    }
}
