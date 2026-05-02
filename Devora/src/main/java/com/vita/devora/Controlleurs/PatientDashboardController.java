package com.vita.devora.Controlleurs;

import com.vita.devora.Entities.User;
import com.vita.devora.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;

public class PatientDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Label nameLabel;
    @FXML private Label emailLabel;
    @FXML private Label telLabel;

    @FXML
    public void initialize() {
        User current = SessionManager.getCurrentUser();
        if (current != null) {
            welcomeLabel.setText("Bienvenue " + current.getPrenom());
            nameLabel.setText(current.getNom() + " " + current.getPrenom());
            emailLabel.setText(current.getEmail());
            telLabel.setText(String.valueOf(current.getNumtel()));
            // populate profile duplicates
            profileNameLabel.setText(current.getNom() + " " + current.getPrenom());
            profileEmailLabel.setText(current.getEmail());
            profileRoleLabel.setText(current.getRole().toString());
        }
    }

    @FXML private Label profileNameLabel;
    @FXML private Label profileEmailLabel;
    @FXML private Label profileRoleLabel;
    @FXML private javafx.scene.layout.VBox profileBox;

    @FXML
    private void handleProfile() {
        if (profileBox == null) return;
        String cur = profileBox.getStyle();
        if (cur != null && cur.contains("-fx-border-color")) {
            profileBox.setStyle("");
        } else {
            profileBox.setStyle("-fx-border-color: #FF4757; -fx-border-width: 2;");
        }
    }

    @FXML
    private void handleLogout() {
        try {
            SessionManager.clearSession();
            Parent root = FXMLLoader.load(getClass().getResource("/com/vita/devora/LoginTest.fxml"));
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
