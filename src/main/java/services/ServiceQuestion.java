package services;
import entities.Question;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceQuestion implements IService<Question> {
    Connection cnx;

    public ServiceQuestion() {
        cnx = MyDatabase.getInstance().getCnx();
    }

    @Override
    public void add(Question question) throws SQLException {
        String req = "INSERT INTO `question`(`question`, `titre_quiz`, `texte`) VALUES (?, ?, ?)";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setString(1, question.getQuestion());
        pst.setString(2, question.getTitreQuiz());
        pst.setString(3, question.getTexte());
        pst.executeUpdate();
        System.out.println("Question ajoutée");
    }

    @Override
    public void update(Question question) throws SQLException {
        String req = "UPDATE question SET question=?, titre_quiz=?, texte=? WHERE id=?";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setString(1, question.getQuestion());
        pst.setString(2, question.getTitreQuiz());
        pst.setString(3, question.getTexte());
        pst.setInt(4, question.getId());
        pst.executeUpdate();
        System.out.println("Question modifiée");
    }

    @Override
    public void delete(Question question) throws SQLException {
        String req = "DELETE FROM question WHERE id=?";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setInt(1, question.getId());
        pst.executeUpdate();
        System.out.println("Question supprimée");
    }

    @Override
    public List<Question> getAll() throws SQLException {
        List<Question> questions = new ArrayList<>();
        String req = "SELECT * FROM question";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);
        while (rs.next()) {
            int id = rs.getInt("id");
            String question = rs.getString("question");
            String titreQuiz = rs.getString("titre_quiz");
            String texte = rs.getString("texte");
            questions.add(new Question(id, question, titreQuiz, texte));
        }
        return questions;
    }
}
