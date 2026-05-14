package services;

import com.example.vita.Entites.Commande;
import com.example.vita.Entites.LigneCommande;
import com.example.vita.utils.MyBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CommandeCRUD {

    private final Connection conn = MyBD.getInstance().getConn();

    public void ajouter(Commande c) throws SQLException {
        String req = "INSERT INTO commande (statut, montant_total, mode_paiement) VALUES (?,?,?)";
        PreparedStatement pst = conn.prepareStatement(req, Statement.RETURN_GENERATED_KEYS);
        pst.setString(1, c.getStatut());
        pst.setDouble(2, c.getMontant_total());
        pst.setString(3, c.getMode_paiement());
        pst.executeUpdate();
        ResultSet rs = pst.getGeneratedKeys();
        if (rs.next()) c.setId_commande(rs.getInt(1));

        for (LigneCommande l : c.getLignes()) {
            l.setId_commande(c.getId_commande());
            ajouterLigne(l);
        }
    }

    private void ajouterLigne(LigneCommande l) throws SQLException {
        String req = "INSERT INTO ligne_commande (id_commande, id_medicament, id_equipement, quantite, prix_unitaire) VALUES (?,?,?,?,?)";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, l.getId_commande());
        pst.setInt(2, l.getId_medicament());
        pst.setInt(3, l.getId_equipement());
        pst.setInt(4, l.getQuantite());
        pst.setDouble(5, l.getPrix_unitaire());
        pst.executeUpdate();
    }

    public void mettreAJourStatut(int id, String statut,
                                  String reference) throws SQLException {
        String req = "UPDATE commande SET statut=?, reference_paiement=? WHERE id_commande=?";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setString(1, statut);
        pst.setString(2, reference);
        pst.setInt(3, id);
        pst.executeUpdate();
    }

    public List<Commande> afficher() throws SQLException {
        List<Commande> list = new ArrayList<>();
        String req = "SELECT * FROM commande ORDER BY date_commande DESC";
        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(req);
        while (rs.next()) {
            Commande c = new Commande();
            c.setId_commande(rs.getInt("id_commande"));
            c.setStatut(rs.getString("statut"));
            c.setMontant_total(rs.getDouble("montant_total"));
            c.setMode_paiement(rs.getString("mode_paiement"));
            c.setReference_paiement(rs.getString("reference_paiement"));
            list.add(c);
        }
        return list;
    }

    public void supprimer(int id) throws SQLException {
        conn.prepareStatement(
                "DELETE FROM ligne_commande WHERE id_commande=" + id).executeUpdate();
        conn.prepareStatement(
                "DELETE FROM commande WHERE id_commande=" + id).executeUpdate();
    }
}