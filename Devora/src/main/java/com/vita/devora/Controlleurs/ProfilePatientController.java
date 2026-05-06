package com.vita.devora.Controlleurs;

import com.vita.devora.Entities.User;
import com.vita.devora.Services.UserService;
import com.vita.devora.utils.SessionManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.io.IOException;

public class ProfilePatientController {

    @FXML private Circle avatarCircle;
    @FXML private Label profileNomLabel;
    @FXML private Label profileEmailLabel;
    @FXML private Label profileTelLabel;
    @FXML private Label profileDepartementLabel;
    @FXML private Label bloodTypelabel;// Used for Blood Type in your FXML

    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label messageLabel;

    private final UserService userService = new UserService();
    private User currentUser;

    /**
     * Call this method from the previous screen to pass the logged-in user
     */
    public void setUserData(User user) {
        this.currentUser = user;
        displayProfile();
    }

    @FXML
    public void initialize() {
        this.currentUser = SessionManager.getCurrentUser();

        if (currentUser != null) {
            displayProfile();
        } else {
            System.err.println("Session error: No user found in SessionManager");
        }

        messageLabel.setText("");
    }

    private void displayProfile() {
        if (currentUser != null) {
            profileNomLabel.setText(currentUser.getNom() + " " + currentUser.getPrenom());
            profileEmailLabel.setText(currentUser.getEmail());
            profileTelLabel.setText(String.valueOf(currentUser.getNumtel()));
            String bloodType = currentUser.getBloodType();
            System.out.println(bloodType);
            bloodTypelabel.setText(bloodType != null ? bloodType : "Non renseigné");           // bloodTypelabel.setText(currentUser.getBloodType());

           // bloodTypelabel.setText(String.valueOf(new Label(currentUser.get() != null ? currentUser.getBloodType() : "Non spécifié"))) ;
            // In your FXML you named the blood type label 'profileDepartementLabel'
            // Ensure your User entity has a getBloodType() method
           // bloodTypelabel.setText(currentUser.getBloodType() != null ? currentUser.getBloodType() : "Non spécifié");
        }
    }

    @FXML
    void handleSavePassword(ActionEvent event) {
        if (currentUser == null) {
            showMessage("Erreur: Session utilisateur introuvable. Veuillez vous reconnecter.", Color.RED);
            return;
        }
        String currentPass = currentPasswordField.getText();
        String newPass = newPasswordField.getText();
        String confirmPass = confirmPasswordField.getText();

        // 1. Basic Validation
        if (currentPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            showMessage("Veuillez remplir tous les champs.", Color.RED);
            return;
        }

        // 2. Check if current password is correct
        if (!currentPass.equals(currentUser.getPassword())) {
            showMessage("Le mot de passe actuel est incorrect.", Color.RED);
            return;
        }

        // 3. Check new password strength
        if (newPass.length() < 8) {
            showMessage("Le nouveau mot de passe doit contenir au moins 8 caractères.", Color.RED);
            return;
        }

        // 4. Check if new passwords match
        if (!newPass.equals(confirmPass)) {
            showMessage("Les nouveaux mots de passe ne correspondent pas.", Color.RED);
            return;
        }

        // 5. Update in Database
        try {
            userService.updatePassword(currentUser.getEmail(), newPass);
            currentUser.setPassword(newPass); // Update local session object

            showMessage("Mot de passe mis à jour avec succès !", Color.GREEN);

            // Clear fields
            currentPasswordField.clear();
            newPasswordField.clear();
            confirmPasswordField.clear();

        } catch (Exception e) {
            showMessage("Erreur lors de la mise à jour : " + e.getMessage(), Color.RED);
            e.printStackTrace();
        }
    }

    private void showMessage(String text, Color color) {
        messageLabel.setText(text);
        messageLabel.setTextFill(color);
    }

    @FXML
    void haddledeconnexion(ActionEvent event) {
        try {
            // Load the Login screen
            Parent loginRoot = FXMLLoader.load(getClass().getResource("/com/vita/devora/LoginTest.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(loginRoot);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}