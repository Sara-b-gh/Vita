package com.vita.devora.Controlleurs;

import com.vita.devora.Entities.User;
import com.vita.devora.Services.UserService;
import com.vita.devora.utils.EmailSender;
import com.vita.devora.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import javafx.concurrent.Worker;
import javafx.scene.web.WebEngine;
import netscape.javascript.JSObject;

import com.vita.devora.utils.JavaConnector;

import java.util.prefs.Preferences;

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

    @FXML
    private WebView captchaWebView;


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
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");

        WebEngine engine = captchaWebView.getEngine();

        captchaWebView.getEngine().setUserAgent(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/120.0.0.0 Safari/537.36"
        );

        engine.setOnError(event -> {
            System.out.println("❌ WebView Error: " + event.getMessage());
        });

        // ← REMPLACEZ L'ANCIEN LISTENER PAR CELUI-CI
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            System.out.println("WebView State: " + newState);

            if (newState == Worker.State.SUCCEEDED) {
                String url = engine.getLocation();
                System.out.println("URL: " + url);

                // ← INTERCEPTER LA VALIDATION
                if (url.contains("captcha-done")) {
                    JavaConnector.captchaValidated = true;
                    System.out.println("✅ Captcha validé !");
                    engine.load("http://localhost:9090/captcha");
                    return;
                }

                javafx.application.Platform.runLater(() -> {
                    JavaConnector connector = new JavaConnector();
                    JSObject window = (JSObject) engine.executeScript("window");
                    window.setMember("javaConnector", connector);
                    System.out.println("✅ javaConnector injecté !");
                });
            }

            if (newState == Worker.State.FAILED) {
                System.out.println("❌ Chargement échoué !");
                System.out.println(engine.getLoadWorker().getException());
            }
        });
        engine.load("http://localhost:9090/captcha?t=");
    }
    @FXML
    private void login() {
        if (!JavaConnector.captchaValidated) {

            loginMsg.setText("❌ Veuillez valider le captcha");

            return;
        }

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
            JavaConnector.captchaValidated = false;

            switch (user.getRole()) {

                case ADMIN:
                    openPage("/com/vita/devora/AdminDashbord.fxml");
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
                System.out.println("❌ FXML NOT FOUND: " + path);
                return;
            }

            System.out.println("✅ FXML trouvé: " + resource);

            Parent root = FXMLLoader.load(resource);
            Stage stage = (Stage) LoginButton.getScene().getWindow();

            // ← DÉBLOQUER pour les dashboards
            stage.setResizable(true);
            stage.setWidth(1280);
            stage.setHeight(720);
            stage.centerOnScreen();

            stage.getScene().setRoot(root);

        } catch (Exception e) {
            System.out.println("❌ ERREUR: " + e.getMessage());
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
