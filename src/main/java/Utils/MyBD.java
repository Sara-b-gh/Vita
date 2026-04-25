package Utils;

import java.sql.*;

public class MyBD {

    String url = "jdbc:mysql://localhost:3306/vita";  // ← changer ici
    String user = "root";
    String password = "";

    private Connection conn;
    private static MyBD instance;

    private MyBD() {
        try {
            conn = DriverManager.getConnection(url, user, password);
            System.out.println("Connection établie !!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static MyBD getInstance() {
        if (instance == null) {
            return instance = new MyBD();
        }
        return instance;
    }

    public Connection getConn() {
        return conn;
    }

    public void setConn(Connection conn) {
        this.conn = conn;
    }
}