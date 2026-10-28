package com.developerworkspace;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class StatisticsPanel extends BasePanel implements MainFrame.Refreshable {
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"Metric", "Value"}, 0) {
        public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);

    public StatisticsPanel(MainFrame frame) {
        super(frame);
        JButton refresh = button("Refresh");
        refresh.addActionListener(e -> refresh());

        setLayout(new BorderLayout(10, 10));
        add(titleBar("Coding Statistics", refresh), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        refresh();
    }

    @Override
    public void refresh() {
        model.setRowCount(0);

        String[] names = {
                "Projects",
                "Completed Tasks",
                "Pending Tasks",
                "Snippets",
                "Bookmarks",
                "DSA Problems Solved",
                "Total Coding Minutes"
        };

        String[] sqls = {
                "SELECT COUNT(*) FROM projects WHERE user_id=?",
                "SELECT COUNT(*) FROM tasks t JOIN projects p ON t.project_id=p.project_id WHERE p.user_id=? AND t.status='COMPLETED'",
                "SELECT COUNT(*) FROM tasks t JOIN projects p ON t.project_id=p.project_id WHERE p.user_id=? AND t.status<>'COMPLETED'",
                "SELECT COUNT(*) FROM snippets WHERE user_id=?",
                "SELECT COUNT(*) FROM bookmarks WHERE user_id=?",
                "SELECT COUNT(*) FROM dsa_problems dp JOIN dsa_topics dt ON dp.topic_id=dt.topic_id WHERE dt.user_id=? AND dp.solved=TRUE",
                "SELECT COALESCE(SUM(duration_seconds),0) DIV 60 FROM coding_sessions WHERE user_id=?"
        };

        try (Connection con = DBConnection.getConnection()) {
            for (int i = 0; i < sqls.length; i++) {
                try (PreparedStatement ps = con.prepareStatement(sqls[i])) {
                    ps.setInt(1, frame.getUserId());
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        model.addRow(new Object[]{names[i], rs.getLong(1)});
                    }
                }
            }
        } catch (SQLException e) {
            error(e.getMessage());
        }
    }
}
