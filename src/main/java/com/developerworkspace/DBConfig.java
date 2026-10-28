package com.developerworkspace;

public final class DBConfig {
    private DBConfig() {}

    public static final String HOST =
            System.getenv().getOrDefault("DW_DB_HOST", "localhost");
    public static final String PORT =
            System.getenv().getOrDefault("DW_DB_PORT", "3306");
    public static final String USER = "root";
    public static final String PASSWORD = "";
    public static final String DATABASE = "developer_workspace";

    public static String serverUrl() {
        return "jdbc:mysql://" + HOST + ":" + PORT
                + "/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    }

    public static String databaseUrl() {
        return "jdbc:mysql://" + HOST + ":" + PORT + "/" + DATABASE
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    }
}
