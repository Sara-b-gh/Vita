package Services;

import Entites.CompteRendu;
import Interffaces.InterfaceCRUD;
import Utils.MyBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CompteRenduCRUD implements InterfaceCRUD<CompteRendu> {
    Connection conn;

    public CompteRenduCRUD() {
        conn = MyBD.getInstance().getConn();
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
}