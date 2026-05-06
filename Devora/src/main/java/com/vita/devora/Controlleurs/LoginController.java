package com.vita.devora.Controlleurs;

import com.vita.devora.Entities.User;
import com.vita.devora.Services.UserService;
import com.vita.devora.utils.EmailSender;
import com.vita.devora.utils.SessionManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.util.prefs.Preferences;

import java.util.Objects;

public class LoginController {

    @FXML
    private TextField UserEmail;

    @FXML
    private PasswordField Password;

    @FXML
    private Label loginMsg;


    @FXML
    private CheckBox rememberMe;
    private Preferences prefs = Preferences.userRoot().node(this.getClass().getName());

    @FXML
    private Button LoginButton;

    private UserService userService = new UserService();

    // =========================
    // 🔐 LOGIN FUNCTION
    // =========================
    @FXML
    public void initialize() {

        String savedEmail = prefs.get("email", "");
        String savedPassword = prefs.get("password", "");

        if (!savedEmail.isEmpty()) {
            UserEmail.setText(savedEmail);
            Password.setText(savedPassword);
            rememberMe.setSelected(true);
        }
    }
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
            if (rememberMe.isSelected()) {
                prefs.put("email", email);
                prefs.put("password", password);
            } else {
                prefs.remove("email");
                prefs.remove("password");
            }
            // Save user in session for other controllers
            SessionManager.setCurrentUser(user);
            loginMsg.setText("✅ Connexion réussie");

            switch (user.getRole()) {

                case ADMIN:
                    openPage("/com/vita/devora/AdminDashbord.fxml");
                    break;

                case DOCTOR:
                    openPage("/com/vita/devora/DoctorDashboard.fxml");
                    break;

                case PATIENT:
                    openPage("/com/vita/devora/PatientPassword.fxml");
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
    private String generatePassword() {
        return "Vita" + (int)(Math.random() * 10000);
    }

        @FXML
        private void forgotPassword() {

            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Mot de passe oublié");
            dialog.setHeaderText("Entrez votre email");

            var result = dialog.showAndWait();

            if (result.isPresent()) {

                String email = result.get();
                User user = userService.findByEmail(email);

                if (user != null) {

                    // 🔐 Générer nouveau mot de passe
                    String newPassword = generatePassword();

                    // 💾 Update DB
                    userService.updatePassword(email, newPassword);

                    // 📧 Envoyer email avec TON service existant
                    EmailSender.envoyerCredentials(
                            email,
                            user.getNom(),
                            user.getRole().toString(),
                            newPassword
                    );

                    loginMsg.setText("✅ Nouveau mot de passe envoyé par email");

                } else {
                    loginMsg.setText("❌ Email introuvable");
                }
            }
        }
    }
