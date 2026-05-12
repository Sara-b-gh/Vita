package services;

import Entites.RendezVous;
import utils.MyBD;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RendezVousCRUD {
    private Connection connection;

    public RendezVousCRUD() {
        try {
            this.connection = MyBD.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public RendezVousCRUD(Connection connection) {
        this.connection = connection;
    }

    // Ajouter
    public boolean ajouter(RendezVous rdv) {
        String sql = "INSERT INTO rendez_vous (patient_id, medecin_id, date_rdv, motif, statut, lieu, notes, date_creation) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, rdv.getPatient_id());
            stmt.setInt(2, rdv.getMedecin_id());
            stmt.setObject(3, rdv.getDate_rdv());
            stmt.setString(4, rdv.getMotif());
            stmt.setString(5, rdv.getStatut());
            stmt.setString(6, rdv.getLieu());
            stmt.setString(7, rdv.getNotes());
            stmt.setObject(8, LocalDateTime.now());

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    rdv.setId_rdv(rs.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Modifier
    public boolean modifier(RendezVous rdv) {
        String sql = "UPDATE rendez_vous SET patient_id=?, medecin_id=?, date_rdv=?, motif=?, statut=?, lieu=?, notes=?, date_modification=? WHERE id_rdv=?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, rdv.getPatient_id());
            stmt.setInt(2, rdv.getMedecin_id());
            stmt.setObject(3, rdv.getDate_rdv());
            stmt.setString(4, rdv.getMotif());
            stmt.setString(5, rdv.getStatut());
            stmt.setString(6, rdv.getLieu());
            stmt.setString(7, rdv.getNotes());
            stmt.setObject(8, LocalDateTime.now());
            stmt.setInt(9, rdv.getId_rdv());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Afficher tous
    public List<RendezVous> afficher() {
        List<RendezVous> list = new ArrayList<>();
        String sql = "SELECT * FROM rendez_vous ORDER BY date_rdv DESC";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(extractRendezVous(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // GET ALL
    public List<RendezVous> getAll() {
        return afficher();
    }

    // Par médecin
    public List<RendezVous> parMedecin(int medecinId) {
        List<RendezVous> list = new ArrayList<>();
        String sql = "SELECT * FROM rendez_vous WHERE medecin_id = ? ORDER BY date_rdv DESC";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, medecinId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(extractRendezVous(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // Par patient
    public List<RendezVous> parPatient(int patientId) {
        List<RendezVous> list = new ArrayList<>();
        String sql = "SELECT * FROM rendez_vous WHERE patient_id = ? ORDER BY date_rdv DESC";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, patientId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(extractRendezVous(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // Changer statut
    public boolean changerStatut(int idRdv, String nouveauStatut) {
        String sql = "UPDATE rendez_vous SET statut = ?, date_modification = ? WHERE id_rdv = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, nouveauStatut);
            stmt.setObject(2, LocalDateTime.now());
            stmt.setInt(3, idRdv);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Supprimer
    public boolean supprimer(int idRdv) {
        String sql = "DELETE FROM rendez_vous WHERE id_rdv = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, idRdv);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // DELETE
    public boolean delete(int idRdv) {
        return supprimer(idRdv);
    }

    // Find by ID
    public RendezVous findById(int id) {
        String sql = "SELECT * FROM rendez_vous WHERE id_rdv = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return extractRendezVous(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private RendezVous extractRendezVous(ResultSet rs) throws SQLException {
        RendezVous rdv = new RendezVous();
        rdv.setId_rdv(rs.getInt("id_rdv"));
        rdv.setPatient_id(rs.getInt("patient_id"));
        rdv.setMedecin_id(rs.getInt("medecin_id"));
        rdv.setDate_rdv(rs.getObject("date_rdv", LocalDateTime.class));
        rdv.setMotif(rs.getString("motif"));
        rdv.setStatut(rs.getString("statut"));
        rdv.setLieu(rs.getString("lieu"));
        rdv.setNotes(rs.getString("notes"));
        rdv.setDate_creation(rs.getObject("date_creation", LocalDateTime.class));
        rdv.setDate_modification(rs.getObject("date_modification", LocalDateTime.class));
        return rdv;
    }
}