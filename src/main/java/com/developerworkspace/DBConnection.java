package com.developerworkspace;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DBConnection {
    private DBConnection() {}

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                DBConfig.databaseUrl(),
                DBConfig.USER,
                DBConfig.PASSWORD
        );
    }

    public static Connection getServerConnection() throws SQLException {
        return DriverManager.getConnection(
                DBConfig.serverUrl(),
                DBConfig.USER,
                DBConfig.PASSWORD
        );
    }
}
