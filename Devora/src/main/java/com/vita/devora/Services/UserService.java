package com.vita.devora.Services;

import com.vita.devora.Entities.User;
import com.vita.devora.Interface.UserCRUD;
import com.vita.devora.MyDB.MyBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserService implements UserCRUD<User> {

    Connection cnx;

    public UserService() {
        cnx = MyBD.getInstance().getConnection();
    }
    public User getUserById(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";

        try (Connection conn = MyBD.getInstance().getConnection();
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
                u.setRole(User.Roles.valueOf(rs.getString("Role").toUpperCase()));

                return u;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void ajouter(User user) {

        // 1. vérifier si user existe
        if (userExists(user.getEmail())) {
            System.out.println("❌ Erreur : cet utilisateur existe déjà !");
            return;
        }

        // 2. sinon insertion
        String req = "INSERT INTO users (Nom, Prenom, Email, Password, NumeroTelephone, Role) VALUES (?, ?, ?, ?, ?, ?)";

        try {
            Connection conn = MyBD.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(req);

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

    public boolean userExists(String email) {

        String sql = "SELECT id FROM users WHERE Email = ?";

        try {
            Connection conn = MyBD.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            return rs.next(); // true = existe

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public void modifier(User user) throws SQLException {
        String sql = "UPDATE users SET Nom=?, Prenom=?, Email=?, Password=?, NumeroTelephone=?, Role=? WHERE id=?";

        try (Connection conn = MyBD.getInstance().getConnection();
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
        String sql = "DELETE FROM users WHERE id=?";

        try (Connection conn = MyBD.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();

            System.out.println("🗑️ User deleted!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
//    public List<User> getAllUsers() {
//        List<User> users = new ArrayList<>();
//        String sql = "SELECT * FROM users";
//
//        try (Connection conn = MyBD.getInstance().getConnection();
//             Statement stmt = conn.createStatement();
//             ResultSet rs = stmt.executeQuery(sql)) {
//
//            while (rs.next()) {
//                User user = new User();
//                user.setId(rs.getInt("id"));
//                user.setNom(rs.getString("Nom"));
//                user.setPrenom(rs.getString("Prenom"));
//                user.setEmail(rs.getString("Email"));
//                user.setPassword(rs.getString("Password"));
//                user.setNumtel(rs.getInt("NumeroTelephone"));
//                user.setRole(User.Roles.valueOf(rs.getString("Role").toUpperCase()));
//
//                users.add(user);
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        return users;
//    }
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";

        // 1. Get the connection WITHOUT putting it in the try() block
        Connection conn = MyBD.getInstance().getConnection();

        // 2. Only put the statement and resultset here to be closed automatically
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                users.add(mapRow(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return users;
    }

    /** Authentification : email + mot de passe */
    public User authentifier(String email, String motDePasse) {
        String req = "SELECT * FROM users WHERE email=? AND Password=?";

        try (Connection conn = MyBD.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(req)) {

            ps.setString(1, email);
            ps.setString(2, motDePasse);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapRow(rs);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
    /** Filtrer par rôle */
//    public List<User> getByRole(User.Roles role) throws SQLException {
//        List<User> liste = new ArrayList<>();
//        String req = "SELECT * FROM users WHERE Role=?";
//        PreparedStatement pst = cnx.prepareStatement(req);
//        pst.setString(1,role.toString().toUpperCase() );
//        ResultSet rs = pst.executeQuery();
//        while (rs.next()) liste.add(mapRow(rs));
//        return liste;
//    }

    public List<User> getByRole(User.Roles role) throws SQLException {
        List<User> liste = new ArrayList<>();
        String req = "SELECT * FROM users WHERE Role=?";

        // Fetch fresh reference from singleton
        Connection conn = MyBD.getInstance().getConnection();

        try (PreparedStatement pst = conn.prepareStatement(req)) {
            pst.setString(1, role.toString().toUpperCase());
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    liste.add(mapRow(rs));
                }
            }
        }
        return liste;
    }

    /** Chercher par email (vérifier doublon avant ajout) */
    public User getByEmail(String email) throws SQLException {
        String req = "SELECT * FROM users WHERE email=?";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setString(1, email);
        ResultSet rs = pst.executeQuery();
        if (rs.next()) return mapRow(rs);
        return null;
    }

    // ─── HELPER PRIVÉ ─────────────────────────────────────────
    private User mapRow(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("id"),
                rs.getString("nom"),
                rs.getString("prenom"),
                rs.getString("email"),
                rs.getString("password"),
                rs.getInt("NumeroTelephone"),
                User.Roles.valueOf(rs.getString("role").toUpperCase())
                //rs.getBoolean("actif")
        );
    }
    public List<User> getPatientsByDoctor(int doctorId) throws SQLException {
        List<User> liste = new ArrayList<>();
        String req = "SELECT DISTINCT u.* FROM users u JOIN rendez_vous rv ON rv.IdPatient = u.id WHERE rv.IdDocteur = ?";
        Connection conn = MyBD.getInstance().getConnection();
        try (PreparedStatement pst = conn.prepareStatement(req)) {
            pst.setInt(1, doctorId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) liste.add(mapRow(rs));
            }
        }
        return liste;
    }
}