package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyBD {
    private static Connection connection;
    private static MyBD instance;

    // Constructeur privé pour Singleton
    private MyBD() {}

    // Méthode getInstance() - Pattern Singleton correct
    public static MyBD getInstance() {
        if (instance == null) {
            synchronized (MyBD.class) {
                if (instance == null) {
                    instance = new MyBD();
                }
            }
        }
        return instance;
    }

    // Méthode getConnection() - Version statique
    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            String url = "jdbc:mysql://localhost:3306/evenn";
            String user = "root";
            String password = "";
            connection = DriverManager.getConnection(url, user, password);
        }
        return connection;
    }

    // Méthode getCnx() - Version non statique (instance)
    public Connection getCnx() {
        try {
            if (connection == null || connection.isClosed()) {
                String url = "jdbc:mysql://localhost:3306/evenn";
                String user = "root";
                String password = "";
                connection = DriverManager.getConnection(url, user, password);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return connection;
    }

    // Méthode pour fermer la connexion
    public static void closeConnection() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
            connection = null;
        }
    }

    // Méthode pour tester la connexion
    public static boolean testConnection() {
        try {
            Connection conn = getConnection();
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}