package com.developerworkspace;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class BookmarksPanel extends BasePanel implements MainFrame.Refreshable {
    private final JTextField search = new JTextField();
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"ID", "Title", "URL", "Category", "Tags", "Description"}, 0) {
        public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);

    public BookmarksPanel(MainFrame frame) {
        super(frame);

        JButton add = button("+ Add Bookmark");
        add.addActionListener(e -> addBookmark());

        JButton searchButton = button("Search");
        searchButton.addActionListener(e -> refresh());

        JPanel searchPanel = new JPanel(new BorderLayout(8, 8));
        searchPanel.add(search, BorderLayout.CENTER);
        searchPanel.add(searchButton, BorderLayout.EAST);

        JPanel north = new JPanel(new BorderLayout(8, 8));
        north.add(titleBar("Bookmarks", add), BorderLayout.NORTH);
        north.add(searchPanel, BorderLayout.SOUTH);

        setLayout(new BorderLayout(10, 10));
        add(north, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        refresh();
    }

    private void addBookmark() {
        JTextField title = new JTextField();
        JTextField url = new JTextField();
        JTextField category = new JTextField();
        JTextField tags = new JTextField();
        JTextField desc = new JTextField();

        JPanel p = new JPanel(new GridLayout(0, 2, 8, 8));
        p.add(new JLabel("Title")); p.add(title);
        p.add(new JLabel("URL")); p.add(url);
        p.add(new JLabel("Category")); p.add(category);
        p.add(new JLabel("Tags")); p.add(tags);
        p.add(new JLabel("Description")); p.add(desc);

        if (JOptionPane.showConfirmDialog(this, p, "Add Bookmark",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;

        String sql = "INSERT INTO bookmarks(user_id,title,url,category,tags,description) VALUES(?,?,?,?,?,?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, frame.getUserId());
            ps.setString(2, title.getText().trim());
            ps.setString(3, url.getText().trim());
            ps.setString(4, category.getText().trim());
            ps.setString(5, tags.getText().trim());
            ps.setString(6, desc.getText().trim());
            ps.executeUpdate();
            refresh();
        } catch (SQLException e) {
            error(e.getMessage());
        }
    }

    @Override
    public void refresh() {
        model.setRowCount(0);
        String q = "%" + search.getText().trim() + "%";
        String sql = """
                SELECT bookmark_id,title,url,category,tags,description
                FROM bookmarks
                WHERE user_id=?
                  AND (title LIKE ? OR category LIKE ? OR tags LIKE ? OR description LIKE ?)
                ORDER BY bookmark_id DESC
                """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, frame.getUserId());
            ps.setString(2, q); ps.setString(3, q);
            ps.setString(4, q); ps.setString(5, q);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    model.addRow(new Object[]{rs.getInt(1), rs.getString(2),
                            rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6)});
                }
            }
        } catch (SQLException e) {
            error(e.getMessage());
        }
    }
}
