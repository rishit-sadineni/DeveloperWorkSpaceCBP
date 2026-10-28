package com.developerworkspace;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class ProjectsPanel extends BasePanel implements MainFrame.Refreshable {
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"ID", "Name", "Description", "Technologies", "Status", "GitHub"}, 0) {
        public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);

    public ProjectsPanel(MainFrame frame) {
        super(frame);
        JButton add = button("+ New Project");
        add.addActionListener(e -> addProject());

        JButton delete = button("Delete Selected");
        delete.addActionListener(e -> deleteSelected());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(delete);
        actions.add(add);

        setLayout(new BorderLayout(10, 10));
        add(titleBar("Projects", actions), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        refresh();
    }

    private void addProject() {
        JTextField name = new JTextField();
        JTextField description = new JTextField();
        JTextField tech = new JTextField();
        JComboBox<String> status = new JComboBox<>(new String[]{"PLANNED", "IN PROGRESS", "COMPLETED"});
        JTextField github = new JTextField();

        JPanel p = new JPanel(new GridLayout(0, 2, 8, 8));
        p.add(new JLabel("Name")); p.add(name);
        p.add(new JLabel("Description")); p.add(description);
        p.add(new JLabel("Technologies")); p.add(tech);
        p.add(new JLabel("Status")); p.add(status);
        p.add(new JLabel("GitHub URL")); p.add(github);

        int result = JOptionPane.showConfirmDialog(this, p, "New Project",
                JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) return;

        if (name.getText().isBlank()) {
            error("Project name is required.");
            return;
        }

        String sql = "INSERT INTO projects(user_id,name,description,technologies,status,github_url) VALUES(?,?,?,?,?,?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, frame.getUserId());
            ps.setString(2, name.getText().trim());
            ps.setString(3, description.getText().trim());
            ps.setString(4, tech.getText().trim());
            ps.setString(5, (String) status.getSelectedItem());
            ps.setString(6, github.getText().trim());
            ps.executeUpdate();
            refresh();
        } catch (SQLException e) {
            error(e.getMessage());
        }
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            error("Select a project first.");
            return;
        }

        int id = (int) model.getValueAt(row, 0);
        int ok = JOptionPane.showConfirmDialog(this,
                "Delete project and its tasks?", "Confirm",
                JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "DELETE FROM projects WHERE project_id=? AND user_id=?")) {
            ps.setInt(1, id);
            ps.setInt(2, frame.getUserId());
            ps.executeUpdate();
            refresh();
        } catch (SQLException e) {
            error(e.getMessage());
        }
    }

    @Override
    public void refresh() {
        model.setRowCount(0);
        String sql = "SELECT project_id,name,description,technologies,status,github_url " +
                     "FROM projects WHERE user_id=? ORDER BY project_id DESC";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, frame.getUserId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    model.addRow(new Object[]{
                            rs.getInt(1), rs.getString(2), rs.getString(3),
                            rs.getString(4), rs.getString(5), rs.getString(6)
                    });
                }
            }
        } catch (SQLException e) {
            error(e.getMessage());
        }
    }
}
