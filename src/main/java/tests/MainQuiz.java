package tests;
import entities.Question;
import entities.Reponse;
import services.ServiceQuestion;
import services.ServiceReponse;

import java.sql.SQLException;
import java.time.LocalDateTime;


public class MainQuiz {
    public static void main(String[] args) {

        ServiceQuestion serviceQuestion = new ServiceQuestion();
        ServiceReponse serviceReponse = new ServiceReponse();

        // --- TEST Question ---
        Question q1 = new Question("Avez-vous mal à la tête ?", "Quiz Neurologie", "Décrivez votre douleur");
        Question q2 = new Question(1, "Avez-vous de la fièvre ?", "Quiz Général", "Température corporelle");

        try {
            // Ajouter une question
            serviceQuestion.add(q1);

            // Modifier une question
            // serviceQuestion.update(q2);

            // Supprimer une question
            // serviceQuestion.delete(q2);

            // Afficher toutes les questions
            System.out.println(serviceQuestion.getAll());

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        // --- TEST Reponse ---
        Reponse r1 = new Reponse(1, 1, "Oui, souvent", LocalDateTime.now());
        Reponse r2 = new Reponse(1, 1, 1, "Non, rarement", LocalDateTime.now());

        try {
            // Ajouter une réponse
            serviceReponse.add(r1);

            // Modifier une réponse
            // serviceReponse.update(r2);

            // Supprimer une réponse
            // serviceReponse.delete(r2);

            // Afficher toutes les réponses
            System.out.println(serviceReponse.getAll());

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}
