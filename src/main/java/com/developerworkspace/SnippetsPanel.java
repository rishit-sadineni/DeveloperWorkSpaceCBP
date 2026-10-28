package com.developerworkspace;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.sql.*;

public class SnippetsPanel extends BasePanel implements MainFrame.Refreshable {
    private final JTextField search = new JTextField();
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"ID", "Title", "Language", "Tags", "Code"}, 0) {
        public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);

    public SnippetsPanel(MainFrame frame) {
        super(frame);

        JButton add = button("+ Add Snippet");
        add.addActionListener(e -> addSnippet());

        JButton searchBtn = button("Search");
        searchBtn.addActionListener(e -> refresh());

        JButton copy = button("Copy Code");
        copy.addActionListener(e -> copySelected());

        JButton edit = button("Edit");
        edit.addActionListener(e -> editSelected());

        JButton delete = button("Delete");
        delete.addActionListener(e -> deleteSelected());

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && e.getButton() == java.awt.event.MouseEvent.BUTTON1) {
                    editSelected();
                }
            }
        });

        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.add(search, BorderLayout.CENTER);
        top.add(searchBtn, BorderLayout.EAST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(copy); actions.add(edit); actions.add(delete); actions.add(add);

        JPanel north = new JPanel(new BorderLayout());
        north.add(titleBar("Code Snippets", actions), BorderLayout.NORTH);
        north.add(top, BorderLayout.SOUTH);

        setLayout(new BorderLayout(10, 10));
        add(north, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        refresh();
    }

    private void addSnippet() {
        saveSnippet(null);
    }

    private void editSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            error("Select a snippet.");
            return;
        }
        saveSnippet((Integer) model.getValueAt(row, 0));
    }

    private void saveSnippet(Integer snippetId) {
        JTextField title = new JTextField();
        JTextField language = new JTextField("Java");
        JTextField tags = new JTextField();
        JTextArea code = new JTextArea(10, 50);

        if (snippetId != null && !loadSnippet(snippetId, title, language, tags, code)) {
            return;
        }

        JPanel p = new JPanel(new BorderLayout(5, 5));
        JPanel fields = new JPanel(new GridLayout(3, 2, 5, 5));
        fields.add(new JLabel("Title")); fields.add(title);
        fields.add(new JLabel("Language")); fields.add(language);
        fields.add(new JLabel("Tags")); fields.add(tags);
        p.add(fields, BorderLayout.NORTH);
        p.add(new JScrollPane(code), BorderLayout.CENTER);

        String dialogTitle = snippetId == null ? "Add Snippet" : "Edit Snippet";
        if (JOptionPane.showConfirmDialog(this, p, dialogTitle,
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;

        if (title.getText().isBlank() || language.getText().isBlank() || code.getText().isBlank()) {
            error("Title and code are required.");
            return;
        }

        String sql = snippetId == null
                ? "INSERT INTO snippets(user_id,title,language,tags,code) VALUES(?,?,?,?,?)"
                : "UPDATE snippets SET title=?,language=?,tags=?,code=? WHERE snippet_id=? AND user_id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            int parameter = 1;
            if (snippetId == null) {
                ps.setInt(parameter++, frame.getUserId());
            }
            ps.setString(parameter++, title.getText().trim());
            ps.setString(parameter++, language.getText().trim());
            ps.setString(parameter++, tags.getText().trim());
            ps.setString(parameter++, code.getText());
            if (snippetId != null) {
                ps.setInt(parameter++, snippetId);
                ps.setInt(parameter, frame.getUserId());
            }
            ps.executeUpdate();
            refresh();
        } catch (SQLException e) {
            error(e.getMessage());
        }
    }

    private boolean loadSnippet(int snippetId, JTextField title, JTextField language,
                                JTextField tags, JTextArea code) {
        String sql = "SELECT title,language,tags,code FROM snippets WHERE snippet_id=? AND user_id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, snippetId);
            ps.setInt(2, frame.getUserId());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    error("The selected snippet could not be found.");
                    refresh();
                    return false;
                }
                title.setText(rs.getString("title"));
                language.setText(rs.getString("language"));
                tags.setText(rs.getString("tags"));
                code.setText(rs.getString("code"));
                return true;
            }
        } catch (SQLException e) {
            error(e.getMessage());
            return false;
        }
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            error("Select a snippet.");
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this,
                "Delete the selected snippet?", "Confirm Delete",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) return;

        String sql = "DELETE FROM snippets WHERE snippet_id=? AND user_id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, (Integer) model.getValueAt(row, 0));
            ps.setInt(2, frame.getUserId());
            ps.executeUpdate();
            refresh();
        } catch (SQLException e) {
            error(e.getMessage());
        }
    }

    private void copySelected() {
        int row = table.getSelectedRow();
        if (row < 0) { error("Select a snippet."); return; }
        String sql = "SELECT code FROM snippets WHERE snippet_id=? AND user_id=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, (Integer) model.getValueAt(row, 0));
            ps.setInt(2, frame.getUserId());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    error("The selected snippet could not be found.");
                    refresh();
                    return;
                }
                Toolkit.getDefaultToolkit().getSystemClipboard()
                        .setContents(new StringSelection(rs.getString("code")), null);
                info("Code copied.");
            }
        } catch (SQLException e) {
            error(e.getMessage());
        }
    }

    @Override
    public void refresh() {
        model.setRowCount(0);
        String q = search.getText().trim();
        String sql = """
                SELECT snippet_id,title,language,tags,code
                FROM snippets
                WHERE user_id=?
                  AND (title LIKE ? OR language LIKE ? OR tags LIKE ? OR code LIKE ?)
                ORDER BY snippet_id DESC
                """;
        String like = "%" + q + "%";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, frame.getUserId());
            ps.setString(2, like);
            ps.setString(3, like);
            ps.setString(4, like);
            ps.setString(5, like);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    model.addRow(new Object[]{rs.getInt(1), rs.getString(2),
                            rs.getString(3), rs.getString(4), rs.getString(5)});
                }
            }
        } catch (SQLException e) {
            error(e.getMessage());
        }
    }
}
