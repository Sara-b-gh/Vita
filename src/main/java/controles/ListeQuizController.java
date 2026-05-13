package controles;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class ListeQuizController {

    @FXML private VBox cardsContainer;
    @FXML private TextField txtRecherche;

    private ObservableList<Question> list = FXCollections.observableArrayList();
    Connection conn;

    @FXML
    public void initialize() {
        conn = MyDatabase.getInstance().getConnection();
        loadData();
    }

    @FXML
    public void loadData() {
        list.clear();
        cardsContainer.getChildren().clear();
        try {
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM question");
            int numero = 1;
            while (rs.next()) {
                Question q = new Question(
                        rs.getInt("id"),
                        rs.getString("question"),
                        rs.getString("reponseA"),
                        rs.getString("reponseB"),
                        rs.getString("reponseC"),
                        rs.getString("reponseD"),
                        rs.getString("bonneReponse")
                );
                list.add(q);
                cardsContainer.getChildren().add(createCard(q, numero++));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private VBox createCard(Question q, int numero) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(24));
        card.setStyle("-fx-background-color: white;" +
                "-fx-background-radius: 10;" +
                "-fx-border-color: #e0e0e0;" +
                "-fx-border-radius: 10;" +
                "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.06),6,0,0,2);");

        Label lblNum = new Label("Question " + numero);
        lblNum.setStyle("-fx-font-size: 12px; -fx-text-fill: #AD1457; -fx-font-weight: bold;");

        Label lblQuestion = new Label(q.getQuestion());
        lblQuestion.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #333;");
        lblQuestion.setWrapText(true);

        card.getChildren().addAll(lblNum, lblQuestion);

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #f0f0f0;");
        card.getChildren().add(sep);

        String[] reponses = {q.getReponseA(), q.getReponseB(), q.getReponseC(), q.getReponseD()};
        String[] lettres  = {"A", "B", "C", "D"};

        for (int i = 0; i < 4; i++) {
            boolean estBonne = lettres[i].equals(q.getBonneReponse());

            HBox row = new HBox(12);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            row.setPadding(new Insets(6, 12, 6, 12));

            if (estBonne) {
                row.setStyle("-fx-background-color: #e8f5e9; -fx-background-radius: 8;");
            }

            Label cercle = new Label(lettres[i]);
            cercle.setStyle(estBonne
                    ? "-fx-background-color: #2E7D32; -fx-text-fill: white;" +
                    "-fx-font-weight: bold; -fx-min-width: 28; -fx-min-height: 28;" +
                    "-fx-alignment: center; -fx-background-radius: 50; -fx-font-size: 13px;"
                    : "-fx-background-color: #e0e0e0; -fx-text-fill: #555;" +
                    "-fx-font-weight: bold; -fx-min-width: 28; -fx-min-height: 28;" +
                    "-fx-alignment: center; -fx-background-radius: 50; -fx-font-size: 13px;");

            Label lblRep = new Label(reponses[i]);
            lblRep.setStyle(estBonne
                    ? "-fx-font-size: 14px; -fx-text-fill: #2E7D32; -fx-font-weight: bold;"
                    : "-fx-font-size: 14px; -fx-text-fill: #444;");
            lblRep.setWrapText(true);

            if (estBonne) {
                Label check = new Label("✔ Bonne réponse");
                check.setStyle("-fx-font-size: 11px; -fx-text-fill: #2E7D32;");
                HBox.setHgrow(lblRep, Priority.ALWAYS);
                row.getChildren().addAll(cercle, lblRep, check);
            } else {
                row.getChildren().addAll(cercle, lblRep);
            }

            card.getChildren().add(row);
        }

        // Boutons
        HBox btnBox = new HBox(10);
        btnBox.setPadding(new Insets(10, 0, 0, 0));

        Button btnModifier = new Button("✎ Modifier");
        btnModifier.setStyle("-fx-background-color: #1565C0; -fx-text-fill: white;" +
                "-fx-background-radius: 6; -fx-cursor: hand;" +
                "-fx-font-size: 12px; -fx-padding: 6 14;");
        btnModifier.setOnAction(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/AjouterQuiz.fxml"));
                Node formulaire = loader.load();
                AjouterQuiz controller = loader.getController();
                controller.remplirChamps(
                        q.getId(), q.getQuestion(),
                        q.getReponseA(), q.getReponseB(),
                        q.getReponseC(), q.getReponseD(),
                        q.getBonneReponse()
                );
                StackPane contentArea = (StackPane) cardsContainer
                        .getScene().lookup("#contentArea");
                contentArea.getChildren().setAll(formulaire);
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        Button btnSupprimer = new Button("✖ Supprimer");
        btnSupprimer.setStyle("-fx-background-color: #C62828; -fx-text-fill: white;" +
                "-fx-background-radius: 6; -fx-cursor: hand;" +
                "-fx-font-size: 12px; -fx-padding: 6 14;");
        btnSupprimer.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirmation");
            confirm.setContentText("Supprimer cette question ?");
            if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                try {
                    // ✅ 1. Supprimer d'abord les réponses liées
                    PreparedStatement ps1 = conn.prepareStatement(
                            "DELETE FROM reponses_utilisateur WHERE question_id = ?");
                    ps1.setInt(1, q.getId());
                    ps1.executeUpdate();

                    // ✅ 2. Ensuite supprimer la question
                    PreparedStatement ps2 = conn.prepareStatement(
                            "DELETE FROM question WHERE id = ?");
                    ps2.setInt(1, q.getId());
                    ps2.executeUpdate();

                    loadData();
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        });

        btnBox.getChildren().addAll(btnModifier, btnSupprimer);
        card.getChildren().add(btnBox);

        return card;
    }
}