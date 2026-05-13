package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDatabase {

    private final String URL = "jdbc:mysql://localhost:3306/vita";
    private final String USER = "root";
    private final String PASSWORD = "";

    private Connection cnx;
    private static MyDatabase instance;

    private MyDatabase() {
        try {
            cnx = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("✅ Connected to database");
        } catch (SQLException e) {
            System.out.println("❌ Database connection error: " + e.getMessage());
        }
    }

    public static MyDatabase getInstance() {
        if (instance == null) {
            instance = new MyDatabase();
        }
        return instance;
    }

    // old method
    public Connection getCnx() {
        return cnx;
    }

    // ✅ new method for controller compatibility
    public Connection getConnection() {
        return cnx;
    }
}