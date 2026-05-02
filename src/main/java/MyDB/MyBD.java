package MyDB;


import java.sql.Connection;
import java.sql.DriverManager;

public class MyBD {

    private static final String URL = "jdbc:mysql://localhost:3306/vita";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    public static Connection getConnection() throws Exception {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
//Une classe simple qui fournit une connexion MySQL à la base vita(localhost, root, sans mot de passe).
// Chaque fois qu'un CRUD en a besoin, il appelle MyBD.getConnection().