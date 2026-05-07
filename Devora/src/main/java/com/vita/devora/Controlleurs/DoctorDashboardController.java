package com.vita.devora.Controlleurs;

import com.vita.devora.Entities.User;
import com.vita.devora.Services.UserService;
import com.vita.devora.utils.SessionManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.List;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;

public class DoctorDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Label totalPatientsLabel;
    @FXML private javafx.scene.control.Button dashBtn;
    @FXML private javafx.scene.control.Button mesPatientsBtn;
    @FXML private FlowPane patientCardsPane;

    private final UserService userService = new UserService();
    private List<User> patientList;

    @FXML
    public void initialize() {
        // show logged-in doctor name
        if (SessionManager.getCurrentUser() != null) {
            welcomeLabel.setText("Bienvenue Dr. " + SessionManager.getCurrentUser().getNom());
        }

        loadMyPatients();
        if (dashBtn != null) dashBtn.setOnAction(e -> {
            loadMyPatients();
            dashBtn.setStyle("-fx-background-color: #FF4757; -fx-text-fill: white;");
            if (mesPatientsBtn != null) mesPatientsBtn.setStyle("-fx-background-color: transparent;");
        });
        if (mesPatientsBtn != null) mesPatientsBtn.setOnAction(e -> {
            loadMyPatients();
            mesPatientsBtn.setStyle("-fx-background-color: #FF4757; -fx-text-fill: white;");
            if (dashBtn != null) dashBtn.setStyle("-fx-background-color: transparent;");
        });
    }

    private void loadMyPatients() {
        User doctor = SessionManager.getCurrentUser();
        if (doctor == null) return;
        try {
            patientList = userService.getPatientsByDoctor(doctor.getId());
            totalPatientsLabel.setText(String.valueOf(patientList.size()));
            buildCards(patientList);
        } catch (SQLException e) {
            totalPatientsLabel.setText("0");
            e.printStackTrace();
        }
    }

    private void buildCards(List<User> patients) {
        patientCardsPane.getChildren().clear();
        for (User u : patients) {
            patientCardsPane.getChildren().add(createCard(u));
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

        // Groupe sanguin
        Label bloodType = new Label(
                u.getBloodType() != null ? u.getBloodType() : "Non assigné"
        );
        bloodType.setStyle("""
            -fx-background-color: #ffe4e1;
            -fx-text-fill: #d32f2f;
            -fx-background-radius: 12;
            -fx-padding: 2 10;
            -fx-font-size: 11;
        """);

        VBox info = new VBox(4, name, bloodType);
        HBox header = new HBox(10, avatarBox, info);

        // Email
        Label email = new Label("✉ " + u.getEmail());

        // Tel
        Label tel = new Label("☎ " + u.getNumtel());

        VBox body = new VBox(6, email, tel);

        VBox card = new VBox(10, header, body);

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
    private void handleProfile(ActionEvent actionEvent) {
       switchPage(actionEvent,"/com/vita/devora/DocteurPassword.fxml");

    }

    @FXML
    private void handleLogout() {
        try {
            SessionManager.clearSession();
            Parent root = FXMLLoader.load(getClass().getResource("/com/vita/devora/LoginTest.fxml"));
            Stage stage = (Stage) patientCardsPane.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
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
}
