package services;

import entities.ReservationPersonne;
import utils.MyBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class ServiceReservationPersonne implements IService<ReservationPersonne> {
    private Connection cnx;

    public ServiceReservationPersonne() {
        cnx = MyBD.getInstance().getCnx();
    }

    @Override
    public void add(ReservationPersonne r) throws SQLException {
        String req = "INSERT INTO reservation_personne (id_evenement, nom_complet, email, telephone, date_reservation, statut, commentaire) VALUES (?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS);
        ps.setInt(1, r.getIdEvenement());
        ps.setString(2, r.getNomComplet());
        ps.setString(3, r.getEmail());
        ps.setString(4, r.getTelephone());
        ps.setTimestamp(5, Timestamp.valueOf(r.getDateReservation()));
        ps.setString(6, r.getStatut());
        ps.setString(7, r.getCommentaire());
        ps.executeUpdate();

        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            r.setId(rs.getInt(1));
        }
    }

    @Override
    public void update(ReservationPersonne r) throws SQLException {
        String req = "UPDATE reservation_personne SET id_evenement=?, nom_complet=?, email=?, telephone=?, date_reservation=?, statut=?, commentaire=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, r.getIdEvenement());
        ps.setString(2, r.getNomComplet());
        ps.setString(3, r.getEmail());
        ps.setString(4, r.getTelephone());
        ps.setTimestamp(5, Timestamp.valueOf(r.getDateReservation()));
        ps.setString(6, r.getStatut());
        ps.setString(7, r.getCommentaire());
        ps.setInt(8, r.getId());
        ps.executeUpdate();
    }

    @Override
    public void delete(ReservationPersonne r) throws SQLException {
        String req = "DELETE FROM reservation_personne WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, r.getId());
        ps.executeUpdate();
    }

    @Override
    public List<ReservationPersonne> getAll() throws SQLException {
        List<ReservationPersonne> list = new ArrayList<>();
        String req = "SELECT * FROM reservation_personne ORDER BY date_reservation DESC";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);
        while (rs.next()) {
            ReservationPersonne r = new ReservationPersonne();
            r.setId(rs.getInt("id"));
            r.setIdEvenement(rs.getInt("id_evenement"));
            r.setNomComplet(rs.getString("nom_complet"));
            r.setEmail(rs.getString("email"));
            r.setTelephone(rs.getString("telephone"));
            r.setDateReservation(rs.getTimestamp("date_reservation").toLocalDateTime());
            r.setStatut(rs.getString("statut"));
            r.setCommentaire(rs.getString("commentaire"));
            list.add(r);
        }
        return list;
    }

    public List<ReservationPersonne> getByEvenement(int idEvenement) throws SQLException {
        List<ReservationPersonne> list = new ArrayList<>();
        String req = "SELECT * FROM reservation_personne WHERE id_evenement=? ORDER BY date_reservation DESC";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, idEvenement);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            ReservationPersonne r = new ReservationPersonne();
            r.setId(rs.getInt("id"));
            r.setIdEvenement(rs.getInt("id_evenement"));
            r.setNomComplet(rs.getString("nom_complet"));
            r.setEmail(rs.getString("email"));
            r.setTelephone(rs.getString("telephone"));
            r.setDateReservation(rs.getTimestamp("date_reservation").toLocalDateTime());
            r.setStatut(rs.getString("statut"));
            r.setCommentaire(rs.getString("commentaire"));
            list.add(r);
        }
        return list;
    }

    public void updateStatut(int id, String statut) throws SQLException {
        String req = "UPDATE reservation_personne SET statut=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, statut);
        ps.setInt(2, id);
        ps.executeUpdate();
    }
    // Ajoutez ces méthodes
    public ReservationPersonne getById(int id) throws SQLException {
        String req = "SELECT * FROM reservation_personne WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            ReservationPersonne r = new ReservationPersonne();
            r.setId(rs.getInt("id"));
            r.setIdEvenement(rs.getInt("id_evenement"));
            r.setNomComplet(rs.getString("nom_complet"));
            r.setEmail(rs.getString("email"));
            r.setTelephone(rs.getString("telephone"));
            r.setDateReservation(rs.getTimestamp("date_reservation").toLocalDateTime());
            r.setStatut(rs.getString("statut"));
            r.setCommentaire(rs.getString("commentaire"));
            try {
                r.setPresent(rs.getBoolean("present"));
            } catch (SQLException ignored) {}
            return r;
        }
        return null;
    }

    public void updatePresence(int id, boolean present) throws SQLException {
        String req = "UPDATE reservation_personne SET present=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setBoolean(1, present);
        ps.setInt(2, id);
        ps.executeUpdate();
    }
    public List<ReservationPersonne> getByEmail(String email) throws SQLException {
        List<ReservationPersonne> list = new ArrayList<>();
        String req = "SELECT * FROM reservation_personne WHERE email=? ORDER BY date_reservation DESC";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, email);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            ReservationPersonne r = new ReservationPersonne();
            r.setId(rs.getInt("id"));
            r.setIdEvenement(rs.getInt("id_evenement"));
            r.setNomComplet(rs.getString("nom_complet"));
            r.setEmail(rs.getString("email"));
            r.setTelephone(rs.getString("telephone"));
            r.setDateReservation(rs.getTimestamp("date_reservation").toLocalDateTime());
            r.setStatut(rs.getString("statut"));
            r.setCommentaire(rs.getString("commentaire"));
            list.add(r);
        }
        return list;
    }

}