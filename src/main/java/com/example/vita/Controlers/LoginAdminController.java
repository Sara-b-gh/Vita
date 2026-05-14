package Controlers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;

import java.util.Objects;

public class LoginAdminController {

    @FXML private PasswordField passwordField;
    @FXML private Label         errorLabel;

    private static final String MOT_DE_PASSE = "admin123";

    @FXML
    public void seConnecter() {
        if (passwordField.getText().equals(MOT_DE_PASSE)) {
            naviguer("/Commande-view.fxml");
        } else {
            errorLabel.setText("Mot de passe incorrect.");
        }
    }

    @FXML
    public void annuler() {
        naviguer("/Main-view.fxml");
    }

    private void naviguer(String fxml) {
        try {
            Parent root = FXMLLoader.load(
                    Objects.requireNonNull(getClass().getResource(fxml)));
            Stage stage = (Stage) passwordField.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            errorLabel.setText("Erreur : " + e.getMessage());
        }
    }
}