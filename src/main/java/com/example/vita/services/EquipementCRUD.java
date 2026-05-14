package services;

import com.example.vita.Entites.equipements;
import com.example.vita.Interfaces.InterfaceCRUD;
import com.example.vita.utils.MyBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EquipementCRUD implements InterfaceCRUD<equipements> {

    Connection conn;

    public EquipementCRUD() {
        conn = MyBD.getInstance().getConn();
    }

    @Override
    public void ajouter(equipements equipement) throws SQLException {
        String req = "INSERT INTO equipement (nom, description, type, marque, etat, localisation, date_acquisition) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setString(1, equipement.getNom());
        pst.setString(2, equipement.getDescription());
        pst.setString(3, equipement.getType());
        pst.setString(4, equipement.getMarque());
        pst.setString(5, equipement.getEtat());
        pst.setString(6, equipement.getLocalisation());
        pst.setDate(7, equipement.getDate_acquisition());
        pst.executeUpdate();
        System.out.println("Équipement ajouté !!");
    }

    @Override
    public void modifier(equipements equipement) throws SQLException {
        String req = "UPDATE equipement SET nom=?, description=?, type=?, marque=?, " +
                "etat=?, localisation=?, date_acquisition=?, date_modification=NOW() " +
                "WHERE id_equipement=?";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setString(1, equipement.getNom());
        pst.setString(2, equipement.getDescription());
        pst.setString(3, equipement.getType());
        pst.setString(4, equipement.getMarque());
        pst.setString(5, equipement.getEtat());
        pst.setString(6, equipement.getLocalisation());
        pst.setDate(7, equipement.getDate_acquisition());
        pst.setInt(8, equipement.getId_equipement());
        pst.executeUpdate();
        System.out.println("Équipement modifié !!");
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String req = "DELETE FROM equipement WHERE id_equipement=?";
        PreparedStatement pst = conn.prepareStatement(req);
        pst.setInt(1, id);
        pst.executeUpdate();
        System.out.println("Équipement supprimé !!");
    }

    @Override
    public List<equipements> afficher() throws SQLException {
        String req = "SELECT * FROM equipement";
        List<equipements> listEquipements = new ArrayList<>();
        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(req);
        while (rs.next()) {
            equipements e = new equipements();
            e.setId_equipement(rs.getInt("id_equipement"));
            e.setNom(rs.getString("nom"));
            e.setDescription(rs.getString("description"));
            e.setType(rs.getString("type"));
            e.setMarque(rs.getString("marque"));
            e.setEtat(rs.getString("etat"));
            e.setLocalisation(rs.getString("localisation"));
            e.setDate_acquisition(rs.getDate("date_acquisition"));
            listEquipements.add(e);
        }
        return listEquipements;
    }
}