package utils;

import java.sql.Connection;
import java.sql.DriverManager;

public class MyBD {

    private static MyBD instance;
    private Connection cnx;

    private final String URL = "jdbc:mysql://localhost:3306/evenn";
    private final String USER = "root";
    private final String PASSWORD = "";

    private MyBD() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            cnx = DriverManager.getConnection(URL, USER, PASSWORD);

            System.out.println("✔ DB CONNECTED OK");

        } catch (Exception e) {
            System.out.println(" DB CONNECTION FAILED");
            e.printStackTrace();

            cnx = null; // IMPORTANT
        }
    }

    public static MyBD getInstance() {
        if (instance == null) instance = new MyBD();
        return instance;
    }

    public Connection getCnx() {
        return cnx;
    }
}