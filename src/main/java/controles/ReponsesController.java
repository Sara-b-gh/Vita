package controles;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import utils.MyDatabase;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class ReponsesController {

    @FXML private Label lblTotalReponses;
    @FXML private Label lblScoreMoyen;
    @FXML private Label lblParticipants;
    @FXML private VBox  boxQuestions;

    @FXML
    public void initialize() {
        chargerStats();
    }

    private void chargerStats() {
        try {
            Connection conn = MyDatabase.getInstance().getConnection();
            Statement st = conn.createStatement();

            // Stats globales
            ResultSet rs = st.executeQuery(
                    "SELECT COUNT(*) AS total, " +
                            "SUM(est_correcte) AS correctes, " +
                            "COUNT(DISTINCT user_id) AS participants " +
                            "FROM reponses_utilisateur");
            if (rs.next()) {
                int total   = rs.getInt("total");
                int correct = rs.getInt("correctes");
                int parts   = rs.getInt("participants");
                lblTotalReponses.setText(String.valueOf(total));
                lblParticipants.setText(String.valueOf(parts));
                lblScoreMoyen.setText(total > 0
                        ? (int)(correct * 100.0 / total) + "%" : "0%");
            }

            // Stats par question
            ResultSet rq = st.executeQuery(
                    "SELECT q.id, q.question, q.bonneReponse, " +
                            "SUM(CASE WHEN ru.reponse_choisie='A' THEN 1 ELSE 0 END) AS cntA, " +
                            "SUM(CASE WHEN ru.reponse_choisie='B' THEN 1 ELSE 0 END) AS cntB, " +
                            "SUM(CASE WHEN ru.reponse_choisie='C' THEN 1 ELSE 0 END) AS cntC, " +
                            "SUM(CASE WHEN ru.reponse_choisie='D' THEN 1 ELSE 0 END) AS cntD, " +
                            "COUNT(ru.id) AS totalRep " +
                            "FROM question q " +
                            "LEFT JOIN reponses_utilisateur ru ON q.id = ru.question_id " +
                            "GROUP BY q.id, q.question, q.bonneReponse " +
                            "ORDER BY q.id");

            boxQuestions.getChildren().clear();
            int num = 1;
            while (rq.next()) {
                boxQuestions.getChildren().add(creerCarteQuestion(
                        num++,
                        rq.getString("question"),
                        rq.getString("bonneReponse"),
                        rq.getInt("cntA"),
                        rq.getInt("cntB"),
                        rq.getInt("cntC"),
                        rq.getInt("cntD"),
                        rq.getInt("totalRep")));
            }

        } catch (Exception e) { e.printStackTrace(); }
    }

    private VBox creerCarteQuestion(int num, String question, String bonne,
                                    int cntA, int cntB, int cntC, int cntD,
                                    int total) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8;" +
                "-fx-border-color: #e0e0e0; -fx-border-radius: 8;" +
                "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.08),6,0,0,2);");
        card.setPadding(new Insets(20));

        Label titre = new Label("Question " + num + " : " + question);
        titre.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #202124;");
        titre.setWrapText(true);

        Label totalLabel = new Label(total + " réponse(s)");
        totalLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #888;");

        card.getChildren().addAll(titre, totalLabel);

        String[] labels = {"A", "B", "C", "D"};
        int[] counts = {cntA, cntB, cntC, cntD};

        for (int i = 0; i < 4; i++) {
            double pct = total > 0 ? (double) counts[i] / total : 0;
            boolean estBonne = labels[i].equals(bonne);

            HBox ligne = new HBox(10);
            ligne.setAlignment(Pos.CENTER_LEFT);

            Label lbl = new Label(labels[i]);
            lbl.setPrefWidth(20);
            lbl.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;" +
                    (estBonne ? "-fx-text-fill: #2e7d32;" : "-fx-text-fill: #555;"));

            ProgressBar bar = new ProgressBar(pct);
            bar.setPrefWidth(300);
            bar.setPrefHeight(22);
            bar.setStyle(estBonne ? "-fx-accent: #2e7d32;" : "-fx-accent: #AD1457;");

            Label pctLabel = new Label((int)(pct * 100) + "% (" + counts[i] + ")");
            pctLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;");

            ligne.getChildren().addAll(lbl, bar, pctLabel);

            if (estBonne) {
                Label check = new Label("✓ Bonne réponse");
                check.setStyle("-fx-text-fill: #2e7d32; -fx-font-size: 11px; -fx-font-weight: bold;");
                ligne.getChildren().add(check);
            }

            card.getChildren().add(ligne);
        }
        return card;
    }
}
