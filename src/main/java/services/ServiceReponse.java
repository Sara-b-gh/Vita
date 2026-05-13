package services;
import entities.Reponse;
import utils.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ServiceReponse implements IService<Reponse>{
    Connection cnx;

    public ServiceReponse() {
        cnx = MyDatabase.getInstance().getCnx();
    }

    @Override
    public void add(Reponse reponse) throws SQLException {
        String req = "INSERT INTO `reponse`(`id_patient`, `id_question`, `reponse`, `date_reponse`) VALUES (?, ?, ?, ?)";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setInt(1, reponse.getIdPatient());
        pst.setInt(2, reponse.getIdQuestion());
        pst.setString(3, reponse.getReponse());
        pst.setTimestamp(4, Timestamp.valueOf(reponse.getDateReponse()));
        pst.executeUpdate();
        System.out.println("Réponse ajoutée");
    }

    @Override
    public void update(Reponse reponse) throws SQLException {
        String req = "UPDATE reponse SET id_patient=?, id_question=?, reponse=?, date_reponse=? WHERE id_reponse=?";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setInt(1, reponse.getIdPatient());
        pst.setInt(2, reponse.getIdQuestion());
        pst.setString(3, reponse.getReponse());
        pst.setTimestamp(4, Timestamp.valueOf(reponse.getDateReponse()));
        pst.setInt(5, reponse.getIdReponse());
        pst.executeUpdate();
        System.out.println("Réponse modifiée");
    }

    @Override
    public void delete(Reponse reponse) throws SQLException {
        String req = "DELETE FROM reponse WHERE id_reponse=?";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setInt(1, reponse.getIdReponse());
        pst.executeUpdate();
        System.out.println("Réponse supprimée");
    }

    @Override
    public List<Reponse> getAll() throws SQLException {
        List<Reponse> reponses = new ArrayList<>();
        String req = "SELECT * FROM reponse";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);
        while (rs.next()) {
            int idReponse = rs.getInt("id_reponse");
            int idPatient = rs.getInt("id_patient");
            int idQuestion = rs.getInt("id_question");
            String reponse = rs.getString("reponse");
            LocalDateTime dateReponse = rs.getTimestamp("date_reponse").toLocalDateTime();
            reponses.add(new Reponse(idReponse, idPatient, idQuestion, reponse, dateReponse));
        }
        return reponses;
    }
}
