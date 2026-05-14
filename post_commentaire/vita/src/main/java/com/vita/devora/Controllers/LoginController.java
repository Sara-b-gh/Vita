package com.vita.devora.Controllers;

import com.vita.devora.Entities.User;
import com.vita.devora.Services.ServiceUser;
import com.vita.devora.Utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private final ServiceUser serviceUser = new ServiceUser();

    @FXML
    private void handleLogin() {
        String email = emailField.getText();
        String pass = passwordField.getText();

        User user = serviceUser.login(email, pass);

        if (user != null) {
            SessionManager.setCurrentUser(user);
            redirectBasedOnRole(user);
        } else {
            errorLabel.setText("Identifiants invalides !");
        }
    }

    private void redirectBasedOnRole(User user) {
        String fxmlFile = "";

        switch (user.getRole()) {
            case DOCTOR:
                // Ensure these paths match your actual folder structure in src/main/resources
                fxmlFile = "/com/vita/devora/DoctorCommunity.fxml";
                break;
            case ADMIN:
                fxmlFile = "/com/vita/devora/Communityview.fxml";
                break;
            case PATIENT:
                fxmlFile = "/com/vita/devora/DoctorCommunity.fxml";
                break;
        }

        try {
            // Use this safer way to check if the file exists before loading
            URL resource = getClass().getResource(fxmlFile);
            if (resource == null) {
                throw new IOException("FXML file not found at: " + fxmlFile);
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
            stage.show();
        } catch (IOException e) {
            errorLabel.setText("Erreur: Page introuvable.");
            e.printStackTrace();
        }
    }
}