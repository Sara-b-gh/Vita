package Services;

import Entites.RendezVous;
import Interffaces.InterfaceCRUD;
import MyDB.MyBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RendezVousCRUD implements InterfaceCRUD<RendezVous> {
    Connection conn;

    public RendezVousCRUD() {
        try {
            conn = MyBD.getConnection(); // ✅ méthode statique existante
        } catch (Exception e) {
            System.out.println("Erreur de connexion : " + e.getMessage());
        }
    }

    @Override
    public void ajouter(RendezVous rv) throws SQLException {
        String req = "INSERT INTO rendez_vous (date, motif, statut, medecin_id) VALUES (?,?,?,?)";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setTimestamp(1, Timestamp.valueOf(rv.getDate()));
        pst.setString(2, rv.getMotif());
        pst.setString(3, rv.getStatut());
        pst.setInt(4, rv.getMedecin_id());
        pst.executeUpdate();
        System.out.println("Rendez-vous ajouté !");
    }

    @Override
    public void modifier(RendezVous rv) throws SQLException {
        String req = "UPDATE rendez_vous SET date=?, motif=?, statut=?, medecin_id=? WHERE id=?";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setTimestamp(1, Timestamp.valueOf(rv.getDate()));
        pst.setString(2, rv.getMotif());
        pst.setString(3, rv.getStatut());
        pst.setInt(4, rv.getMedecin_id());
        pst.setInt(5, rv.getId());
        pst.executeUpdate();
        System.out.println("Rendez-vous modifié !");
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String req = "DELETE FROM rendez_vous WHERE id=?";
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
            rv.setId(rs.getInt("id"));
            rv.setDate(rs.getTimestamp("date").toLocalDateTime());
            rv.setMotif(rs.getString("motif"));
            rv.setStatut(rs.getString("statut"));
            rv.setMedecin_id(rs.getInt("medecin_id"));
            liste.add(rv);
        }

        return liste;
    }
}