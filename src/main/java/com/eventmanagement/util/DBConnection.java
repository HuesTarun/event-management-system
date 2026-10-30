package com.eventmanagement.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DBConnection {

    private static String url;
    private static String username;
    private static String password;

    static {
        try (InputStream input = DBConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
            Properties props = new Properties();
            if (input == null) {
                System.err.println("[DBConnection] db.properties not found in resources folder! Using default localhost settings.");
                url = "jdbc:mysql://localhost:3306/event_management?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
                username = "root";
                password = "root";
            } else {
                props.load(input);
                String driver = props.getProperty("db.driver", "com.mysql.cj.jdbc.Driver");
                Class.forName(driver);
                url = props.getProperty("db.url");
                username = props.getProperty("db.username");
                password = props.getProperty("db.password");
                System.out.println("[DBConnection] MySQL JDBC Driver loaded successfully.");
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[DBConnection] Error initializing database configuration: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        if (url == null || username == null) {
            throw new SQLException("Database credentials not configured. Please check db.properties.");
        }
        return DriverManager.getConnection(url, username, password);
    }
}

