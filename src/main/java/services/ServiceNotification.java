package services;

import entities.LocalDateTime;
import utils.MyBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceNotification implements IService<LocalDateTime.Notification> {

    private Connection cnx;

    public ServiceNotification() {
        cnx = MyBD.getInstance().getCnx();
    }

    @Override
    public void add(LocalDateTime.Notification n) throws SQLException {
        String req = "INSERT INTO notification (type, titre, message, is_read) VALUES (?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, n.getType());
        ps.setString(2, n.getTitre());
        ps.setString(3, n.getMessage());
        ps.setInt(4, n.getIsRead());
        ps.executeUpdate();
    }

    @Override
    public void update(LocalDateTime.Notification n) throws SQLException {
        String req = "UPDATE notification SET type=?, titre=?, message=?, is_read=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setString(1, n.getType());
        ps.setString(2, n.getTitre());
        ps.setString(3, n.getMessage());
        ps.setInt(4, n.getIsRead());
        ps.setInt(5, n.getId());
        ps.executeUpdate();
    }

    @Override
    public void delete(LocalDateTime.Notification n) throws SQLException {
        String req = "DELETE FROM notification WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, n.getId());
        ps.executeUpdate();
    }

    @Override
    public List<LocalDateTime.Notification> getAll() throws SQLException {
        List<LocalDateTime.Notification> list = new ArrayList<>();
        String req = "SELECT * FROM notification ORDER BY date_creation DESC";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);
        while (rs.next()) {
            LocalDateTime.Notification n = new LocalDateTime.Notification();
            n.setId(rs.getInt("id"));
            n.setType(rs.getString("type"));
            n.setTitre(rs.getString("titre"));
            n.setMessage(rs.getString("message"));
            n.setDateCreation(rs.getTimestamp("date_creation").toLocalDateTime());
            n.setIsRead(rs.getInt("is_read"));
            list.add(n);
        }
        return list;
    }

    public void markAsRead(int id) throws SQLException {
        String req = "UPDATE notification SET is_read = 1 WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    public void markAllAsRead() throws SQLException {
        String req = "UPDATE notification SET is_read = 1";
        Statement st = cnx.createStatement();
        st.executeUpdate(req);
    }
}