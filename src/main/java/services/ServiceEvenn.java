package services;

import Entites.RendezVous;
import utils.MyBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceEvenn implements IService<RendezVous.Evenn> {

    private Connection cnx;

    public ServiceEvenn() {
        cnx = MyBD.getInstance().getCnx();
    }

    @Override
    public void add(RendezVous.Evenn e) throws SQLException {
        String req = "INSERT INTO evenn (titre, date_evenement, description, lieu) VALUES (?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, e.getTitre());
        ps.setTimestamp(2, Timestamp.valueOf(e.getDateEvenement()));
        ps.setString(3, e.getDescription());
        ps.setString(4, e.getLieu());
        ps.executeUpdate();
    }

    @Override
    public void update(RendezVous.Evenn e) throws SQLException {
        String req = "UPDATE evenn SET titre=?, date_evenement=?, description=?, lieu=? WHERE id_Evenn=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, e.getTitre());
        ps.setTimestamp(2, Timestamp.valueOf(e.getDateEvenement()));
        ps.setString(3, e.getDescription());
        ps.setString(4, e.getLieu());
        ps.setInt(5, e.getId_Evenn());
        ps.executeUpdate();
    }

    @Override
    public void delete(RendezVous.Evenn e) throws SQLException {
        String req = "DELETE FROM evenn WHERE id_Evenn=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, e.getId_Evenn());
        ps.executeUpdate();
    }

    @Override
    public List<RendezVous.Evenn> getAll() throws SQLException {
        List<RendezVous.Evenn> list = new ArrayList<>();
        String req = "SELECT * FROM evenn";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);
        while (rs.next()) {
            RendezVous.Evenn e = new RendezVous.Evenn();
            e.setId_Evenn(rs.getInt("id_Evenn"));
            e.setTitre(rs.getString("titre"));
            e.setDateEvenement(rs.getTimestamp("date_evenement").toLocalDateTime());
            e.setDescription(rs.getString("description"));
            e.setLieu(rs.getString("lieu"));
            list.add(e);
        }
        return list;
    }

    public RendezVous.Evenn getById(int id) throws SQLException {
        String req = "SELECT * FROM evenn WHERE id_Evenn = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            RendezVous.Evenn e = new RendezVous.Evenn();
            e.setId_Evenn(rs.getInt("id_Evenn"));
            e.setTitre(rs.getString("titre"));
            e.setDateEvenement(rs.getTimestamp("date_evenement").toLocalDateTime());
            e.setDescription(rs.getString("description"));
            e.setLieu(rs.getString("lieu"));
            return e;
        }
        return null;
    }
}