package com.developerworkspace;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class TasksPanel extends BasePanel implements MainFrame.Refreshable {
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"ID", "Task", "Project", "Priority", "Status", "Due Date"}, 0) {
        public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);

    public TasksPanel(MainFrame frame) {
        super(frame);
        JButton add = button("+ Add Task");
        add.addActionListener(e -> addTask());
        JButton complete = button("Mark Completed");
        complete.addActionListener(e -> complete());
        JButton delete = button("Delete");
        delete.addActionListener(e -> delete());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(delete); actions.add(complete); actions.add(add);

        setLayout(new BorderLayout(10, 10));
        add(titleBar("Tasks / Issues", actions), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        refresh();
    }

    private void addTask() {
        JComboBox<ProjectItem> project = new JComboBox<>();
        loadProjects(project);

        JTextField title = new JTextField();
        JComboBox<String> priority = new JComboBox<>(new String[]{"LOW", "MEDIUM", "HIGH"});
        JComboBox<String> status = new JComboBox<>(new String[]{"TODO", "IN PROGRESS", "COMPLETED"});
        JTextField due = new JTextField("2026-12-31");

        JPanel p = new JPanel(new GridLayout(0, 2, 8, 8));
        p.add(new JLabel("Project")); p.add(project);
        p.add(new JLabel("Task")); p.add(title);
        p.add(new JLabel("Priority")); p.add(priority);
        p.add(new JLabel("Status")); p.add(status);
        p.add(new JLabel("Due Date (YYYY-MM-DD)")); p.add(due);

        if (project.getItemCount() == 0) {
            error("Create a project first.");
            return;
        }

        if (JOptionPane.showConfirmDialog(this, p, "Add Task",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO tasks(project_id,title,priority,status,due_date) VALUES(?,?,?,?,?)")) {
            ps.setInt(1, ((ProjectItem) project.getSelectedItem()).id());
            ps.setString(2, title.getText().trim());
            ps.setString(3, (String) priority.getSelectedItem());
            ps.setString(4, (String) status.getSelectedItem());
            ps.setDate(5, Date.valueOf(due.getText().trim()));
            ps.executeUpdate();
            refresh();
        } catch (Exception e) {
            error("Invalid input or database error: " + e.getMessage());
        }
    }

    private void loadProjects(JComboBox<ProjectItem> combo) {
        String sql = "SELECT project_id,name FROM projects WHERE user_id=? ORDER BY name";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, frame.getUserId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) combo.addItem(new ProjectItem(rs.getInt(1), rs.getString(2)));
            }
        } catch (SQLException e) {
            error(e.getMessage());
        }
    }

    private void complete() {
        int row = table.getSelectedRow();
        if (row < 0) { error("Select a task."); return; }
        int id = (int) model.getValueAt(row, 0);
        updateStatus(id, "COMPLETED");
    }

    private void updateStatus(int id, String status) {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE tasks t JOIN projects p ON t.project_id=p.project_id " +
                     "SET t.status=? WHERE t.task_id=? AND p.user_id=?")) {
            ps.setString(1, status);
            ps.setInt(2, id);
            ps.setInt(3, frame.getUserId());
            ps.executeUpdate();
            refresh();
        } catch (SQLException e) {
            error(e.getMessage());
        }
    }

    private void delete() {
        int row = table.getSelectedRow();
        if (row < 0) { error("Select a task."); return; }
        int id = (int) model.getValueAt(row, 0);

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "DELETE t FROM tasks t JOIN projects p ON t.project_id=p.project_id " +
                     "WHERE t.task_id=? AND p.user_id=?")) {
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
        String sql = """
                SELECT t.task_id,t.title,p.name,t.priority,t.status,t.due_date
                FROM tasks t
                JOIN projects p ON t.project_id=p.project_id
                WHERE p.user_id=?
                ORDER BY t.due_date
                """;
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, frame.getUserId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    model.addRow(new Object[]{rs.getInt(1), rs.getString(2),
                            rs.getString(3), rs.getString(4), rs.getString(5), rs.getDate(6)});
                }
            }
        } catch (SQLException e) {
            error(e.getMessage());
        }
    }

    private record ProjectItem(int id, String name) {
        public String toString() { return name; }
    }
}
