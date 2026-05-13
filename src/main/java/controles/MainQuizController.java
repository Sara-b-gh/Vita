package controles;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import utils.JsonImporter;
import utils.MyDatabase;
import utils.OpenTDBImporter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

public class MainQuizController {

    @FXML private StackPane contentArea;
    @FXML private Label lblTitre;
    @FXML private Button btnOngletQuestions;
    @FXML private Button btnOngletReponses;

    @FXML
    public void initialize() {
        showFormulaire(null);
        setOngletActif(btnOngletQuestions);
    }

    @FXML
    public void showFormulaire(ActionEvent event) {
        try {
            Node node = FXMLLoader.load(getClass().getResource("/AjouterQuiz.fxml"));
            contentArea.getChildren().setAll(node);
            lblTitre.setText("Ajouter une Question");
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    public void showListe(ActionEvent event) {
        try {
            Node node = FXMLLoader.load(getClass().getResource("/ListeQuiz.fxml"));
            contentArea.getChildren().setAll(node);
            lblTitre.setText("Liste des Questions");
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    public void showOngletQuestions(ActionEvent event) {
        setOngletActif(btnOngletQuestions);
        showFormulaire(null);
    }

    @FXML
    public void showOngletReponses(ActionEvent event) {
        setOngletActif(btnOngletReponses);
        try {
            Node node = FXMLLoader.load(getClass().getResource("/ReponsesView.fxml"));
            contentArea.getChildren().setAll(node);
            lblTitre.setText("Réponses");
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ✅ IMPORTER depuis JSON (français)
    @FXML
    public void importerQuestionsJson(ActionEvent event) {
        lblTitre.setText("⏳ Importation FR en cours...");

        new Thread(() -> {
            List<String[]> questions = JsonImporter.importerQuestions();

            Platform.runLater(() -> {
                if (questions.isEmpty()) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Erreur");
                    alert.setHeaderText(null);
                    alert.setContentText("❌ Fichier questions.json introuvable !");
                    alert.showAndWait();
                    lblTitre.setText("Gestion des Quiz");
                    return;
                }
                insererQuestions(questions, "FR");
            });
        }).start();
    }

    // ✅ IMPORTER depuis OpenTDB (anglais santé/moral)
    @FXML
    public void importerQuestionsOpenTDB(ActionEvent event) {
        lblTitre.setText("⏳ Importation EN en cours...");

        new Thread(() -> {
            List<String[]> questions = OpenTDBImporter.importerQuestions();

            Platform.runLater(() -> {
                if (questions.isEmpty()) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Erreur");
                    alert.setHeaderText(null);
                    alert.setContentText(
                            "❌ Aucune question trouvée.\n" +
                                    "Vérifiez votre connexion internet.");
                    alert.showAndWait();
                    lblTitre.setText("Gestion des Quiz");
                    return;
                }
                insererQuestions(questions, "EN");
            });
        }).start();
    }

    // ✅ Méthode commune d'insertion
    private void insererQuestions(List<String[]> questions, String langue) {
        try {
            Connection conn = MyDatabase.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO question(question,reponseA,reponseB," +
                            "reponseC,reponseD,bonneReponse) VALUES(?,?,?,?,?,?)");

            int count = 0;
            for (String[] q : questions) {
                ps.setString(1, q[0]);
                ps.setString(2, q[1]);
                ps.setString(3, q[2]);
                ps.setString(4, q[3]);
                ps.setString(5, q[4]);
                ps.setString(6, q[5]);
                ps.addBatch();
                count++;
            }
            ps.executeBatch();

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Succès");
            alert.setHeaderText(null);
            alert.setContentText("✅ " + count +
                    " questions " + langue + " importées avec succès !");
            alert.showAndWait();

            showListe(null);
            lblTitre.setText("Liste des Questions");

        } catch (Exception e) {
            e.printStackTrace();
            lblTitre.setText("Gestion des Quiz");
        }
    }

    private void setOngletActif(Button actif) {
        String actifStyle = "-fx-background-color: transparent; -fx-font-size: 14px;" +
                "-fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 18 20;" +
                "-fx-text-fill: #6D1B3A;" +
                "-fx-border-color: #6D1B3A transparent transparent transparent;" +
                "-fx-border-width: 3 0 0 0;";
        String inactifStyle = "-fx-background-color: transparent; -fx-font-size: 14px;" +
                "-fx-cursor: hand; -fx-padding: 18 20; -fx-text-fill: #888;" +
                "-fx-border-color: transparent; -fx-border-width: 3 0 0 0;";
        btnOngletQuestions.setStyle(actif == btnOngletQuestions ? actifStyle : inactifStyle);
        btnOngletReponses.setStyle(actif == btnOngletReponses ? actifStyle : inactifStyle);
    }

    @FXML
    public void handleDeconnexion(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/LoginView.fxml"));
            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(false);
            stage.show();
        } catch (Exception e) { e.printStackTrace(); }
    }
}