package com.developerworkspace;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.time.LocalDateTime;

public class TimerPanel extends BasePanel implements MainFrame.Refreshable {
    private final JLabel display = new JLabel("00:00:00", SwingConstants.CENTER);
    private final JComboBox<ProjectItem> project = new JComboBox<>();
    private Timer timer;
    private LocalDateTime start;
    private int seconds;

    public TimerPanel(MainFrame frame) {
        super(frame);

        display.setFont(new Font("Monospaced", Font.BOLD, 58));

        JButton startBtn = button("START");
        JButton stopBtn = button("STOP");
        startBtn.addActionListener(e -> startTimer());
        stopBtn.addActionListener(e -> stopTimer());

        JPanel controls = new JPanel(new GridLayout(2, 2, 10, 10));
        controls.add(new JLabel("Project"));
        controls.add(project);
        controls.add(startBtn);
        controls.add(stopBtn);

        setLayout(new BorderLayout(20, 20));
        add(titleBar("Coding Timer", null), BorderLayout.NORTH);
        add(display, BorderLayout.CENTER);
        add(controls, BorderLayout.SOUTH);

        refresh();
    }

    private void startTimer() {
        if (timer != null && timer.isRunning()) return;

        start = LocalDateTime.now();
        seconds = 0;

        timer = new Timer(1000, e -> {
            seconds++;
            display.setText(format(seconds));
        });
        timer.start();
    }

    private void stopTimer() {
        if (timer == null || !timer.isRunning()) return;

        timer.stop();
        LocalDateTime end = LocalDateTime.now();
        ProjectItem item = (ProjectItem) project.getSelectedItem();

        try (Connection con = DBConnection.getConnection()) {
            String sql = "INSERT INTO coding_sessions(user_id,project_id,start_time,end_time,duration_seconds) VALUES(?,?,?,?,?)";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, frame.getUserId());
                if (item == null) ps.setNull(2, Types.INTEGER);
                else ps.setInt(2, item.id());
                ps.setTimestamp(3, Timestamp.valueOf(start));
                ps.setTimestamp(4, Timestamp.valueOf(end));
                ps.setInt(5, seconds);
                ps.executeUpdate();
            }
            info("Coding session saved: " + format(seconds));
        } catch (SQLException e) {
            error(e.getMessage());
        }
    }

    private String format(int s) {
        return String.format("%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60);
    }

    @Override
    public void refresh() {
        project.removeAllItems();
        String sql = "SELECT project_id,name FROM projects WHERE user_id=? ORDER BY name";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, frame.getUserId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) project.addItem(new ProjectItem(rs.getInt(1), rs.getString(2)));
            }
        } catch (SQLException e) {
            error(e.getMessage());
        }
    }

    private record ProjectItem(int id, String name) {
        public String toString() { return name; }
    }
}
