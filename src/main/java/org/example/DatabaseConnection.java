package org.example;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnection {
    private static final Properties props = new Properties();

    static {
        try (InputStream in = DatabaseConnection.class
                .getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (in != null) {
                props.load(in);
            } else {
                throw new RuntimeException("config.properties not found on classpath (src/main/resources).");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load config.properties: " + e.getMessage(), e);
        }
    }

    public static Connection getConnection() {
        String host = props.getProperty("DB_HOST");
        String port = props.getProperty("DB_PORT");
        String db   = props.getProperty("DB_NAME");
        String user = props.getProperty("DB_USER");
        String pass = props.getProperty("DB_PASSWORD");

        String url = "jdbc:mysql://" + host + ":" + port + "/" + db
                + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";

        try {
            return DriverManager.getConnection(url, user, pass);
        } catch (SQLException e) {
            throw new RuntimeException("DB connect failed: " + e.getMessage(), e);
        }
    }
}
