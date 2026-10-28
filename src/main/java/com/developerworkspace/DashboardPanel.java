package com.developerworkspace;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class DashboardPanel extends BasePanel implements MainFrame.Refreshable {
    private final JLabel projects = new JLabel();
    private final JLabel tasks = new JLabel();
    private final JLabel snippets = new JLabel();
    private final JLabel dsa = new JLabel();
    private final JLabel bookmarks = new JLabel();
    private final JLabel time = new JLabel();

    public DashboardPanel(MainFrame frame) {
        super(frame);

        JButton refresh = button("Refresh");
        refresh.addActionListener(e -> refresh());

        setLayout(new BorderLayout(15, 15));
        add(titleBar("Welcome, " + frame.getTitle().replace("Developer Workspace", "").trim(), refresh),
                BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(2, 3, 15, 15));
        grid.add(card("Projects", projects));
        grid.add(card("Open Tasks", tasks));
        grid.add(card("Snippets", snippets));
        grid.add(card("DSA Solved", dsa));
        grid.add(card("Bookmarks", bookmarks));
        grid.add(card("Coding Time", time));

        add(grid, BorderLayout.CENTER);
        refresh();
    }

    private JPanel card(String name, JLabel value) {
        JPanel p = new JPanel(new BorderLayout(5, 5));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)));
        JLabel n = new JLabel(name);
        n.setFont(new Font("SansSerif", Font.BOLD, 16));
        value.setFont(new Font("SansSerif", Font.BOLD, 30));
        p.add(n, BorderLayout.NORTH);
        p.add(value, BorderLayout.CENTER);
        return p;
    }

    @Override
    public void refresh() {
        String[] sqls = {
                "SELECT COUNT(*) FROM projects WHERE user_id=?",
                "SELECT COUNT(*) FROM tasks t JOIN projects p ON t.project_id=p.project_id WHERE p.user_id=? AND t.status <> 'COMPLETED'",
                "SELECT COUNT(*) FROM snippets WHERE user_id=?",
                "SELECT COUNT(*) FROM dsa_problems dp JOIN dsa_topics dt ON dp.topic_id=dt.topic_id WHERE dt.user_id=? AND dp.solved=TRUE",
                "SELECT COUNT(*) FROM bookmarks WHERE user_id=?",
                "SELECT COALESCE(SUM(duration_seconds),0) FROM coding_sessions WHERE user_id=?"
        };

        JLabel[] labels = {projects, tasks, snippets, dsa, bookmarks, time};

        try (Connection con = DBConnection.getConnection()) {
            for (int i = 0; i < sqls.length; i++) {
                try (PreparedStatement ps = con.prepareStatement(sqls[i])) {
                    ps.setInt(1, frame.getUserId());
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        long v = rs.getLong(1);
                        if (i == 5) {
                            long h = v / 3600;
                            long m = (v % 3600) / 60;
                            labels[i].setText(h + "h " + m + "m");
                        } else {
                            labels[i].setText(String.valueOf(v));
                        }
                    }
                }
            }
        } catch (SQLException e) {
            error(e.getMessage());
        }
    }
}
