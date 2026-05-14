package com.vita.devora.Services;

import com.vita.devora.Entities.User.User;
import com.vita.devora.MyDB.MyBD;
import com.vita.devora.utils.PasswordHasher;

import java.sql.*;
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

        if (!User.isValidEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email invalide !");
        }

        if (!User.isValidPhone(user.getNumtel())) {
            throw new IllegalArgumentException("Numéro de téléphone invalide !");
        }

        // ← HACHAGE du mot de passe
        String hashedPassword = PasswordHasher.hash(user.getPassword());
        user.setPassword(hashedPassword);

        String sqlUser = "INSERT INTO users (Id, Nom, Prenom, Email, Password, NumeroTelephone, Role, DateNaissance) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement psUser = conn.prepareStatement(sqlUser, Statement.RETURN_GENERATED_KEYS)) {

                psUser.setInt(1, user.getId());
                psUser.setString(2, user.getNom());
                psUser.setString(3, user.getPrenom());
                psUser.setString(4, user.getEmail());
                psUser.setString(5, hashedPassword);
                psUser.setInt(6, user.getNumtel());
                psUser.setString(7, user.getRole().name());
                if (user.getDateNaissance() != null) {
                    psUser.setDate(8, java.sql.Date.valueOf(user.getDateNaissance()));
                } else {
                    psUser.setNull(8, java.sql.Types.DATE);
                }

                psUser.executeUpdate();

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

                conn.commit();

            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }

    // ───────── GET BY ID ─────────
    public User getUserById(int id) {
        String sql = "SELECT u.*, d.departement " +
                "FROM users u " +
                "LEFT JOIN doctor d ON u.id = d.id_user " +
                "WHERE u.id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // ───────── UPDATE ─────────
    public void modifier(User user) throws SQLException {

        String sqlUser = "UPDATE users SET Nom=?, Prenom=?, Email=?, NumeroTelephone=?, DateNaissance=? WHERE id=?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlUser)) {
            ps.setString(1, user.getNom());
            ps.setString(2, user.getPrenom());
            ps.setString(3, user.getEmail());
            ps.setInt(4, user.getNumtel());
            if (user.getDateNaissance() != null) {
                ps.setDate(5, java.sql.Date.valueOf(user.getDateNaissance()));
            } else {
                ps.setNull(5, java.sql.Types.DATE);
            }
            ps.setInt(6, user.getId());
            ps.executeUpdate();
        }

        if (user.getRole() == User.Roles.DOCTOR) {
            String sqlDoctor = "UPDATE doctor SET departement=? WHERE id_user=?";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sqlDoctor)) {
                ps.setString(1, user.getDepartement());
                ps.setInt(2, user.getId());
                ps.executeUpdate();
            }
        } else if (user.getRole() == User.Roles.PATIENT) {
            String sqlPatient = "UPDATE patient SET blood_type=? WHERE id_user=?";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sqlPatient)) {
                ps.setString(1, user.getBloodType());
                ps.setInt(2, user.getId());
                ps.executeUpdate();
            }
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
            while (rs.next()) list.add(mapRow(rs));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    // ───────── AUTHENTIFICATION avec HACHAGE ─────────
    public User authentifier(String email, String password) {

        // ← HACHAGE du mot de passe saisi
        String hashedPassword = PasswordHasher.hash(password);

        String sql = "SELECT u.*, d.departement, p.blood_type " +
                "FROM users u " +
                "LEFT JOIN doctor d ON u.id = d.id_user " +
                "LEFT JOIN patient p ON u.id = p.id_user " +
                "WHERE u.email = ? AND u.password = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, hashedPassword); // ← comparaison avec hash
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // ───────── GET BY ROLE ─────────
    public List<User> getByRole(User.Roles role) {
        List<User> list = new ArrayList<>();
        String sql = "SELECT u.*, d.departement " +
                "FROM users u " +
                "LEFT JOIN doctor d ON u.id = d.id_user " +
                "WHERE u.Role = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, role.name());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
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

        Date sqlDate = rs.getDate("DateNaissance");
        if (sqlDate != null) user.setDateNaissance(sqlDate.toLocalDate());

        try { user.setBloodType(rs.getString("blood_type")); } catch (SQLException ignored) {}
        try { user.setDepartement(rs.getString("departement")); } catch (SQLException ignored) {}

        return user;
    }

    // ───────── PATIENTS BY DOCTOR ─────────
    public List<User> getPatientsByDoctor(int doctorId) throws SQLException {
        List<User> liste = new ArrayList<>();
        String req = "SELECT DISTINCT u.*, p.blood_type " +
                "FROM users u " +
                "JOIN rendez_vous rv ON rv.IdPatient = u.id " +
                "LEFT JOIN patient p ON p.id_user = u.id " +
                "WHERE rv.IdDocteur = ?";

        try (Connection conn = MyBD.getInstance().getConnection();
             PreparedStatement pst = conn.prepareStatement(req)) {
            pst.setInt(1, doctorId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) liste.add(mapRow(rs));
            }
        }
        return liste;
    }

    // ───────── GET DOCTORS ─────────
    public List<User> getDoctors() throws SQLException {
        List<User> list = new ArrayList<>();
        String sql = "SELECT u.*, d.departement " +
                "FROM users u " +
                "JOIN doctor d ON u.ID = d.id_user " +
                "WHERE u.Role = 'DOCTOR'";

        PreparedStatement ps = getConnection().prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) list.add(mapRow(rs));
        return list;
    }

    // ───────── GET PATIENTS ─────────
    public List<User> getPatients() throws SQLException {
        List<User> list = new ArrayList<>();
        String sql = "SELECT u.*, p.blood_type " +
                "FROM users u " +
                "LEFT JOIN patient p ON u.ID = p.id_user " +
                "WHERE u.Role = 'PATIENT'";

        PreparedStatement ps = getConnection().prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            User user = mapRow(rs);
            try {
                String bloodType = rs.getString("blood_type");
                if (bloodType != null) user.setBloodType(bloodType);
            } catch (SQLException ignored) {}
            list.add(user);
        }
        return list;
    }

    // ───────── FIND BY EMAIL ─────────
    public User findByEmail(String email) {
        String sql = "SELECT u.*, d.departement " +
                "FROM users u " +
                "LEFT JOIN doctor d ON u.id = d.id_user " +
                "WHERE u.email = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ───────── GET BY EMAIL ─────────
    public User getByEmail(String email) throws SQLException {
        String req = "SELECT * FROM users WHERE email=?";
        try (Connection conn = getConnection();
             PreparedStatement pst = conn.prepareStatement(req)) {
            pst.setString(1, email);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    // ───────── UPDATE PASSWORD avec HACHAGE ─────────
    public void updatePassword(String email, String newPassword) {

        // ← HACHAGE du nouveau mot de passe
        String hashedPassword = PasswordHasher.hash(newPassword);

        String sql = "UPDATE users SET password = ? WHERE email = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hashedPassword);
            ps.setString(2, email);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ───────── GET FULL USER DATA ─────────
    public User getFullUserData(int userId) {
        String sql = "SELECT u.*, p.blood_type " +
                "FROM users u " +
                "LEFT JOIN patient p ON u.Id = p.id_user " +
                "WHERE u.Id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                User user = mapRow(rs);
                user.setBloodType(rs.getString("blood_type"));
                return user;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}