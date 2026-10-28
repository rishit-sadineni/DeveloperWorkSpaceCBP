package com.developerworkspace;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class DSAPanel extends BasePanel implements MainFrame.Refreshable {
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"Topic", "Solved", "Total", "Progress"}, 0) {
        public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);

    public DSAPanel(MainFrame frame) {
        super(frame);
        JButton add = button("+ Add Problem");
        add.addActionListener(e -> showProblemForm(null));

        JButton manage = button("Manage Problems");
        manage.addActionListener(e -> manageSelectedTopic());

        JButton refresh = button("Refresh");
        refresh.addActionListener(e -> refresh());

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && e.getButton() == java.awt.event.MouseEvent.BUTTON1) {
                    manageSelectedTopic();
                }
            }
        });

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(refresh);
        actions.add(manage);
        actions.add(add);

        setLayout(new BorderLayout(10, 10));
        add(titleBar("DSA Tracker", actions), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        refresh();
    }

    private void manageSelectedTopic() {
        int row = table.getSelectedRow();
        if (row < 0) {
            error("Select a topic.");
            return;
        }
        TopicItem topic = findTopic(String.valueOf(model.getValueAt(row, 0)));
        if (topic == null) return;

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this),
                "Problems - " + topic.name(), Dialog.ModalityType.APPLICATION_MODAL);
        DefaultTableModel problems = new DefaultTableModel(
                new String[]{"ID", "Problem", "Difficulty", "Platform", "URL", "Solved"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable problemTable = new JTable(problems);
        JButton add = button("Add");
        JButton edit = button("Edit");
        JButton toggle = button("Toggle Solved");
        JButton delete = button("Delete");
        JButton close = button("Close");

        Runnable load = () -> loadProblems(topic.id(), problems);
        add.addActionListener(e -> {
            showProblemForm(null);
            load.run();
        });
        edit.addActionListener(e -> {
            int selected = problemTable.getSelectedRow();
            if (selected < 0) {
                error("Select a problem.");
                return;
            }
            showProblemForm((Integer) problems.getValueAt(selected, 0));
            load.run();
        });
        toggle.addActionListener(e -> {
            int selected = problemTable.getSelectedRow();
            if (selected < 0) {
                error("Select a problem.");
                return;
            }
            toggleProblem((Integer) problems.getValueAt(selected, 0));
            load.run();
        });
        delete.addActionListener(e -> {
            int selected = problemTable.getSelectedRow();
            if (selected < 0) {
                error("Select a problem.");
                return;
            }
            deleteProblem((Integer) problems.getValueAt(selected, 0));
            load.run();
        });
        close.addActionListener(e -> dialog.dispose());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(add);
        actions.add(edit);
        actions.add(toggle);
        actions.add(delete);
        actions.add(close);
        dialog.add(new JScrollPane(problemTable), BorderLayout.CENTER);
        dialog.add(actions, BorderLayout.SOUTH);
        dialog.setSize(900, 400);
        dialog.setLocationRelativeTo(this);
        load.run();
        dialog.setVisible(true);
    }

    private void showProblemForm(Integer problemId) {
        JComboBox<TopicItem> topic = new JComboBox<>();
        loadTopics(topic);
        JTextField title = new JTextField();
        JComboBox<String> difficulty = new JComboBox<>(new String[]{"Easy", "Medium", "Hard"});
        JTextField platform = new JTextField();
        JTextField url = new JTextField();
        JCheckBox solved = new JCheckBox("Solved");

        if (problemId != null && !loadProblem(problemId, topic, title, difficulty, platform, url, solved)) {
            return;
        }

        JPanel fields = new JPanel(new GridLayout(0, 2, 8, 8));
        fields.add(new JLabel("Problem name")); fields.add(title);
        fields.add(new JLabel("Topic")); fields.add(topic);
        fields.add(new JLabel("Difficulty")); fields.add(difficulty);
        fields.add(new JLabel("Platform")); fields.add(platform);
        fields.add(new JLabel("URL")); fields.add(url);
        fields.add(new JLabel("Status")); fields.add(solved);

        String heading = problemId == null ? "Add DSA Problem" : "Edit DSA Problem";
        if (JOptionPane.showConfirmDialog(this, fields, heading,
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        TopicItem selectedTopic = (TopicItem) topic.getSelectedItem();
        if (selectedTopic == null || title.getText().isBlank()) {
            error("Problem name and topic are required.");
            return;
        }

        String sql = problemId == null
                ? "INSERT INTO dsa_problems(topic_id,title,difficulty,platform,url,solved) VALUES(?,?,?,?,?,?)"
                : "UPDATE dsa_problems SET topic_id=?,title=?,difficulty=?,platform=?,url=?,solved=? " +
                  "WHERE problem_id=? AND topic_id IN " +
                  "(SELECT topic_id FROM dsa_topics WHERE user_id=?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            int parameter = 1;
            ps.setInt(parameter++, selectedTopic.id());
            ps.setString(parameter++, title.getText().trim());
            ps.setString(parameter++, String.valueOf(difficulty.getSelectedItem()));
            ps.setString(parameter++, platform.getText().trim());
            ps.setString(parameter++, url.getText().trim());
            ps.setBoolean(parameter++, solved.isSelected());
            if (problemId != null) {
                ps.setInt(parameter++, problemId);
                ps.setInt(parameter, frame.getUserId());
            }
            ps.executeUpdate();
            refresh();
        } catch (SQLException e) {
            error(e.getMessage());
        }
    }

    private boolean loadProblem(int problemId, JComboBox<TopicItem> topic, JTextField title,
                                JComboBox<String> difficulty, JTextField platform,
                                JTextField url, JCheckBox solved) {
        String sql = """
                SELECT dp.topic_id, dp.title, dp.difficulty, dp.platform, dp.url, dp.solved
                FROM dsa_problems dp
                JOIN dsa_topics dt ON dt.topic_id=dp.topic_id
                WHERE dp.problem_id=? AND dt.user_id=?
                """;
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, problemId);
            ps.setInt(2, frame.getUserId());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    error("The selected problem could not be found.");
                    refresh();
                    return false;
                }
                selectTopic(topic, rs.getInt("topic_id"));
                title.setText(rs.getString("title"));
                difficulty.setSelectedItem(rs.getString("difficulty"));
                platform.setText(rs.getString("platform"));
                url.setText(rs.getString("url"));
                solved.setSelected(rs.getBoolean("solved"));
                return true;
            }
        } catch (SQLException e) {
            error(e.getMessage());
            return false;
        }
    }

    private void loadProblems(int topicId, DefaultTableModel problems) {
        problems.setRowCount(0);
        String sql = """
                SELECT dp.problem_id, dp.title, dp.difficulty, dp.platform, dp.url, dp.solved
                FROM dsa_problems dp
                JOIN dsa_topics dt ON dt.topic_id=dp.topic_id
                WHERE dp.topic_id=? AND dt.user_id=?
                ORDER BY dp.problem_id
                """;
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, topicId);
            ps.setInt(2, frame.getUserId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    problems.addRow(new Object[]{rs.getInt("problem_id"), rs.getString("title"),
                            rs.getString("difficulty"), rs.getString("platform"),
                            rs.getString("url"), rs.getBoolean("solved")});
                }
            }
        } catch (SQLException e) {
            error(e.getMessage());
        }
    }

    private void toggleProblem(int problemId) {
        String sql = """
                UPDATE dsa_problems dp
                JOIN dsa_topics dt ON dp.topic_id=dt.topic_id
                SET dp.solved=NOT dp.solved
                WHERE dp.problem_id=? AND dt.user_id=?
                """;
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, problemId);
            ps.setInt(2, frame.getUserId());
            ps.executeUpdate();
            refresh();
        } catch (SQLException e) {
            error(e.getMessage());
        }
    }

    private void deleteProblem(int problemId) {
        int answer = JOptionPane.showConfirmDialog(this,
                "Delete the selected problem?", "Confirm Delete",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) return;

        String sql = """
                DELETE dp FROM dsa_problems dp
                JOIN dsa_topics dt ON dp.topic_id=dt.topic_id
                WHERE dp.problem_id=? AND dt.user_id=?
                """;
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, problemId);
            ps.setInt(2, frame.getUserId());
            ps.executeUpdate();
            refresh();
        } catch (SQLException e) {
            error(e.getMessage());
        }
    }

    private void loadTopics(JComboBox<TopicItem> combo) {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT topic_id,name FROM dsa_topics WHERE user_id=? ORDER BY name")) {
            ps.setInt(1, frame.getUserId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) combo.addItem(new TopicItem(rs.getInt(1), rs.getString(2)));
            }
        } catch (SQLException e) {
            error(e.getMessage());
        }
    }

    private TopicItem findTopic(String name) {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT topic_id,name FROM dsa_topics WHERE user_id=? AND name=?")) {
            ps.setInt(1, frame.getUserId());
            ps.setString(2, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new TopicItem(rs.getInt(1), rs.getString(2)) : null;
            }
        } catch (SQLException e) {
            error(e.getMessage());
            return null;
        }
    }

    private void selectTopic(JComboBox<TopicItem> combo, int topicId) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (combo.getItemAt(i).id() == topicId) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    @Override
    public void refresh() {
        model.setRowCount(0);
        String sql = """
                SELECT dt.name,
                       COALESCE(SUM(CASE WHEN dp.solved THEN 1 ELSE 0 END),0) AS solved,
                       COUNT(dp.problem_id) AS total
                FROM dsa_topics dt
                LEFT JOIN dsa_problems dp ON dt.topic_id=dp.topic_id
                WHERE dt.user_id=?
                GROUP BY dt.topic_id,dt.name
                ORDER BY dt.name
                """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, frame.getUserId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int solved = rs.getInt("solved");
                    int total = rs.getInt("total");
                    String progress = total == 0 ? "0%" :
                            String.format("%.0f%%", solved * 100.0 / total);
                    model.addRow(new Object[]{rs.getString("name"), solved, total, progress});
                }
            }
        } catch (SQLException e) {
            error(e.getMessage());
        }
    }

    private record TopicItem(int id, String name) {
        public String toString() { return name; }
    }
}
