package services;

import Entites.RendezVous;
import utils.MyBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceRessource implements IService<RendezVous.Ressource> {

    private Connection cnx;

    public ServiceRessource() {
        cnx = MyBD.getInstance().getCnx();
    }

    @Override
    public void add(RendezVous.Ressource r) throws SQLException {
        String req = "INSERT INTO ressource (id_evenement, nom_Ressource, type, quantite_requise, quantite_disponible, cout_unitaire, responsable, statut, evenement_titre, delai_reapprovisionnement_jours) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, r.getIdEvenement());
        ps.setString(2, r.getNomRessource());
        ps.setString(3, r.getType());
        ps.setInt(4, r.getQuantiteRequise());
        ps.setInt(5, r.getQuantiteDisponible());
        ps.setDouble(6, r.getCoutUnitaire());
        ps.setString(7, r.getResponsable());
        ps.setString(8, r.getStatut());
        ps.setString(9, r.getEvenementTitre());
        ps.setObject(10, r.getDelaiReapprovisionnementJours());
        ps.executeUpdate();
    }

    @Override
    public void update(RendezVous.Ressource r) throws SQLException {
        String req = "UPDATE ressource SET id_evenement=?, nom_Ressource=?, type=?, quantite_requise=?, quantite_disponible=?, cout_unitaire=?, responsable=?, statut=?, evenement_titre=?, delai_reapprovisionnement_jours=? WHERE id_Ressource=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, r.getIdEvenement());
        ps.setString(2, r.getNomRessource());
        ps.setString(3, r.getType());
        ps.setInt(4, r.getQuantiteRequise());
        ps.setInt(5, r.getQuantiteDisponible());
        ps.setDouble(6, r.getCoutUnitaire());
        ps.setString(7, r.getResponsable());
        ps.setString(8, r.getStatut());
        ps.setString(9, r.getEvenementTitre());
        ps.setObject(10, r.getDelaiReapprovisionnementJours());
        ps.setInt(11, r.getIdRessource());
        ps.executeUpdate();
    }

    @Override
    public void delete(RendezVous.Ressource r) throws SQLException {
        String req = "DELETE FROM ressource WHERE id_Ressource=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, r.getIdRessource());
        ps.executeUpdate();
    }

    @Override
    public List<RendezVous.Ressource> getAll() throws SQLException {
        List<RendezVous.Ressource> list = new ArrayList<>();
        String req = "SELECT * FROM ressource";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);
        while (rs.next()) {
            RendezVous.Ressource r = new RendezVous.Ressource();
            r.setIdRessource(rs.getInt("id_Ressource"));      // ← underscore
            r.setIdEvenement(rs.getInt("id_evenement"));      // ← underscore
            r.setNomRessource(rs.getString("nom_Ressource")); // ← underscore
            r.setType(rs.getString("type"));
            r.setQuantiteRequise(rs.getInt("quantite_requise"));
            r.setQuantiteDisponible(rs.getInt("quantite_disponible"));
            r.setCoutUnitaire(rs.getDouble("cout_unitaire"));
            r.setResponsable(rs.getString("responsable"));
            r.setStatut(rs.getString("statut"));
            r.setEvenementTitre(rs.getString("evenement_titre"));
            list.add(r);
        }
        return list;
    }
}