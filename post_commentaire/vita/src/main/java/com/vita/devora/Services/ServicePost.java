package com.vita.devora.Services;

import com.vita.devora.Entities.Post;
import com.vita.devora.Entities.PostView;
import com.vita.devora.Interfaces.InterfacePost;
import com.vita.devora.MyDB.MyBD;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ServicePost implements InterfacePost<Post> {
    Connection conn;
    public ServicePost() {
        conn = MyBD.getInstance().getConnection();
    }

    @Override
    public void AddPost(Post p) throws SQLException {
        String sql = "INSERT INTO post (IdUser, Category, contenu, DateCreation, DateModification, Statu, NbCommentaire) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, p.getIdUser());
            ps.setString(2, p.getCategory().name());
            ps.setString(3, p.getContenu());
            ps.setTimestamp(4, Timestamp.valueOf(p.getDateCreation()));
            ps.setTimestamp(5, Timestamp.valueOf(p.getDateModification()));
            ps.setString(6, p.getStatut().name());
            ps.setInt(7, p.getNbCommentaire());

            ps.executeUpdate();
            System.out.println("Post added ✅");

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void UpdatePost(Post p) throws SQLException {
        String sql = "UPDATE post SET Category=?, contenu=?, DateModification=?, statu=?, NbCommentaire=? WHERE IdPost=?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, p.getCategory().name());
            ps.setString(2, p.getContenu());
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(4, p.getStatut().name());
            ps.setInt(5, p.getNbCommentaire());
            ps.setInt(6, p.getIdPost());

            ps.executeUpdate();
            System.out.println("Post updated ✅");

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

    }

    @Override
    public void DeletePost(int id) throws SQLException {
        String sql = "DELETE FROM post WHERE IdPost = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();

            System.out.println("Post deleted ✅");

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

    }

    @Override
    public List<Post> GetAllPost() throws SQLException {
        List<Post> posts = new ArrayList<>();
        String sql = "SELECT * FROM post";

        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                posts.add(mapResultSetToPost(rs));
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return posts;
    }

    @Override
    public Post GetPostById(int id) throws SQLException {
        String sql = "SELECT * FROM post WHERE IdPost = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapResultSetToPost(rs);
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return null;
    }

    @Override
    public List<PostView> getAllPostsSortedByDateDesc() {
        List<PostView> posts = new ArrayList<>();
        String sql = """
        SELECT p.idPost,p.idUser, u.nom, u.role, p.category, p.contenu, 
               p.statu, p.NbCommentaire, p.dateCreation, p.dateModification,
               (SELECT COUNT(*) FROM commentaires c WHERE c.idPost = p.idPost) AS totalComments
        FROM post p
        LEFT JOIN users u ON p.idUser = u.id
        ORDER BY p.dateCreation DESC
    """;

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                posts.add(mapResultSetToPostView(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error in getAllPosts: " + e.getMessage());
        }
        return posts;
    }

    @Override
    public List<PostView> filterByCategory(Post.Category category) {
        List<PostView> posts = new ArrayList<>();
        // Added u.role and switched to LEFT JOIN for consistency
        String sql = """
        SELECT p.idPost, p.idUser, u.nom, u.role, p.category, p.contenu, 
               p.statu, p.NbCommentaire, p.dateCreation, p.dateModification,
               (SELECT COUNT(*) FROM commentaires c WHERE c.idPost = p.idPost) AS totalComments
        FROM post p
        LEFT JOIN users u ON p.idUser = u.id
        WHERE p.category = ?
        ORDER BY p.dateCreation DESC
    """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, category.name()); // category.name() is cleaner than String.valueOf()

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    posts.add(mapResultSetToPostView(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error in filterByCategory: " + e.getMessage());
        }
        return posts;
    }


    private Post mapResultSetToPost(ResultSet rs) throws SQLException {
        Post p = new Post();

        p.setIdPost(rs.getInt("idpost"));
        p.setIdUser(rs.getInt("iduser"));
        p.setCategory(Post.Category.valueOf(rs.getString("category").toUpperCase()));
        p.setContenu(rs.getString("contenu"));
        p.setDateCreation(rs.getTimestamp("datecreation").toLocalDateTime());
        p.setDateModification(rs.getTimestamp("datemodification").toLocalDateTime());
        p.setStatut(Post.Statut.valueOf(rs.getString("statu").toUpperCase()));
        p.setNbCommentaire(rs.getInt("nbcommentaire"));

        return p;
    }

    private PostView mapResultSetToPostView(ResultSet rs) throws SQLException {
        PostView pv = new PostView();
        // Be careful with case: matching lowercase to match the SQL query
        pv.setNbCommentaire(rs.getInt("totalComments"));
        pv.setIdPost(rs.getInt("idPost"));
        pv.setIdUser(rs.getInt("IdUser"));
        pv.setUsername(rs.getString("nom"));
        pv.setCategory(rs.getString("category"));
        pv.setContenu(rs.getString("contenu"));
        pv.setStatut(rs.getString("statu"));
        pv.setDateCreation(rs.getTimestamp("dateCreation").toLocalDateTime());
        pv.setDateModification(rs.getTimestamp("dateModification").toLocalDateTime());

        // FETCH THE ROLE
        pv.setUserRole(rs.getString("role"));

        return pv;
    }
}