package Services;

import Entites.User;
import MyDB.MyBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserService {

    Connection conn;

    public UserService() {
        try {
            conn = MyBD.getConnection();
        } catch (Exception e) {
            System.out.println("Erreur connexion DB : " + e.getMessage());
        }
    }

    public void ajouter(User user) throws SQLException {

        if (!User.isValidEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email invalide !");
        }

        if (!User.isValidPhone(user.getNumtel())) {
            throw new IllegalArgumentException("Numéro invalide !");
        }

        String sqlUser = "INSERT INTO users (Id, Nom, Prenom, Email, Password, NumeroTelephone, Role, DateNaissance) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try {
            conn.setAutoCommit(false);
            PreparedStatement psUser = conn.prepareStatement(sqlUser);
            psUser.setInt(1, user.getId());
            psUser.setString(2, user.getNom());
            psUser.setString(3, user.getPrenom());
            psUser.setString(4, user.getEmail());
            psUser.setString(5, user.getPassword());
            psUser.setInt(6, user.getNumtel());
            psUser.setString(7, user.getRole().name());

            if (user.getDateNaissance() != null) {
                psUser.setDate(8, Date.valueOf(user.getDateNaissance()));
            } else {
                psUser.setNull(8, Types.DATE);
            }

            psUser.executeUpdate();

            if (user.getRole() == User.Roles.DOCTOR) {
                String sqlDoctor = "INSERT INTO doctor (id_user, departement) VALUES (?, ?)";
                PreparedStatement ps = conn.prepareStatement(sqlDoctor);
                ps.setInt(1, user.getId());
                ps.setString(2, user.getDepartement());
                ps.executeUpdate();
            } else if (user.getRole() == User.Roles.PATIENT) {
                String sqlPatient = "INSERT INTO patient (id_user, blood_type) VALUES (?, ?)";
                PreparedStatement ps = conn.prepareStatement(sqlPatient);
                ps.setInt(1, user.getId());
                ps.setString(2, user.getBloodType());
                ps.executeUpdate();
            }

            conn.commit();
            System.out.println("Utilisateur ajouté !");

        } catch (Exception e) {
            conn.rollback();
            System.out.println("Erreur ajout : " + e.getMessage());
        }
    }

    public User getByEmail(String email) throws SQLException {
        String req = "SELECT * FROM users WHERE email=?";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setString(1, email);
        ResultSet rs = pst.executeQuery();
        if (rs.next()) {
            return mapRow(rs);
        }
        return null;
    }

    public void updatePassword(String email, String newPassword) {
        String sql = "UPDATE users SET password=? WHERE email=?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, newPassword);
            ps.setString(2, email);
            ps.executeUpdate();
            System.out.println("Mot de passe modifié !");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public User findById(int id) {
        User user = null;
        String sql = "SELECT * FROM users WHERE id = ?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                user = mapRow(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return user;
    }

    public List<User> afficherTous() throws SQLException {
        List<User> liste = new ArrayList<>();
        String sql = "SELECT * FROM users";
        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            liste.add(mapRow(rs));
        }
        return liste;
    }


    public List<User> getDoctors() throws SQLException {
        List<User> liste = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE Role = 'DOCTOR' ORDER BY Nom, Prenom";
        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            liste.add(mapRow(rs));
        }
        return liste;
    }
    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User(
                rs.getInt("id"),
                rs.getString("Nom"),
                rs.getString("Prenom"),
                rs.getString("Email"),
                rs.getString("Password"),
                rs.getInt("NumeroTelephone"),
                User.Roles.valueOf(rs.getString("Role").toUpperCase())
        );

        Date sqlDate = rs.getDate("DateNaissance");
        if (sqlDate != null) {
            user.setDateNaissance(sqlDate.toLocalDate());
        }

        return user;
    }
}