/*package Services;

import Entites.CompteRendu;
import Interffaces.InterfaceCRUD;
import MyDB.MyBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CompteRenduCRUD implements InterfaceCRUD<CompteRendu> {
    Connection conn;

    public CompteRenduCRUD() {
        try {
            conn = MyBD.getConnection();
        } catch (Exception e) {
            System.out.println("Erreur de connexion : " + e.getMessage());
        }
    }

    @Override
    public void ajouter(CompteRendu cr) throws SQLException {
        String req = "INSERT INTO compte_rendu (rdv_id, contenu, date_redaction) VALUES (?,?,?)";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, cr.getRdv_id());
        pst.setString(2, cr.getContenu());
        pst.setDate(3, Date.valueOf(cr.getDateRedaction()));
        pst.executeUpdate();
        System.out.println("Compte rendu ajouté !");
    }

    @Override
    public void modifier(CompteRendu cr) throws SQLException {
        String req = "UPDATE compte_rendu SET rdv_id=?, contenu=?, date_redaction=? WHERE id=?";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, cr.getRdv_id());
        pst.setString(2, cr.getContenu());
        pst.setDate(3, Date.valueOf(cr.getDateRedaction()));
        pst.setInt(4, cr.getId());
        pst.executeUpdate();
        System.out.println("Compte rendu modifié !");
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String req = "DELETE FROM compte_rendu WHERE id=?";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, id);
        pst.executeUpdate();
        System.out.println("Compte rendu supprimé !");
    }

    @Override
    public List<CompteRendu> afficher() throws SQLException {
        List<CompteRendu> liste = new ArrayList<>();
        String req = "SELECT * FROM compte_rendu";
        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {
            CompteRendu cr = new CompteRendu();
            cr.setId(rs.getInt("id"));
            cr.setRdv_id(rs.getInt("rdv_id"));
            cr.setContenu(rs.getString("contenu"));
            cr.setDateRedaction(rs.getDate("date_redaction").toLocalDate());
            liste.add(cr);
        }

        return liste;
    }
}*/
package Services;

import Entites.CompteRendu;
import Interfaces.InterfaceCRUD;
import MyDB.MyBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CompteRenduCRUD implements InterfaceCRUD<CompteRendu> {
    Connection conn;

    public CompteRenduCRUD() {
        try {
            conn = MyBD.getConnection();
        } catch (Exception e) {
            System.out.println("Erreur de connexion : " + e.getMessage());
        }
    }

    @Override
    public void ajouter(CompteRendu cr) throws SQLException {
        String req = "INSERT INTO compte_rendu (id_rdv, redige_par, contenu, diagnostic, traitement, prochain_rdv, confidentiel) " +
                "VALUES (?,?,?,?,?,?,?)";
        PreparedStatement pst = conn.prepareStatement(req, Statement.RETURN_GENERATED_KEYS);
        pst.setInt(1, cr.getId_rdv());
        pst.setInt(2, cr.getRedige_par());
        pst.setString(3, cr.getContenu());
        pst.setString(4, cr.getDiagnostic());
        pst.setString(5, cr.getTraitement());
        pst.setDate(6, cr.getProchain_rdv() != null ? Date.valueOf(cr.getProchain_rdv()) : null);
        pst.setBoolean(7, cr.isConfidentiel());
        pst.executeUpdate();
        ResultSet keys = pst.getGeneratedKeys();
        if (keys.next()) {
            cr.setId_cr(keys.getInt(1));
        }
    }

    @Override
    public void modifier(CompteRendu cr) throws SQLException {
        String req = "UPDATE compte_rendu SET id_rdv=?, redige_par=?, contenu=?, diagnostic=?, " +
                "traitement=?, prochain_rdv=?, confidentiel=?, date_modification=NOW() " +
                "WHERE id_cr=?";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, cr.getId_rdv());
        pst.setInt(2, cr.getRedige_par());
        pst.setString(3, cr.getContenu());
        pst.setString(4, cr.getDiagnostic());
        pst.setString(5, cr.getTraitement());
        pst.setDate(6, cr.getProchain_rdv() != null ? Date.valueOf(cr.getProchain_rdv()) : null);
        pst.setBoolean(7, cr.isConfidentiel());
        pst.setInt(8, cr.getId_cr());
        pst.executeUpdate();
        System.out.println("Compte rendu modifié !");
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String req = "DELETE FROM compte_rendu WHERE id_cr=?";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, id);
        pst.executeUpdate();
        System.out.println("Compte rendu supprimé !");
    }

    @Override
    public List<CompteRendu> afficher() throws SQLException {
        List<CompteRendu> liste = new ArrayList<>();
        String req = "SELECT * FROM compte_rendu";
        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {
            CompteRendu cr = new CompteRendu();
            cr.setId_cr(rs.getInt("id_cr"));
            cr.setId_rdv(rs.getInt("id_rdv"));
            cr.setRedige_par(rs.getInt("redige_par"));
            cr.setContenu(rs.getString("contenu"));
            cr.setDiagnostic(rs.getString("diagnostic"));
            cr.setTraitement(rs.getString("traitement"));

            Date prochainRdv = rs.getDate("prochain_rdv");
            cr.setProchain_rdv(prochainRdv != null ? prochainRdv.toLocalDate() : null);

            cr.setConfidentiel(rs.getBoolean("confidentiel"));

            Timestamp dateCreation = rs.getTimestamp("date_creation");
            cr.setDate_creation(dateCreation != null ? dateCreation.toLocalDateTime() : null);

            Timestamp dateMod = rs.getTimestamp("date_modification");
            cr.setDate_modification(dateMod != null ? dateMod.toLocalDateTime() : null);

            liste.add(cr);
        }

        return liste;
    }
    public CompteRendu trouverParRdv(int idRdv) throws SQLException {
        String sql = "SELECT * FROM compte_rendu WHERE id_rdv = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idRdv);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                CompteRendu cr = new CompteRendu();
                cr.setId_cr(rs.getInt("id_cr"));
                cr.setId_rdv(rs.getInt("id_rdv"));
                cr.setRedige_par(rs.getInt("redige_par"));
                cr.setContenu(rs.getString("contenu"));
                cr.setDiagnostic(rs.getString("diagnostic"));
                cr.setTraitement(rs.getString("traitement"));
                cr.setConfidentiel(rs.getBoolean("confidentiel"));
                // adapt column names to match your actual DB schema
                if (rs.getDate("prochain_rdv") != null)
                    cr.setProchain_rdv(rs.getDate("prochain_rdv").toLocalDate());
                if (rs.getTimestamp("date_creation") != null)
                    cr.setDate_creation(rs.getTimestamp("date_creation").toLocalDateTime());
                return cr;
            }
            return null; // pas de CR pour ce RDV
        }
    }
}