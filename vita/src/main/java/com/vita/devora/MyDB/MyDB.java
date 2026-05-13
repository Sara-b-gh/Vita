package com.vita.devora.MyDB;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDB {

    private static MyDB instance;
    private Connection conn;

    private final String URL = "jdbc:mysql://localhost:3306/vita2";
    private final String USER = "root";
    private final String PASSWORD = "";

    public MyDB() {
        try {
            conn = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("✅ Connexion établie !");
        } catch (SQLException e) {
            System.out.println("❌ Erreur connexion : " + e.getMessage());
        }
    }

    public static MyDB getInstance() {
        if (instance == null) {
            instance = new MyDB(); // 🔥 UNE SEULE FOIS
        }
        return instance;
    }

    public Connection getConnection() {
        try {
            // If connection was closed or is null, recreate it
            if (conn == null || conn.isClosed()) {
                conn = DriverManager.getConnection(URL, USER, PASSWORD);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return conn;
    }
}