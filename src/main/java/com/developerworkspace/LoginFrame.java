package com.developerworkspace;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class LoginFrame extends JFrame {
    private final JTextField username = new JTextField("admin");
    private final JPasswordField password = new JPasswordField("admin123");

    public LoginFrame() {
        setTitle("Developer Workspace — Login");
        setSize(420, 300);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        build();
    }

    private void build() {
        JPanel root = new JPanel(new BorderLayout(15, 15));
        root.setBorder(BorderFactory.createEmptyBorder(25, 35, 25, 35));

        JLabel title = new JLabel("DEVELOPER WORKSPACE", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        root.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(4, 1, 8, 8));
        form.add(new JLabel("Username"));
        form.add(username);
        form.add(new JLabel("Password"));
        form.add(password);

        JButton login = new JButton("LOGIN");
        login.addActionListener(e -> login());

        JPanel center = new JPanel(new BorderLayout(10, 10));
        center.add(form, BorderLayout.CENTER);
        center.add(login, BorderLayout.SOUTH);

        root.add(center, BorderLayout.CENTER);

        JLabel hint = new JLabel("First run: admin / admin123", SwingConstants.CENTER);
        root.add(hint, BorderLayout.SOUTH);

        setContentPane(root);
        getRootPane().setDefaultButton(login);
    }

    private void login() {
        String user = username.getText().trim();
        String pass = new String(password.getPassword());

        String sql = "SELECT user_id, username FROM users WHERE username=? AND password_hash=?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, user);
            ps.setString(2, PasswordUtil.sha256(pass));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    dispose();
                    new MainFrame(rs.getInt("user_id"), rs.getString("username")).setVisible(true);
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Invalid username or password.",
                            "Login Failed",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Login failed: " + ex.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
