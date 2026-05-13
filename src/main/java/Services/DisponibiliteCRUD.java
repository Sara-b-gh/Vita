package services;

import entities.Disponibilite;
import MyDB.MyBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DisponibiliteCRUD {

    private Connection conn;

    public DisponibiliteCRUD() {
        try { conn = MyBD.getConnection(); }
        catch (Exception e) { System.out.println("Erreur connexion : " + e.getMessage()); }
    }

    // ── Ajouter un créneau ────────────────────────────────────────────
    public void ajouter(Disponibilite d) throws SQLException {
        String req = "INSERT INTO disponibilite (medecin_id, date_dispo, heure_debut, heure_fin, statut) " +
                "VALUES (?, ?, ?, ?, ?)";
        PreparedStatement pst = conn.prepareStatement(req, Statement.RETURN_GENERATED_KEYS);
        pst.setInt(1, d.getMedecin_id());
        pst.setDate(2, Date.valueOf(d.getDate_dispo()));
        pst.setTime(3, Time.valueOf(d.getHeure_debut()));
        pst.setTime(4, Time.valueOf(d.getHeure_fin()));
        pst.setString(5, d.getStatut());
        pst.executeUpdate();
        ResultSet rs = pst.getGeneratedKeys();
        if (rs.next()) d.setId_dispo(rs.getInt(1));
    }

    // ── Modifier statut d'un créneau ─────────────────────────────────
    public void modifierStatut(int id_dispo, String statut, Integer id_rdv) throws SQLException {
        String req = "UPDATE disponibilite SET statut=?, id_rdv=? WHERE id_dispo=?";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setString(1, statut);
        if (id_rdv == null) pst.setNull(2, Types.INTEGER);
        else                pst.setInt(2, id_rdv);
        pst.setInt(3, id_dispo);
        pst.executeUpdate();
    }

    // ── Supprimer un créneau ─────────────────────────────────────────
    public void supprimer(int id_dispo) throws SQLException {
        PreparedStatement pst = conn.prepareStatement(
                "DELETE FROM disponibilite WHERE id_dispo=?");
        pst.setInt(1, id_dispo);
        pst.executeUpdate();
    }

    // ── Lister les créneaux d'un médecin ─────────────────────────────
    public List<Disponibilite> parMedecin(int medecin_id) throws SQLException {
        List<Disponibilite> liste = new ArrayList<>();
        String req = "SELECT * FROM disponibilite WHERE medecin_id=? ORDER BY date_dispo, heure_debut";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, medecin_id);
        ResultSet rs = pst.executeQuery();
        while (rs.next()) liste.add(map(rs));
        return liste;
    }

    // ── Créneaux libres d'un médecin à partir d'aujourd'hui ──────────
    public List<Disponibilite> creneauxLibres(int medecin_id) throws SQLException {
        List<Disponibilite> liste = new ArrayList<>();
        String req = "SELECT * FROM disponibilite WHERE medecin_id=? AND statut='libre' " +
                "AND date_dispo >= CURDATE() ORDER BY date_dispo, heure_debut";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, medecin_id);
        ResultSet rs = pst.executeQuery();
        while (rs.next()) liste.add(map(rs));
        return liste;
    }

    // ── Synchroniser : quand un RDV change de statut ─────────────────
    /**
     * Appelé depuis RendezVousCRUD après chaque modification de statut.
     * - RDV "en_attente" ou "confirme" → créneau "occupee"
     * - RDV "annule" ou "termine"      → créneau "libre"
     */
    public void syncAvecRdv(int id_rdv, String nouveauStatutRdv) throws SQLException {
        if ("annule".equals(nouveauStatutRdv) || "termine".equals(nouveauStatutRdv)) {
            // Libérer le créneau lié à ce RDV
            PreparedStatement pst = conn.prepareStatement(
                    "UPDATE disponibilite SET statut='libre', id_rdv=NULL WHERE id_rdv=?");
            pst.setInt(1, id_rdv);
            pst.executeUpdate();
        } else {
            // Marquer occupée
            PreparedStatement pst = conn.prepareStatement(
                    "UPDATE disponibilite SET statut='occupee' WHERE id_rdv=?");
            pst.setInt(1, id_rdv);
            pst.executeUpdate();
        }
    }

    /**
     * Quand un nouveau RDV est créé : trouver le créneau correspondant et le marquer occupé.
     */
    public void occuperCreneau(int medecin_id, java.time.LocalDateTime dateRdv, int id_rdv)
            throws SQLException {
        String req = "UPDATE disponibilite SET statut='occupee', id_rdv=? " +
                "WHERE medecin_id=? AND date_dispo=? " +
                "AND heure_debut <= ? AND heure_fin > ? AND statut='libre' LIMIT 1";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, id_rdv);
        pst.setInt(2, medecin_id);
        pst.setDate(3, Date.valueOf(dateRdv.toLocalDate()));
        pst.setTime(4, Time.valueOf(dateRdv.toLocalTime()));
        pst.setTime(5, Time.valueOf(dateRdv.toLocalTime()));
        pst.executeUpdate();
    }

    // ── Mapping ResultSet → Disponibilite ────────────────────────────
    private Disponibilite map(ResultSet rs) throws SQLException {
        Disponibilite d = new Disponibilite();
        d.setId_dispo(rs.getInt("id_dispo"));
        d.setMedecin_id(rs.getInt("medecin_id"));
        d.setDate_dispo(rs.getDate("date_dispo").toLocalDate());
        d.setHeure_debut(rs.getTime("heure_debut").toLocalTime());
        d.setHeure_fin(rs.getTime("heure_fin").toLocalTime());
        d.setStatut(rs.getString("statut"));
        int rdvId = rs.getInt("id_rdv");
        d.setId_rdv(rs.wasNull() ? null : rdvId);
        return d;
    }
}