package MyDB;


import java.sql.*;

public class TestDBConnection {

    public static void main(String[] args) {

        String url = "jdbc:mysql://localhost:3306/vita";
        String user = "root";
        String password = "";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT 1");

            if (rs.next()) {
                System.out.println("✅ DB is responding correctly!");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
