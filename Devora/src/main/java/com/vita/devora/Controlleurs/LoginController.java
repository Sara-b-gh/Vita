package com.vita.devora.Controlleurs;

import com.vita.devora.Entities.User;
import com.vita.devora.Services.UserService;
import com.vita.devora.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.Objects;

public class LoginController {

    @FXML
    private TextField UserEmail;

    @FXML
    private PasswordField Password;

    @FXML
    private Label loginMsg;

    @FXML
    private Button LoginButton;

    private UserService userService = new UserService();

    // =========================
    // 🔐 LOGIN FUNCTION
    // =========================
    @FXML
    private void login() {

        String email = UserEmail.getText();
        String password = Password.getText();

        if (email.isEmpty() || password.isEmpty()) {
            loginMsg.setText("❌ Veuillez remplir tous les champs");
            return;
        }

        User user = userService.authentifier(email, password);

        if (user != null) {
            // Save user in session for other controllers
            SessionManager.setCurrentUser(user);
            loginMsg.setText("✅ Connexion réussie");

            switch (user.getRole()) {

                case ADMIN:
                    openPage("/com/vita/devora/AdminDashboard.fxml");
                    break;

                case DOCTOR:
                    openPage("/com/vita/devora/DoctorDashboard.fxml");
                    break;

                case PATIENT:
                    openPage("/com/vita/devora/PatientDashboard.fxml");
                    break;
            }

        } else {
            loginMsg.setText("❌ Email ou mot de passe incorrect");
        }
    }

    // =========================
    // 🔁 PAGE SWITCH
    // =========================

    private void openPage(String path) {
        try {
            System.out.println("Loading: " + path);

            var resource = getClass().getResource(path);

            if (resource == null) {
                System.out.println("❌ FXML NOT FOUND!");
                return;
            }

            Parent root = FXMLLoader.load(resource);

            Stage stage = (Stage) LoginButton.getScene().getWindow();
            stage.getScene().setRoot(root);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}