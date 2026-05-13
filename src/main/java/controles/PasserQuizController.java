package controles;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import utils.EmailSender;
import utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class PasserQuizController {

    @FXML private Label lblProgression;
    @FXML private ProgressBar progressBar;
    @FXML private Label lblQuestion;
    @FXML private VBox reponsesBox;
    @FXML private VBox questionCard;
    @FXML private VBox resultatCard;
    @FXML private VBox detailsBox;
    @FXML private Label lblScore;
    @FXML private Label lblMessage;
    @FXML private Button btnPrecedent;
    @FXML private Button btnSuivant;

    private List<Question> questions = new ArrayList<>();
    private int indexActuel = 0;
    private String[] reponsesUtilisateur;
    private ToggleGroup toggleGroup;
    private int userId;
    private String userEmail;  // ← email utilisateur
    private String username;   // ← nom utilisateur
    Connection conn;

    // ── Appelé depuis LoginController ───────────────────────────
    public void setUserInfo(int id, String email, String username) {
        this.userId   = id;
        this.userEmail = email;
        this.username  = username;
    }

    @FXML
    public void initialize() {
        conn = MyDatabase.getInstance().getConnection();
        chargerQuestions();
    }

    private void chargerQuestions() {
        questions.clear();
        try {
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM question");
            while (rs.next()) {
                questions.add(new Question(
                        rs.getInt("id"),
                        rs.getString("question"),
                        rs.getString("reponseA"),
                        rs.getString("reponseB"),
                        rs.getString("reponseC"),
                        rs.getString("reponseD"),
                        rs.getString("bonneReponse")
                ));
            }
        } catch (Exception e) { e.printStackTrace(); }

        if (!questions.isEmpty()) {
            reponsesUtilisateur = new String[questions.size()];
            afficherQuestion(0);
        }
    }

    private void afficherQuestion(int index) {
        Question q = questions.get(index);

        lblProgression.setText("Question " + (index + 1) + " sur " + questions.size());
        progressBar.setProgress((double)(index + 1) / questions.size());
        lblQuestion.setText(q.getQuestion());

        reponsesBox.getChildren().clear();
        toggleGroup = new ToggleGroup();

        String[] reponses = {q.getReponseA(), q.getReponseB(), q.getReponseC(), q.getReponseD()};
        String[] lettres  = {"A", "B", "C", "D"};

        for (int i = 0; i < 4; i++) {
            final String lettre = lettres[i];
            final String texte  = reponses[i];

            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(10, 16, 10, 16));

            boolean selectionne = reponsesUtilisateur[index] != null &&
                    reponsesUtilisateur[index].equals(lettre);

            row.setStyle(selectionne
                    ? "-fx-background-color: #fce4ec; -fx-background-radius: 8;" +
                    "-fx-border-color: #AD1457; -fx-border-radius: 8; -fx-cursor: hand;"
                    : "-fx-background-color: #f9f9f9; -fx-background-radius: 8;" +
                    "-fx-border-color: #e0e0e0; -fx-border-radius: 8; -fx-cursor: hand;");

            Label cercle = new Label(lettre);
            cercle.setStyle(selectionne
                    ? "-fx-background-color: #AD1457; -fx-text-fill: white;" +
                    "-fx-font-weight: bold; -fx-min-width: 30; -fx-min-height: 30;" +
                    "-fx-alignment: center; -fx-background-radius: 50; -fx-font-size: 13px;"
                    : "-fx-background-color: #e0e0e0; -fx-text-fill: #555;" +
                    "-fx-font-weight: bold; -fx-min-width: 30; -fx-min-height: 30;" +
                    "-fx-alignment: center; -fx-background-radius: 50; -fx-font-size: 13px;");

            RadioButton rb = new RadioButton(texte);
            rb.setToggleGroup(toggleGroup);
            rb.setStyle("-fx-font-size: 14px; -fx-text-fill: #333;");
            rb.setUserData(lettre);
            HBox.setHgrow(rb, Priority.ALWAYS);

            if (selectionne) rb.setSelected(true);

            row.setOnMouseClicked(e -> {
                rb.setSelected(true);
                reponsesUtilisateur[index] = lettre;
                afficherQuestion(index);
            });

            row.getChildren().addAll(cercle, rb);
            reponsesBox.getChildren().add(row);
        }

        btnPrecedent.setDisable(index == 0);
        btnSuivant.setText(index == questions.size() - 1 ? "✔ Terminer" : "Suivant ▶");
    }

    @FXML
    public void suivant() {
        if (toggleGroup.getSelectedToggle() != null) {
            reponsesUtilisateur[indexActuel] =
                    (String) toggleGroup.getSelectedToggle().getUserData();
        }
        if (indexActuel == questions.size() - 1) {
            sauvegarderEnBase();
            afficherResultat();
        } else {
            indexActuel++;
            afficherQuestion(indexActuel);
        }
    }

    @FXML
    public void precedent() {
        if (indexActuel > 0) {
            indexActuel--;
            afficherQuestion(indexActuel);
        }
    }

    private void sauvegarderEnBase() {
        try {
            String sql = "INSERT INTO reponses_utilisateur " +
                    "(user_id, question_id, reponse_choisie, est_correcte) VALUES (?,?,?,?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            for (int i = 0; i < questions.size(); i++) {
                String rep   = reponsesUtilisateur[i];
                if (rep == null) rep = "";
                String bonne = questions.get(i).getBonneReponse();
                ps.setInt(1, userId);
                ps.setInt(2, questions.get(i).getId());
                ps.setString(3, rep);
                ps.setInt(4, bonne.equals(rep) ? 1 : 0);
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void afficherResultat() {
        questionCard.setVisible(false);
        questionCard.setManaged(false);
        btnPrecedent.setVisible(false);
        btnSuivant.setVisible(false);
        resultatCard.setVisible(true);
        resultatCard.setManaged(true);

        // Calcul score
        int score = 0;
        for (int i = 0; i < questions.size(); i++) {
            if (questions.get(i).getBonneReponse().equals(reponsesUtilisateur[i]))
                score++;
        }

        lblScore.setText("🏆 Score : " + score + " / " + questions.size());

        double pct = (double) score / questions.size();
        if (pct == 1.0)
            lblMessage.setText("🎉 Parfait ! Vous avez tout bon !");
        else if (pct >= 0.7)
            lblMessage.setText("👍 Très bien ! Continuez comme ça !");
        else if (pct >= 0.5)
            lblMessage.setText("😊 Pas mal ! Vous pouvez faire mieux !");
        else
            lblMessage.setText("💪 Courage ! Réessayez pour améliorer votre score !");

        // Détails réponses
        detailsBox.getChildren().clear();
        StringBuilder details = new StringBuilder();

        for (int i = 0; i < questions.size(); i++) {
            boolean correct = questions.get(i).getBonneReponse()
                    .equals(reponsesUtilisateur[i]);
            String repUser = reponsesUtilisateur[i] != null
                    ? reponsesUtilisateur[i] : "Non répondu";

            // UI
            HBox row = new HBox(10);
            row.setPadding(new Insets(10, 16, 10, 16));
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle(correct
                    ? "-fx-background-color: #e8f5e9; -fx-background-radius: 8;"
                    : "-fx-background-color: #ffebee; -fx-background-radius: 8;");

            Label icon = new Label(correct ? "✔" : "✖");
            icon.setStyle(correct
                    ? "-fx-text-fill: #2E7D32; -fx-font-size: 16px; -fx-font-weight: bold;"
                    : "-fx-text-fill: #C62828; -fx-font-size: 16px; -fx-font-weight: bold;");

            VBox info = new VBox(4);
            Label qLabel = new Label("Q" + (i+1) + ": " + questions.get(i).getQuestion());
            qLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #333;");
            qLabel.setWrapText(true);

            Label repLabel = new Label("Votre réponse : " + repUser +
                    " | Bonne réponse : " + questions.get(i).getBonneReponse());
            repLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

            info.getChildren().addAll(qLabel, repLabel);
            HBox.setHgrow(info, Priority.ALWAYS);
            row.getChildren().addAll(icon, info);
            detailsBox.getChildren().add(row);

            // Pour email
            details.append(correct ? "✔ " : "✖ ")
                    .append("Q").append(i + 1).append(": ")
                    .append(questions.get(i).getQuestion()).append("\n")
                    .append("   Votre réponse : ").append(repUser)
                    .append(" | Bonne : ")
                    .append(questions.get(i).getBonneReponse())
                    .append("\n\n");
        }

        // ── Envoyer email dans un thread séparé ─────────────────
        if (userEmail != null && !userEmail.isEmpty()) {
            final int finalScore   = score;
            final int finalTotal   = questions.size();
            final String finalDet  = details.toString();
            new Thread(() ->
                    EmailSender.envoyerScore(userEmail, username, finalScore, finalTotal, finalDet)
            ).start();
        }
    }

    @FXML
    public void recommencer() {
        indexActuel = 0;
        reponsesUtilisateur = new String[questions.size()];
        questionCard.setVisible(true);
        questionCard.setManaged(true);
        btnPrecedent.setVisible(true);
        btnSuivant.setVisible(true);
        resultatCard.setVisible(false);
        resultatCard.setManaged(false);
        afficherQuestion(0);
    }

    @FXML
    public void handleDeconnexion() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/LoginView.fxml"));
            Stage stage = (Stage) lblQuestion.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(false);
            stage.show();
        } catch (Exception e) { e.printStackTrace(); }
    }
}