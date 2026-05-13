package com.example.vita.services;

import com.example.vita.Entites.medicaments;
import com.example.vita.Interfaces.InterfaceCRUD;
import com.example.vita.utils.MyBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MedicamentCRUD implements InterfaceCRUD<medicaments> {

    Connection conn;

    public MedicamentCRUD() {
        conn = MyBD.getInstance().getConn();
    }

    @Override
    public void ajouter(medicaments medicament) throws SQLException {
        String req = "INSERT INTO medicament (nom, description, dosage, forme, prix, stock, statut) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setString(1, medicament.getNom());
        pst.setString(2, medicament.getDescription());
        pst.setString(3, medicament.getDosage());
        pst.setString(4, medicament.getForme());
        pst.setDouble(5, medicament.getPrix());
        pst.setInt(6, medicament.getStock());
        pst.setString(7, medicament.getStatut());
        pst.executeUpdate();
        System.out.println("Médicament ajouté !!");
    }

    @Override
    public void modifier(medicaments medicament) throws SQLException {
        String req = "UPDATE medicament SET nom=?, description=?, dosage=?, forme=?, " +
                "prix=?, stock=?, statut=?, date_modification=NOW() " +
                "WHERE id_medicament=?";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setString(1, medicament.getNom());
        pst.setString(2, medicament.getDescription());
        pst.setString(3, medicament.getDosage());
        pst.setString(4, medicament.getForme());
        pst.setDouble(5, medicament.getPrix());
        pst.setInt(6, medicament.getStock());
        pst.setString(7, medicament.getStatut());
        pst.setInt(8, medicament.getId_medicament());
        pst.executeUpdate();
        System.out.println("Médicament modifié !!");
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String req = "DELETE FROM medicament WHERE id_medicament=?";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, id);
        pst.executeUpdate();
        System.out.println("Médicament supprimé !!");
    }

    @Override
    public List<medicaments> afficher() throws SQLException {
        String req = "SELECT * FROM medicament";
        List<medicaments> listMedicaments = new ArrayList<>();
        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(req);
        while (rs.next()) {
            medicaments m = new medicaments();   // corrigé : medicamentsn et mesdicaments → medicaments
            m.setId_medicament(rs.getInt("id_medicament"));
            m.setNom(rs.getString("nom"));
            m.setDescription(rs.getString("description"));
            m.setDosage(rs.getString("dosage"));
            m.setForme(rs.getString("forme"));
            m.setPrix(rs.getDouble("prix"));
            m.setStock(rs.getInt("stock"));
            m.setStatut(rs.getString("statut"));
            listMedicaments.add(m);
        }
        return listMedicaments;
    }
}