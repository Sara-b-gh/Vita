package Services;

import Entites.RendezVous;
import Interffaces.InterfaceCRUD; //interface générique
import MyDB.MyBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RendezVousCRUD implements InterfaceCRUD<RendezVous> {
    Connection conn; //conn qui représente le canal ouvert vers la base de données

    public RendezVousCRUD() {
        try {
            conn = MyBD.getConnection();
        } catch (Exception e) {
            System.out.println("Erreur de connexion : " + e.getMessage());
        }
    }
//Si la connexion échoue (MySQL pas démarré, mauvais mot de passe...)
// on affiche l'erreur au lieu de faire planter toute l'application.

    @Override
    public void ajouter(RendezVous rv) throws SQLException {
        String req = "INSERT INTO rendez_vous (patient_id, medecin_id, date_rdv, motif, statut, lieu, notes) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement pst = conn.prepareStatement(req, Statement.RETURN_GENERATED_KEYS);
        //prepareStatement(req) → compile la requête SQL à l'avance (plus sûr et plus rapide)
        //Statement.RETURN_GENERATED_KEYS → demande à MySQL de retourner l'ID auto-incrémenté qui sera créé

        pst.setInt(1, rv.getPatient_id());
        pst.setInt(2, rv.getMedecin_id());
        pst.setTimestamp(3, Timestamp.valueOf(rv.getDate_rdv()));
        pst.setString(4, rv.getMotif());
        pst.setString(5, rv.getStatut());
        pst.setString(6, rv.getLieu());
        pst.setString(7, rv.getNotes());
        pst.executeUpdate();

        // Récupérer l'id_rdv généré
        ResultSet generatedKeys = pst.getGeneratedKeys();
        if (generatedKeys.next()) {
            rv.setId_rdv(generatedKeys.getInt(1)); // ✅ l'objet reçoit son ID
        }

        System.out.println("Rendez-vous ajouté avec id_rdv = " + rv.getId_rdv());
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
        System.out.println("Rendez-vous modifié !");
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String req = "DELETE FROM rendez_vous WHERE id_rdv=?";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, id);
        pst.executeUpdate();
        System.out.println("Rendez-vous supprimé !");
    }

    @Override
    public List<RendezVous> afficher() throws SQLException {
        List<RendezVous> liste = new ArrayList<>();
        String req = "SELECT * FROM rendez_vous";
        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(req);

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

            // Champs nullable
            Timestamp tc = rs.getTimestamp("date_creation");
            if (tc != null) rv.setDate_creation(tc.toLocalDateTime());

            Timestamp tm = rs.getTimestamp("date_modification");
            if (tm != null) rv.setDate_modification(tm.toLocalDateTime());

            liste.add(rv);
        }

        return liste;
    }
}