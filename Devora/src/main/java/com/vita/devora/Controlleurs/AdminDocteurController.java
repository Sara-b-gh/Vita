package com.vita.devora.Controlleurs;

import com.vita.devora.Entities.User;
import com.vita.devora.Services.UserService;
import com.vita.devora.utils.SessionManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.List;

public class AdminDocteurController {

    @FXML private FlowPane doctorCardsPane;
    @FXML private TextField searchField;
    @FXML private Label totalDoctorsLabel;

    private final UserService userService = new UserService();
    private List<User> userList;

    public AdminDocteurController() throws SQLException {
    }

    @FXML
    public void initialize() {
        loadData();
        searchField.textProperty().addListener((obs, oldV, newV) ->
                filterCards(newV)
        );
    }

    private void loadData() {
        try {
            userList = userService.getDoctors();
            totalDoctorsLabel.setText(String.valueOf(userList.size()));
            buildCards(userList);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void filterCards(String text) {
        if (text == null || text.isEmpty()) {
            buildCards(userList);
            return;
        }
        String q = text.toLowerCase();
        List<User> filtered = userList.stream()
                .filter(u ->
                        u.getNom().toLowerCase().contains(q) ||
                                u.getPrenom().toLowerCase().contains(q)
                )
                .toList();
        buildCards(filtered);
    }

    private void buildCards(List<User> users) {
        doctorCardsPane.getChildren().clear();
        for (User u : users) {
            doctorCardsPane.getChildren().add(createCard(u));
        }
    }

    @FXML
    private void handleAddDoctor() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/vita/devora/AjouterDocteur.fxml"));
            Stage stage = new Stage();
            stage.setTitle("Ajouter un docteur");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
            loadData();
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Impossible d'ouvrir le formulaire d'ajout.").showAndWait();
        }
    }

    private VBox createCard(User u) {

        // Avatar
        Label avatar = new Label(
                (u.getPrenom().charAt(0) + "" + u.getNom().charAt(0)).toUpperCase()
        );
        avatar.setStyle("""
            -fx-background-color: #fdedf3;
            -fx-text-fill: #8a0037;
            -fx-font-weight: bold;
            -fx-font-size: 16;
        """);

        StackPane avatarBox = new StackPane(avatar);
        avatarBox.setMinSize(50, 50);
        avatarBox.setStyle("-fx-background-radius: 25; -fx-background-color: #fdedf3;");

        // Nom
        Label name = new Label(u.getPrenom() + " " + u.getNom());
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");

        // Département
        Label dept = new Label(
                u.getDepartement() != null ? u.getDepartement() : "Non assigné"
        );
        dept.setStyle("""
            -fx-background-color: #eaf7ff;
            -fx-text-fill: #0077b6;
            -fx-background-radius: 12;
            -fx-padding: 2 10;
            -fx-font-size: 11;
        """);

        VBox info = new VBox(4, name, dept);
        HBox header = new HBox(10, avatarBox, info);

        // Email
        Label email = new Label("✉ " + u.getEmail());

        // Tel
        Label tel = new Label("☎ " + u.getNumtel());

        VBox body = new VBox(6, email, tel);

        // ✅ CHANGEMENT 1 — Bouton Modifier (nouveau)
        Button edit = new Button("Modifier");
        edit.setStyle("""
            -fx-background-color: #2ecc71;
            -fx-text-fill: white;
            -fx-background-radius: 8;
            -fx-font-size: 12;
            -fx-cursor: hand;
        """);
        edit.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(edit, Priority.ALWAYS);
        edit.setOnAction(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/com/vita/devora/AjouterDocteur.fxml")
                );
                Parent root = loader.load();
                // ── pré-remplir le formulaire avec les données du docteur ──
                AddUserController controller = loader.getController();
                controller.setUser(u);
                Stage stage = new Stage();
                stage.setTitle("Modifier un docteur");
                stage.setScene(new Scene(root));
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.showAndWait();
                loadData(); // rafraîchir après modification
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        // ✅ CHANGEMENT 2 — Style ajouté sur Supprimer
        Button delete = new Button("Supprimer");
        delete.setStyle("""
            -fx-background-color: #e74c3c;
            -fx-text-fill: white;
            -fx-background-radius: 8;
            -fx-font-size: 12;
            -fx-cursor: hand;
        """);
        delete.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(delete, Priority.ALWAYS);
        delete.setOnAction(e -> {
            try {
                userService.supprimer(u.getId());
                loadData();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });

        // ✅ CHANGEMENT 3 — HBox actions remplace le bouton delete seul
        HBox actions = new HBox(8, edit, delete);

        // ✅ CHANGEMENT 4 — "actions" remplace "delete" dans le VBox
        VBox card = new VBox(10, header, body, actions);

        card.setStyle("""
            -fx-background-color: white;
            -fx-padding: 15;
            -fx-border-radius: 10;
            -fx-background-radius: 10;
            -fx-border-color: #e0e0e0;
        """);

        card.setPrefWidth(240);
        card.setMaxWidth(240);

        return card;
    }

    @FXML
    private void haddledeconnexion() {
        try {
            // Vider la session
            SessionManager.clearSession();

            Parent root = FXMLLoader.load(
                    getClass().getResource("/com/vita/devora/LoginTest.fxml")
            );
            Stage stage = (Stage) doctorCardsPane.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Connexion");
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Impossible de se déconnecter.").showAndWait();
        }
    }


    private void switchPage(ActionEvent event, String fxmlPath) {
        try {
            // 1. Check if resource exists before loading to avoid generic IOExceptions
            java.net.URL resource = getClass().getResource(fxmlPath);
            if (resource == null) {
                System.err.println("❌ FXML File not found at: " + fxmlPath);
                return;
            }

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(resource);
            javafx.scene.Parent root = loader.load();

            // 2. Get the Stage safely
            javafx.scene.Node sourceNode = (javafx.scene.Node) event.getSource();
            javafx.stage.Stage stage = (javafx.stage.Stage) sourceNode.getScene().getWindow();

            // 3. Set the new root
            stage.getScene().setRoot(root);

            // Optional: If you want to ensure the window adjusts to the new size
            // stage.sizeToScene();

        } catch (java.io.IOException e) {
            System.err.println("❌ Critical error loading: " + fxmlPath);
            e.printStackTrace();
        }
    }

    public void toDash(ActionEvent actionEvent) {
        switchPage(actionEvent,"/com/vita/devora/AdminDashbord.fxml");
    }

    public void toPatientDash(ActionEvent actionEvent) {
        switchPage(actionEvent,"/com/vita/devora/AdminPatient.fxml");
    }
}