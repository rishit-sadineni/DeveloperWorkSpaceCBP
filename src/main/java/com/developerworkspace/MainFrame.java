package com.developerworkspace;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class MainFrame extends JFrame {
    private final int userId;
    private final String username;
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Map<String, JPanel> panels = new HashMap<>();

    public MainFrame(int userId, String username) {
        this.userId = userId;
        this.username = username;

        setTitle("Developer Workspace");
        setSize(1200, 760);
        setMinimumSize(new Dimension(950, 650));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        build();
    }

    public int getUserId() {
        return userId;
    }

    public void showPanel(String name) {
        cards.show(content, name);
        JPanel panel = panels.get(name);
        if (panel instanceof Refreshable refreshable) {
            refreshable.refresh();
        }
    }

    private void build() {
        setLayout(new BorderLayout());

        JPanel sidebar = new JPanel();
        sidebar.setPreferredSize(new Dimension(190, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));

        JLabel brand = new JLabel("<html><b>DEVELOPER<br>WORKSPACE</b></html>");
        brand.setFont(new Font("SansSerif", Font.BOLD, 18));
        sidebar.add(brand);
        sidebar.add(Box.createVerticalStrut(20));

        addNav(sidebar, "Dashboard", "dashboard");
        addNav(sidebar, "Projects", "projects");
        addNav(sidebar, "Tasks / Issues", "tasks");
        addNav(sidebar, "Code Snippets", "snippets");
        addNav(sidebar, "DSA Tracker", "dsa");
        addNav(sidebar, "Bookmarks", "bookmarks");
        addNav(sidebar, "Coding Timer", "timer");
        addNav(sidebar, "Statistics", "statistics");

        sidebar.add(Box.createVerticalGlue());

        JButton logout = new JButton("Logout");
        logout.addActionListener(e -> {
            dispose();
            new LoginFrame().setVisible(true);
        });
        sidebar.add(logout);

        content.add(panel("dashboard", new DashboardPanel(this)), "dashboard");
content.add(panel("projects", new ProjectsPanel(this)), "projects");
content.add(panel("tasks", new TasksPanel(this)), "tasks");
content.add(panel("snippets", new SnippetsPanel(this)), "snippets");
content.add(panel("dsa", new DSAPanel(this)), "dsa");
content.add(panel("bookmarks", new BookmarksPanel(this)), "bookmarks");
content.add(panel("timer", new TimerPanel(this)), "timer");
content.add(panel("statistics", new StatisticsPanel(this)), "statistics");

        add(sidebar, BorderLayout.WEST);
        add(content, BorderLayout.CENTER);

        showPanel("dashboard");
    }

    private JPanel panel(String key, JPanel panel) {
        panels.put(key, panel);
        return panel;
    }

    private void addNav(JPanel sidebar, String text, String key) {
        JButton b = new JButton(text);
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        b.addActionListener(e -> showPanel(key));
        sidebar.add(b);
        sidebar.add(Box.createVerticalStrut(5));
    }

    public interface Refreshable {
        void refresh();
    }
}
