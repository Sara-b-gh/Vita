package services;

import entities.CompteRendu;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class CompteRenduCRUD {
    private static final Logger LOGGER = Logger.getLogger(CompteRenduCRUD.class.getName());
    private final Connection connection;

    public CompteRenduCRUD() throws SQLException {
        this.connection = utils.MyBD.getConnection();
    }

    public entities.CompteRendu trouverParRdv(int idRdv) {
        String sql = "SELECT * FROM compte_rendus WHERE id_rdv = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, idRdv);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                CompteRendu cr = new CompteRendu();
                cr.setId_cr(rs.getInt("id_cr"));
                cr.setId_rdv(rs.getInt("id_rdv"));
                cr.setRedige_par(rs.getInt("redige_par"));
                cr.setContenu(rs.getString("contenu"));
                cr.setDiagnostic(rs.getString("diagnostic"));
                cr.setTraitement(rs.getString("traitement"));
                cr.setProchain_rdv(rs.getObject("prochain_rdv", LocalDate.class));
                cr.setConfidentiel(rs.getBoolean("confidentiel"));
                cr.setDate_creation(rs.getObject("date_creation", LocalDateTime.class));
                cr.setDate_modification(rs.getObject("date_modification", LocalDateTime.class));
                return cr;
            }
        } catch (SQLException e) {
            LOGGER.severe("Erreur trouverParRdv: " + e.getMessage());
        }
        return null;
    }

    public void ajouter(CompteRendu cr) throws SQLException {
        String sql = "INSERT INTO compte_rendus (id_rdv, redige_par, contenu, diagnostic, traitement, prochain_rdv, confidentiel, date_creation) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, cr.getId_rdv());
            stmt.setInt(2, cr.getRedige_par());
            stmt.setString(3, cr.getContenu());
            stmt.setString(4, cr.getDiagnostic());
            stmt.setString(5, cr.getTraitement());
            stmt.setObject(6, cr.getProchain_rdv());
            stmt.setBoolean(7, cr.isConfidentiel());
            stmt.setObject(8, LocalDateTime.now());
            stmt.executeUpdate();
        }
    }

    public List<entities.CompteRendu> afficher() throws SQLException {
        List<CompteRendu> list = new ArrayList<>();
        String sql = "SELECT * FROM compte_rendus ORDER BY date_creation DESC";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                CompteRendu cr = new CompteRendu();
                cr.setId_cr(rs.getInt("id_cr"));
                cr.setId_rdv(rs.getInt("id_rdv"));
                cr.setRedige_par(rs.getInt("redige_par"));
                cr.setContenu(rs.getString("contenu"));
                cr.setDiagnostic(rs.getString("diagnostic"));
                cr.setTraitement(rs.getString("traitement"));
                cr.setProchain_rdv(rs.getObject("prochain_rdv", LocalDate.class));
                cr.setConfidentiel(rs.getBoolean("confidentiel"));
                cr.setDate_creation(rs.getObject("date_creation", LocalDateTime.class));
                cr.setDate_modification(rs.getObject("date_modification", LocalDateTime.class));
                list.add(cr);
            }
        }
        return list;
    }

    /**
     * Supprime un compte rendu par son ID
     * @param idCr L'ID du compte rendu à supprimer
     * @throws SQLException Si une erreur SQL survient
     */
    public void supprimer(int idCr) throws SQLException {
        String sql = "DELETE FROM compte_rendus WHERE id_cr = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, idCr);
            int lignesAffectees = stmt.executeUpdate();
            if (lignesAffectees == 0) {
                throw new SQLException("Aucun compte rendu trouvé avec l'ID: " + idCr);
            }
            LOGGER.info("Compte rendu supprimé avec succès. ID: " + idCr);
        }
    }

    /**
     * Supprime tous les comptes rendus associés à un rendez-vous
     * @param idRdv L'ID du rendez-vous
     * @throws SQLException Si une erreur SQL survient
     */
    public void supprimerParRdv(int idRdv) throws SQLException {
        String sql = "DELETE FROM compte_rendus WHERE id_rdv = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, idRdv);
            int lignesAffectees = stmt.executeUpdate();
            LOGGER.info(lignesAffectees + " compte(s) rendu(s) supprimé(s) pour le RDV: " + idRdv);
        }
    }

    /**
     * Met à jour un compte rendu existant
     * @param cr Le compte rendu avec les nouvelles valeurs
     * @throws SQLException Si une erreur SQL survient
     */
    public void modifier(CompteRendu cr) throws SQLException {
        String sql = "UPDATE compte_rendus SET id_rdv = ?, redige_par = ?, contenu = ?, diagnostic = ?, traitement = ?, prochain_rdv = ?, confidentiel = ?, date_modification = ? WHERE id_cr = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, cr.getId_rdv());
            stmt.setInt(2, cr.getRedige_par());
            stmt.setString(3, cr.getContenu());
            stmt.setString(4, cr.getDiagnostic());
            stmt.setString(5, cr.getTraitement());
            stmt.setObject(6, cr.getProchain_rdv());
            stmt.setBoolean(7, cr.isConfidentiel());
            stmt.setObject(8, LocalDateTime.now());
            stmt.setInt(9, cr.getId_cr());

            int lignesAffectees = stmt.executeUpdate();
            if (lignesAffectees == 0) {
                throw new SQLException("Aucun compte rendu trouvé avec l'ID: " + cr.getId_cr());
            }
            LOGGER.info("Compte rendu modifié avec succès. ID: " + cr.getId_cr());
        }
    }

    /**
     * Trouve un compte rendu par son ID
     * @param idCr L'ID du compte rendu
     * @return Le compte rendu trouvé ou null
     */
    public CompteRendu trouverParId(int idCr) {
        String sql = "SELECT * FROM compte_rendus WHERE id_cr = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, idCr);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                CompteRendu cr = new CompteRendu();
                cr.setId_cr(rs.getInt("id_cr"));
                cr.setId_rdv(rs.getInt("id_rdv"));
                cr.setRedige_par(rs.getInt("redige_par"));
                cr.setContenu(rs.getString("contenu"));
                cr.setDiagnostic(rs.getString("diagnostic"));
                cr.setTraitement(rs.getString("traitement"));
                cr.setProchain_rdv(rs.getObject("prochain_rdv", LocalDate.class));
                cr.setConfidentiel(rs.getBoolean("confidentiel"));
                cr.setDate_creation(rs.getObject("date_creation", LocalDateTime.class));
                cr.setDate_modification(rs.getObject("date_modification", LocalDateTime.class));
                return cr;
            }
        } catch (SQLException e) {
            LOGGER.severe("Erreur trouverParId: " + e.getMessage());
        }
        return null;
    }

    /**
     * Compte le nombre de comptes rendus pour un rendez-vous
     * @param idRdv L'ID du rendez-vous
     * @return Le nombre de comptes rendus
     * @throws SQLException Si une erreur SQL survient
     */
    public int compterParRdv(int idRdv) throws SQLException {
        String sql = "SELECT COUNT(*) FROM compte_rendus WHERE id_rdv = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, idRdv);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }

    /**
     * Vérifie si un compte rendu existe pour un rendez-vous
     * @param idRdv L'ID du rendez-vous
     * @return true si un compte rendu existe, false sinon
     * @throws SQLException Si une erreur SQL survient
     */
    public boolean existePourRdv(int idRdv) throws SQLException {
        return compterParRdv(idRdv) > 0;
    }



    }