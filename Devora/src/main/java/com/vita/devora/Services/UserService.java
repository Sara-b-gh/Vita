package com.vita.devora.Services;

import com.vita.devora.Entities.User;
import com.vita.devora.MyDB.MyBD;
import javafx.util.converter.LocalDateStringConverter;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class UserService {
     Connection cnx;
    // ───────── CONNECTION ─────────
    private Connection getConnection() {
        return MyBD.getInstance().getConnection();
    }

    // ───────── AJOUT USER + TABLE FILLE ─────────
    public void ajouter(User user) throws SQLException {

        // 🔴 VALIDATIONS
        if (!User.isValidEmail(user.getEmail())) {
            throw new IllegalArgumentException(" Email invalide !");
        }

        if (!User.isValidPhone(user.getNumtel())) {
            throw new IllegalArgumentException(" Numéro de téléphone invalide !");
        }

        String sqlUser = "INSERT INTO users (Id, Nom, Prenom, Email, Password, NumeroTelephone, Role, DateNaissance) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false); // 🔥 TRANSACTION START

            try (PreparedStatement psUser = conn.prepareStatement(sqlUser, Statement.RETURN_GENERATED_KEYS)) {

                // 1. INSERT USER
                psUser.setInt(1, user.getId());
                psUser.setString(2, user.getNom());
                psUser.setString(3, user.getPrenom());
                psUser.setString(4, user.getEmail());
                psUser.setString(5, user.getPassword());
                psUser.setInt(6, user.getNumtel());
                psUser.setString(7, user.getRole().name());
                if (user.getDateNaissance() != null) {
                    psUser.setDate(8, java.sql.Date.valueOf(user.getDateNaissance()));
                } else {
                    psUser.setNull(8, java.sql.Types.DATE);
                }

                psUser.executeUpdate();


                // 2. INSERT TABLE FILLE
                if (user.getRole() == User.Roles.DOCTOR) {

                    String sqlDoctor = "INSERT INTO doctor (id_user, departement) VALUES (?, ?)";

                    try (PreparedStatement ps = conn.prepareStatement(sqlDoctor)) {
                        ps.setInt(1, user.getId());
                        ps.setString(2, user.getDepartement());
                        ps.executeUpdate();
                    }

                } else if (user.getRole() == User.Roles.PATIENT) {

                    String sqlPatient = "INSERT INTO patient (id_user, blood_type) VALUES (?, ?)";

                    try (PreparedStatement ps = conn.prepareStatement(sqlPatient)) {
                        ps.setInt(1, user.getId());
                        ps.setString(2, user.getBloodType());
                        ps.executeUpdate();
                    }
                }

                conn.commit(); // ✅ SUCCESS

            } catch (Exception e) {
                conn.rollback(); // ❌ rollback si erreur
                throw e;
            }
        }
    }

    // ───────── GET BY ID ─────────
    public User getUserById(int id) {

        String sql = "SELECT * FROM users WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapRow(rs);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    // ───────── UPDATE ─────────
    public void modifier(User user) throws SQLException {

        String sql = "UPDATE users SET Nom=?, Prenom=?, Email=?,  NumeroTelephone=?, Role=?, DateNaissance=? WHERE id=?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user.getNom());
            ps.setString(2, user.getPrenom());
            ps.setString(3, user.getEmail());
            //ps.setString(4, user.getPassword());
            ps.setInt(4, user.getNumtel());
            ps.setString(5, user.getRole().name());
            if (user.getDateNaissance() != null) {
                ps.setDate(6, java.sql.Date.valueOf(user.getDateNaissance()));
            } else {
                ps.setNull(6, java.sql.Types.DATE);
            }
            ps.setInt(7, user.getId());

            ps.executeUpdate();
        }
    }

    // ───────── DELETE ─────────
    public void supprimer(int id) throws SQLException {

        String sql = "DELETE FROM users WHERE id=?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ───────── GET ALL ─────────
    public List<User> getAllUsers() {

        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users";

        try (Connection conn = getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    // ───────── AUTHENTIFICATION ─────────
//    public User authentifier(String email, String password) {
//
//        String sql = "SELECT * FROM users WHERE email=? AND password=?";
//
//        try (Connection conn = getConnection();
//             PreparedStatement ps = conn.prepareStatement(sql)) {
//
//            ps.setString(1, email);
//            ps.setString(2, password);
//
//            ResultSet rs = ps.executeQuery();
//
//            if (rs.next()) {
//                return mapRow(rs);
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        return null;
//    }

    public User authentifier(String email, String password) {
        // Join users with patient table to get blood_type
        String sql = "SELECT u.*, p.blood_type " +
                "FROM users u " +
                "LEFT JOIN patient p ON u.id = p.id_user " +
                "WHERE u.email = ? AND u.password = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);
            ps.setString(2, password);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                User user = mapRow(rs); // Map basic fields

                // Manually extract the blood_type from the join result
                if (user.getRole() == User.Roles.PATIENT) {
                    user.setBloodType(rs.getString("blood_type"));
                }
                return user;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // ───────── GET BY ROLE ─────────
    public List<User> getByRole(User.Roles role) {

        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE Role=?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, role.name());

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(mapRow(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    // ───────── MAPPER ─────────
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

        Date sqlDate = rs.getDate(8);
        System.out.println("DB Raw Date: " + sqlDate); // Is this null?
        if (sqlDate != null) {
            user.setDateNaissance(sqlDate.toLocalDate());
        }

        // Check if the blood_type column exists in the current result set
        // (This prevents errors if you call mapRow on a query that didn't join the patient table)
        try {
            String bt = rs.getString("blood_type");
            if (bt != null) {
                user.setBloodType(bt);
            }
        } catch (SQLException e) {
            // Column not found in this specific query, skip it
        }

        return user;
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
    public List<User> getDoctors() throws SQLException {
        List<User> list = new ArrayList<>();

        String sql = "SELECT u.*, d.departement " +
                "FROM users u " +
                "JOIN doctor d ON u.ID = d.id_user " +
                "WHERE u.Role = 'DOCTOR'";

        PreparedStatement ps = getConnection().prepareStatement(sql);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            User user = mapRow(rs);
            // très important
            user.setDepartement(rs.getString("departement"));

            list.add(user);
        }

        return list;
    }

//    public void updatePassword(String email, String newPassword) {
//
//        String sql = "UPDATE user SET password = ? WHERE email = ?";
//        try {
//            PreparedStatement ps = cnx.prepareStatement(sql);
//
//            ps.setString(1, newPassword);
//            ps.setString(2, email);
//
//            ps.executeUpdate();
//
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//    }

    // 1. Fixed getByEmail
    public User getByEmail(String email) throws SQLException {
        String req = "SELECT * FROM users WHERE email=?";
        // Use getConnection() instead of the null cnx variable
        try (Connection conn = getConnection();
             PreparedStatement pst = conn.prepareStatement(req)) {
            pst.setString(1, email);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    // 2. Fixed findByEmail (The one causing your current crash)
    public User findByEmail(String email) {
        User user = null;
        // Changed table name to 'users' to match your other queries
        String sql = "SELECT * FROM users WHERE email = ?";

        try (Connection conn = getConnection(); // Use the helper method
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Using your mapRow helper is cleaner than manual setting
                    user = mapRow(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return user;
    }

    // 3. Fixed updatePassword
    public void updatePassword(String email, String newPassword) {
        String sql = "UPDATE users SET password = ? WHERE email = ?";
        try (Connection conn = getConnection(); // Use the helper method
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newPassword);
            ps.setString(2, email);
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public User getFullUserData(int userId) {
        // We join 'users' with 'patient' to get the blood_type
        String sql = "SELECT u.*, p.blood_type " +
                "FROM users u " +
                "LEFT JOIN patient p ON u.Id = p.id_user " +
                "WHERE u.Id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                User user = mapRow(rs); // Map basic info
                // Manually set the blood type from the joined column
                user.setBloodType(rs.getString("blood_type"));
                return user;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}