/*package Services;

import Entites.RendezVous;
import Interfaces.InterfaceCRUD;
import MyDB.MyBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RendezVousCRUD implements InterfaceCRUD<RendezVous> {

    Connection conn;
    private final DisponibiliteCRUD dispoCRUD = new DisponibiliteCRUD();

    public RendezVousCRUD() {
        try { conn = MyBD.getConnection(); }
        catch (Exception e) { System.out.println("Erreur de connexion : " + e.getMessage()); }
    }

    @Override
    public void ajouter(RendezVous rv) throws SQLException {
        rv.setStatut("planifie");

        String req = "INSERT INTO rendez_vous (patient_id, medecin_id, date_rdv, motif, statut, lieu, notes) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement pst = conn.prepareStatement(req, Statement.RETURN_GENERATED_KEYS);
        pst.setInt(1, rv.getPatient_id());
        pst.setInt(2, rv.getMedecin_id());
        pst.setTimestamp(3, Timestamp.valueOf(rv.getDate_rdv()));
        pst.setString(4, rv.getMotif());
        pst.setString(5, rv.getStatut());
        pst.setString(6, rv.getLieu());
        pst.setString(7, rv.getNotes());
        pst.executeUpdate();

        ResultSet generatedKeys = pst.getGeneratedKeys();
        if (generatedKeys.next()) {
            rv.setId_rdv(generatedKeys.getInt(1));
        }

        try {
            dispoCRUD.occuperCreneau(rv.getMedecin_id(), rv.getDate_rdv(), rv.getId_rdv());
        } catch (Exception e) {
            System.out.println("Avertissement sync dispo : " + e.getMessage());
        }

        System.out.println("Rendez-vous ajouté (en cours) id=" + rv.getId_rdv());
    }

    @Override
    public void modifier(RendezVous rv) throws SQLException {
        String req = "UPDATE rendez_vous SET patient_id=?, medecin_id=?, date_rdv=?, motif=?, " +
                "statut=?, lieu=?, notes=?, date_modification=NOW() WHERE id_rdv=?";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, rv.getPatient_id());
        pst.setInt(2, rv.getMedecin_id());
        pst.setTimestamp(3, Timestamp.valueOf(rv.getDate_rdv()));
        pst.setString(4, rv.getMotif());
        pst.setString(5, rv.getStatut());
        pst.setString(6, rv.getLieu());
        pst.setString(7, rv.getNotes());
        pst.setInt(8, rv.getId_rdv());
        pst.executeUpdate();

        try {
            dispoCRUD.syncAvecRdv(rv.getId_rdv(), rv.getStatut());
        } catch (Exception e) {
            System.out.println("Avertissement sync dispo : " + e.getMessage());
        }

        System.out.println("Rendez-vous modifié, statut=" + rv.getStatut());
    }

    public void changerStatut(int id_rdv, String nouveauStatut) throws SQLException {
        PreparedStatement pst = conn.prepareStatement(
                "UPDATE rendez_vous SET statut=?, date_modification=NOW() WHERE id_rdv=?");
        pst.setString(1, nouveauStatut);
        pst.setInt(2, id_rdv);
        pst.executeUpdate();

        try { dispoCRUD.syncAvecRdv(id_rdv, nouveauStatut); }
        catch (Exception e) { System.out.println("Avertissement sync dispo : " + e.getMessage()); }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        try { dispoCRUD.syncAvecRdv(id, "annule"); }
        catch (Exception e) { System.out.println("Avertissement sync dispo : " + e.getMessage()); }

        PreparedStatement pst = conn.prepareStatement(
                "DELETE FROM rendez_vous WHERE id_rdv=?");
        pst.setInt(1, id);
        pst.executeUpdate();
        System.out.println("Rendez-vous supprimé id=" + id);
    }

    @Override
    public List<RendezVous> afficher() throws SQLException {
        return fetchWhere("SELECT * FROM rendez_vous ORDER BY date_rdv DESC");
    }

    public List<RendezVous> parPatient(int patient_id) throws SQLException {
        String req = "SELECT * FROM rendez_vous WHERE patient_id=? ORDER BY date_rdv DESC";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, patient_id);
        return map(pst.executeQuery());
    }

    public List<RendezVous> parMedecin(int medecin_id) throws SQLException {
        String req = "SELECT * FROM rendez_vous WHERE medecin_id=? ORDER BY date_rdv DESC";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, medecin_id);
        return map(pst.executeQuery());
    }

    public List<RendezVous> enAttenteParMedecin(int medecin_id) throws SQLException {
        String req = "SELECT * FROM rendez_vous WHERE medecin_id=? AND statut='en_cours' " +
                "ORDER BY date_rdv ASC";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, medecin_id);
        return map(pst.executeQuery());
    }

    private List<RendezVous> fetchWhere(String sql) throws SQLException {
        Statement st = conn.createStatement();
        return map(st.executeQuery(sql));
    }

    private List<RendezVous> map(ResultSet rs) throws SQLException {
        List<RendezVous> liste = new ArrayList<>();
        while (rs.next()) {
            RendezVous rv = new RendezVous();
            rv.setId_rdv(rs.getInt("id_rdv"));
            rv.setPatient_id(rs.getInt("patient_id"));
            rv.setMedecin_id(rs.getInt("medecin_id"));
            rv.setDate_rdv(rs.getTimestamp("date_rdv").toLocalDateTime());
            rv.setMotif(rs.getString("motif"));
            rv.setStatut(rs.getString("statut"));
            rv.setLieu(rs.getString("lieu"));
            rv.setNotes(rs.getString("notes"));
            Timestamp tc = rs.getTimestamp("date_creation");
            if (tc != null) rv.setDate_creation(tc.toLocalDateTime());
            Timestamp tm = rs.getTimestamp("date_modification");
            if (tm != null) rv.setDate_modification(tm.toLocalDateTime());
            liste.add(rv);
        }
        return liste;
    }
}*/
package Services;

import Entites.RendezVous;
import Interfaces.InterfaceCRUD;
import MyDB.MyBD;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RendezVousCRUD implements InterfaceCRUD<RendezVous> {

    private Connection conn;
    private final DisponibiliteCRUD dispoCRUD = new DisponibiliteCRUD();

    public RendezVousCRUD() {
        try {
            conn = MyBD.getConnection();
        } catch (Exception e) {
            System.out.println("Erreur de connexion : " + e.getMessage());
        }
    }

    private void notifier(String action, RendezVous rv) {
        String sujet = "Rendez-vous - " + action;
        StringBuilder contenu = new StringBuilder();
        contenu.append("Action : ").append(action).append("\n\n");
        contenu.append("ID RDV : ").append(rv.getId_rdv()).append("\n");
        contenu.append("Patient ID : ").append(rv.getPatient_id()).append("\n");
        contenu.append("Médecin ID : ").append(rv.getMedecin_id()).append("\n");
        contenu.append("Date : ").append(rv.getDate_rdv()).append("\n");
        contenu.append("Motif : ").append(rv.getMotif()).append("\n");
        contenu.append("Statut : ").append(rv.getStatut()).append("\n");
        contenu.append("Lieu : ").append(rv.getLieu()).append("\n");
        contenu.append("Notes : ").append(rv.getNotes() != null ? rv.getNotes() : "").append("\n");
        contenu.append("Date de l'action : ").append(new Timestamp(System.currentTimeMillis()));

        EmailService.envoyerNotificationRendezVous(sujet, contenu.toString());
    }

    @Override
    public void ajouter(RendezVous rv) throws SQLException {
        rv.setStatut("planifie");

        String req = "INSERT INTO rendez_vous (patient_id, medecin_id, date_rdv, motif, statut, lieu, notes) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement pst = conn.prepareStatement(req, Statement.RETURN_GENERATED_KEYS);
        pst.setInt(1, rv.getPatient_id());
        pst.setInt(2, rv.getMedecin_id());
        pst.setTimestamp(3, Timestamp.valueOf(rv.getDate_rdv()));
        pst.setString(4, rv.getMotif());
        pst.setString(5, rv.getStatut());
        pst.setString(6, rv.getLieu());
        pst.setString(7, rv.getNotes());

        pst.executeUpdate();

        ResultSet generatedKeys = pst.getGeneratedKeys();
        if (generatedKeys.next()) {
            rv.setId_rdv(generatedKeys.getInt(1));
        }

        try {
            dispoCRUD.occuperCreneau(rv.getMedecin_id(), rv.getDate_rdv(), rv.getId_rdv());
        } catch (Exception e) {
            System.out.println("Avertissement sync dispo : " + e.getMessage());
        }

        notifier("Création", rv);
        System.out.println("Rendez-vous ajouté avec succès (id=" + rv.getId_rdv() + ")");
    }

    @Override
    public void modifier(RendezVous rv) throws SQLException {
        String req = "UPDATE rendez_vous SET patient_id=?, medecin_id=?, date_rdv=?, motif=?, " +
                "statut=?, lieu=?, notes=?, date_modification=NOW() WHERE id_rdv=?";

        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, rv.getPatient_id());
        pst.setInt(2, rv.getMedecin_id());
        pst.setTimestamp(3, Timestamp.valueOf(rv.getDate_rdv()));
        pst.setString(4, rv.getMotif());
        pst.setString(5, rv.getStatut());
        pst.setString(6, rv.getLieu());
        pst.setString(7, rv.getNotes());
        pst.setInt(8, rv.getId_rdv());

        pst.executeUpdate();

        try {
            dispoCRUD.syncAvecRdv(rv.getId_rdv(), rv.getStatut());
        } catch (Exception e) {
            System.out.println("Avertissement sync dispo : " + e.getMessage());
        }

        notifier("Modification", rv);
        System.out.println("Rendez-vous modifié, statut=" + rv.getStatut());
    }

    public void changerStatut(int id_rdv, String nouveauStatut) throws SQLException {
        PreparedStatement pst = conn.prepareStatement(
                "UPDATE rendez_vous SET statut=?, date_modification=NOW() WHERE id_rdv=?");
        pst.setString(1, nouveauStatut);
        pst.setInt(2, id_rdv);
        pst.executeUpdate();

        try {
            dispoCRUD.syncAvecRdv(id_rdv, nouveauStatut);
        } catch (Exception e) {
            System.out.println("Avertissement sync dispo : " + e.getMessage());
        }

        // Récupérer le RDV pour l'email
        RendezVous rv = getById(id_rdv);
        if (rv != null) {
            rv.setStatut(nouveauStatut);
            notifier("Changement de statut → " + nouveauStatut, rv);
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        RendezVous rv = getById(id); // pour l'email

        try {
            dispoCRUD.syncAvecRdv(id, "annule");
        } catch (Exception e) {
            System.out.println("Avertissement sync dispo : " + e.getMessage());
        }

        PreparedStatement pst = conn.prepareStatement("DELETE FROM rendez_vous WHERE id_rdv=?");
        pst.setInt(1, id);
        pst.executeUpdate();

        if (rv != null) {
            notifier("Suppression", rv);
        }
        System.out.println("Rendez-vous supprimé id=" + id);
    }

    // Méthode utilitaire pour récupérer un RDV par ID
    public RendezVous getById(int id_rdv) throws SQLException {
        String req = "SELECT * FROM rendez_vous WHERE id_rdv = ?";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, id_rdv);
        ResultSet rs = pst.executeQuery();

        if (rs.next()) {
            RendezVous rv = new RendezVous();
            rv.setId_rdv(rs.getInt("id_rdv"));
            rv.setPatient_id(rs.getInt("patient_id"));
            rv.setMedecin_id(rs.getInt("medecin_id"));
            rv.setDate_rdv(rs.getTimestamp("date_rdv").toLocalDateTime());
            rv.setMotif(rs.getString("motif"));
            rv.setStatut(rs.getString("statut"));
            rv.setLieu(rs.getString("lieu"));
            rv.setNotes(rs.getString("notes"));
            return rv;
        }
        return null;
    }

    // Les autres méthodes restent inchangées
    @Override
    public List<RendezVous> afficher() throws SQLException {
        return fetchWhere("SELECT * FROM rendez_vous ORDER BY date_rdv DESC");
    }

    public List<RendezVous> parPatient(int patient_id) throws SQLException {
        String req = "SELECT * FROM rendez_vous WHERE patient_id=? ORDER BY date_rdv DESC";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, patient_id);
        return map(pst.executeQuery());
    }

    public List<RendezVous> parMedecin(int medecin_id) throws SQLException {
        String req = "SELECT * FROM rendez_vous WHERE medecin_id=? ORDER BY date_rdv DESC";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, medecin_id);
        return map(pst.executeQuery());
    }

    public List<RendezVous> enAttenteParMedecin(int medecin_id) throws SQLException {
        String req = "SELECT * FROM rendez_vous WHERE medecin_id=? AND statut='en_cours' " +
                "ORDER BY date_rdv ASC";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, medecin_id);
        return map(pst.executeQuery());
    }

    private List<RendezVous> fetchWhere(String sql) throws SQLException {
        Statement st = conn.createStatement();
        return map(st.executeQuery(sql));
    }

    private List<RendezVous> map(ResultSet rs) throws SQLException {
        List<RendezVous> liste = new ArrayList<>();
        while (rs.next()) {
            RendezVous rv = new RendezVous();
            rv.setId_rdv(rs.getInt("id_rdv"));
            rv.setPatient_id(rs.getInt("patient_id"));
            rv.setMedecin_id(rs.getInt("medecin_id"));
            rv.setDate_rdv(rs.getTimestamp("date_rdv").toLocalDateTime());
            rv.setMotif(rs.getString("motif"));
            rv.setStatut(rs.getString("statut"));
            rv.setLieu(rs.getString("lieu"));
            rv.setNotes(rs.getString("notes"));
            Timestamp tc = rs.getTimestamp("date_creation");
            if (tc != null) rv.setDate_creation(tc.toLocalDateTime());
            Timestamp tm = rs.getTimestamp("date_modification");
            if (tm != null) rv.setDate_modification(tm.toLocalDateTime());
            liste.add(rv);
        }
        return liste;
    }}