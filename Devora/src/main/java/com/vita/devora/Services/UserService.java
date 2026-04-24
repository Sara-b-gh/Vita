package com.vita.devora.Services;

import com.vita.devora.Entities.User;
import com.vita.devora.Interface.UserCRUD;
import com.vita.devora.MyDB.MyBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserService implements UserCRUD<User> {

    private Connection conn;

    public User getUserById(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";

        try (Connection conn = MyBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                User u = new User();
                u.setId(rs.getInt("id"));
                u.setNom(rs.getString("Nom"));
                u.setPrenom(rs.getString("Prenom"));
                u.setEmail(rs.getString("Email"));
                u.setPassword(rs.getString("Password"));
                u.setNumtel(rs.getInt("NumeroTelephone"));
                u.setRole(User.Roles.valueOf(rs.getString("Role")));

                return u;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
    @Override
    public void ajouter(User user) {
        String req = "INSERT INTO users (Nom, Prenom, Email, Password, NumeroTelephone, Role) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = MyBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(req)) {

            ps.setString(1, user.getNom());
            ps.setString(2, user.getPrenom());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getPassword());
            ps.setInt(5, user.getNumtel());
            ps.setString(6, user.getRole().toString());

            ps.executeUpdate();

            System.out.println("✅ User inserted successfully!");

        } catch (Exception e) {
            System.out.println("❌ Error inserting user:");
            e.printStackTrace();
        }


    }

    @Override
    public void modifier(User user) throws SQLException {
        String sql = "UPDATE users SET Nom=?, Prenom=?, Email=?, Password=?, NumeroTelephone=? , Role=? WHERE id=?";

        try (Connection conn = MyBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user.getNom());
            ps.setString(2, user.getPrenom());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getPassword());
            ps.setInt(5, user.getNumtel());
            ps.setString(6, user.getRole().toString());
            ps.setInt(7, user.getId());

            ps.executeUpdate();
            System.out.println("✅ User updated!");

        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM user WHERE id=?";

        try (Connection conn = MyBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();

            System.out.println("🗑️ User deleted!");

        } catch (Exception e) {
            e.printStackTrace();
        }


    }



    @Override
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";

        try (Connection conn = MyBD.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setNom(rs.getString("Nom"));
                user.setPrenom(rs.getString("Prenom"));
                user.setEmail(rs.getString("Email"));
                user.setPassword(rs.getString("Password"));
                user.setNumtel(rs.getInt("NumeroTelephone"));
                user.setRole(User.Roles.valueOf(rs.getString("Role").toUpperCase()));

                users.add(user);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return users;
    }


}

