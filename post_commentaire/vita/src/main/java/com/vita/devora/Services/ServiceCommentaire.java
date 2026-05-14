package com.vita.devora.Services;

import com.vita.devora.Entities.CommentDisplay;
import com.vita.devora.Entities.Commentaire;
import com.vita.devora.Interfaces.InterfaceCommentaire;
import com.vita.devora.MyDB.MyBD;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ServiceCommentaire implements InterfaceCommentaire<Commentaire> {

    Connection conn;

    public ServiceCommentaire() {
        conn = MyBD.getInstance().getConnection();
    }

    @Override
    public void AddComment(Commentaire c) throws SQLException {
        String filteredContent = filterBadWords(c.getContenu()); // 👈 filter first
        c.setContenu(filteredContent);
        String sql = "INSERT INTO commentaires (IdPost, IdUser, contenu, statut, DateCreation, DateModification, ParentId) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, c.getIdPost());
            ps.setInt(2, c.getIdUser());
            ps.setString(3, c.getContenu());
            ps.setString(4, c.getStatut().name());
            ps.setTimestamp(5, Timestamp.valueOf(c.getDateCreation()));
            ps.setTimestamp(6, Timestamp.valueOf(c.getDateModification()));

            if (c.getParentId() != null) {
                ps.setInt(7, c.getParentId());
            } else {
                ps.setNull(7, Types.INTEGER);
            }

            ps.executeUpdate();
            System.out.println("Comment added ✅");

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

    }

    @Override
    public void Updatecomment(Commentaire c) throws SQLException {
        String filteredContent = filterBadWords(c.getContenu()); // 👈 filter first
        c.setContenu(filteredContent);
        String sql = "UPDATE commentaires SET contenu=?, statut=?, DateModification=? WHERE IdCommentaire=?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, c.getContenu());
            ps.setString(2, c.getStatut().name());
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(4, c.getIdCommentaire());

            ps.executeUpdate();
            System.out.println("Comment updated ✅");

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

    }

    @Override
    public void DeleteComment(int id) throws SQLException {
        String sql = "DELETE FROM commentaires WHERE IdCommentaire=?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();

            System.out.println("Comment deleted ✅");

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

    }

    @Override
    public List<Commentaire> GetAllComments() throws SQLException {
        List<Commentaire> list = new ArrayList<>();
        String sql = "SELECT * FROM commentaires";

        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                list.add(map(rs));
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return list;
    }

    @Override
    public Commentaire GetCommentById(int id) throws SQLException {
        String sql = "SELECT * FROM commentaires WHERE IdCommentaire=?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return map(rs);
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return null;
    }

    @Override
    public List<Commentaire> getByPost(int postId) {
        List<Commentaire> list = new ArrayList<>();
        // We select everything from 'c' (comments) and specific columns from 'u' (users)
        String sql = "SELECT c.*, u.nom, u.role FROM commentaires c " +
                "JOIN users u ON c.IdUser = u.id " +
                "WHERE c.IdPost=? AND c.ParentId IS NULL";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, postId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Commentaire c = map(rs);
                // We temporarily store the joined data in the object
                c.setUsername(rs.getString("nom"));
                c.setUserRole(rs.getString("role"));
                list.add(c);
            }
        } catch (SQLException e) { System.out.println(e.getMessage()); }
        return list;
    }

//    @Override
//    public List<Commentaire> getReplies(int parentId) {
//        List<Commentaire> list = new ArrayList<>();
//        // We select everything from 'c' (comments) and specific columns from 'u' (users)
//        String sql = "SELECT c.*, u.nom, u.role FROM commentaires c " +
//                "JOIN users u ON c.IdUser = u.id " +
//                "WHERE c.IdPost=? AND c.ParentId=?  ";
//
//        try (PreparedStatement ps = conn.prepareStatement(sql)) {
//            ps.setInt(1, parentId);
//            ResultSet rs = ps.executeQuery();
//            while (rs.next()) {
//                Commentaire c = map(rs);
//                // We temporarily store the joined data in the object
//                c.setUsername(rs.getString("nom"));
//                c.setUserRole(rs.getString("role"));
//                list.add(c);
//            }
//        } catch (SQLException e) { System.out.println(e.getMessage()); }
//        return list;
//    }

    @Override
    public List<Commentaire> getReplies(int parentId) {

        List<Commentaire> list = new ArrayList<>();

        //System.out.println("Replies loaded: " + list.size());
        String sql = """
        SELECT c.*, u.nom, u.role
        FROM commentaires c
        JOIN users u ON c.IdUser = u.id
        WHERE c.ParentId = ?
        ORDER BY c.DateCreation ASC
    """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, parentId);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                Commentaire c = map(rs);

                c.setUsername(rs.getString("nom"));
                c.setUserRole(rs.getString("role"));

                list.add(c);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    @Override
    public List<Commentaire> getAllSortedByLatest() {
        List<Commentaire> list = new ArrayList<>();
        String sql = "SELECT * FROM commentaires ORDER BY datecreation DESC";

        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                list.add(map(rs));
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return list;
    }

    @Override
    public List<Commentaire> getAllSortedByOldest() {
        List<Commentaire> list = new ArrayList<>();
        String sql = "SELECT * FROM commentaires ORDER BY datecreation ASC";

        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                list.add(map(rs));
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return list;
    }

    @Override
    public List<CommentDisplay> getAllCommentDisplays() {
        List<CommentDisplay> list = new ArrayList<>();

        String sql = """
        SELECT 
            c.idcommentaire,
            u.nom AS username,
            p.category,
            c.contenu,
            c.statut,
            c.datecreation
        FROM commentaires c
        JOIN users u ON c.iduser = u.id
        JOIN post p ON c.idpost = p.idpost
        ORDER BY c.datecreation DESC
    """;

        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {

                CommentDisplay dto = new CommentDisplay();

                dto.setIdCommentaire(rs.getInt("idcommentaire"));
                dto.setUsername(rs.getString("username"));
                dto.setCategory(rs.getString("category"));
                dto.setContenu(rs.getString("contenu"));
                dto.setStatut(rs.getString("statut"));
                dto.setDateCreation(rs.getTimestamp("datecreation").toLocalDateTime());

                list.add(dto);
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return list;
    }


    // 🔥 Mapper
    private Commentaire map(ResultSet rs) throws SQLException {
        Commentaire c = new Commentaire();

        // Ensure these strings match your database column names EXACTLY (Case Sensitive)
        c.setIdCommentaire(rs.getInt("IdCommentaire"));
        c.setIdPost(rs.getInt("IdPost"));
        c.setIdUser(rs.getInt("IdUser"));

        // Remove .toUpperCase() to preserve user's original formatting
        c.setContenu(rs.getString("contenu"));

        // Safety check for Enum conversion
        String statutStr = rs.getString("statut");
        if (statutStr != null) {
            c.setStatut(Commentaire.Statut.valueOf(statutStr.toUpperCase().trim()));
        }

        c.setDateCreation(rs.getTimestamp("DateCreation").toLocalDateTime());
        c.setDateModification(rs.getTimestamp("DateModification").toLocalDateTime());

        int parent = rs.getInt("ParentId");
        if (!rs.wasNull()) {
            c.setParentId(parent);
        }

        return c;
    }

    private String filterBadWords(String text) {
        try {
            String encoded = java.net.URLEncoder.encode(text, "UTF-8");
            String urlStr = "https://www.purgomalum.com/service/plain?text=" + encoded + "&add=damn,crap,hell";
            System.out.println("Calling API: " + urlStr); // 👈 debug

            java.net.URL url = new java.net.URL(urlStr);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            System.out.println("Response code: " + responseCode); // 👈 debug

            java.util.Scanner scanner = new java.util.Scanner(connection.getInputStream());
            String result = scanner.useDelimiter("\\A").next();
            scanner.close();

            System.out.println("Filtered result: " + result); // 👈 debug
            return result;

        } catch (Exception e) {
            System.out.println("Filter API error: " + e.getMessage()); // 👈 what error?
            return text;
        }
    }
}