package com.vita.devora.Controlleurs;

import com.vita.devora.Entities.User;
import com.vita.devora.Services.UserService;
import com.vita.devora.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class ProfileController {

    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    @FXML private TextField telField;
    @FXML private Label roleLabel;

    private final UserService userService = new UserService();

    @FXML
    public void initialize() {
        loadProfile();
    }

    private void loadProfile() {
        User u = SessionManager.getCurrentUser();
        if (u == null) return;
        nomField.setText(u.getNom());
        prenomField.setText(u.getPrenom());
        emailField.setText(u.getEmail());
        telField.setText(String.valueOf(u.getNumtel()));
        roleLabel.setText(u.getRole().toString());
    }

    @FXML
    private void handleCancel() {
        // reload to discard changes
        loadProfile();
    }

    @FXML
    private void handleUpdate() {
        User u = SessionManager.getCurrentUser();
        if (u == null) return;

        String nom = nomField.getText().trim();
        String prenom = prenomField.getText().trim();
        String email = emailField.getText().trim();
        String telText = telField.getText().trim();

        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty()) {
            showAlert("Erreur", "Nom, prénom et email sont requis.");
            return;
        }

        int numtel = 0;
        try { if (!telText.isEmpty()) numtel = Integer.parseInt(telText); } catch (NumberFormatException e) { showAlert("Erreur", "Téléphone invalide."); return; }

        u.setNom(nom);
        u.setPrenom(prenom);
        u.setEmail(email);
        u.setNumtel(numtel);

        try {
            userService.modifier(u);
            // update session
            SessionManager.setCurrentUser(u);
            showAlert("Succès", "Profil mis à jour.");
        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert("Erreur", "Impossible de mettre à jour le profil: " + ex.getMessage());
        }
    }

    private void showAlert(String title, String content) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, content);
        a.setTitle(title);
        a.showAndWait();
    }
}
