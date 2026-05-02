package com.vita.devora.Controlleurs;

import com.vita.devora.Entities.User;
import com.vita.devora.Services.UserService;
import com.vita.devora.utils.EmailSender;
import com.vita.devora.utils.PasswordGenerator;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.SQLException;

public class AddUserController {

    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    @FXML private TextField telField;
    @FXML private ChoiceBox<String> roleChoice;

    private final UserService userService = new UserService();
    private com.vita.devora.Entities.User existingUser = null;

    @FXML
    public void initialize() {
        roleChoice.getItems().addAll("DOCTOR", "PATIENT");
        roleChoice.setValue("DOCTOR");
    }

    private User.Roles defaultRole = null;

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    @FXML
    private void handleAdd() {
        String nom = nomField.getText().trim();
        String prenom = prenomField.getText().trim();
        String email = emailField.getText().trim();
        String telText = telField.getText().trim();
        String roleStr = roleChoice.getValue();

        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty()) {
            showAlert("Erreur", "Veuillez remplir au moins le nom, le prénom et l'email.");
            return;
        }

        int numtel = 0;
        try {
            if (!telText.isEmpty()) numtel = Integer.parseInt(telText);
        } catch (NumberFormatException e) {
            showAlert("Erreur", "Téléphone invalide.");
            return;
        }

        // generate password
        String generated = PasswordGenerator.generer(8);

        User u = new User();
        u.setNom(nom);
        u.setPrenom(prenom);
        u.setEmail(email);
        u.setPassword(generated);
        u.setNumtel(numtel);
        u.setRole("DOCTOR".equals(roleStr) ? User.Roles.DOCTOR : User.Roles.PATIENT);
        try {
            if (existingUser != null) {
                // editing: preserve id and password if not changed
                u.setId(existingUser.getId());
                if (nom.isEmpty()) u.setNom(existingUser.getNom());
                // keep original password
                u.setPassword(existingUser.getPassword());
                userService.modifier(u);
            } else {
                userService.ajouter(u);
            }
            // send email
            if (existingUser == null) {
                try {
                    EmailSender.envoyerCredentials(email, nom + " " + prenom, roleStr, generated);
                } catch (Exception ex) {
                    // log but keep going
                    System.out.println("Envoi email échoué: " + ex.getMessage());
                }
            }
            // Notify success
            Alert ok = new Alert(Alert.AlertType.INFORMATION, "Utilisateur ajouté avec succès.", ButtonType.OK);
            ok.showAndWait();
            // Close and let caller refresh
            closeWindow();
        } catch (Exception ex) {
            showAlert("Erreur", "Impossible d'ajouter l'utilisateur: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Put the dialog into edit mode for the given user.
     */
    public void setUser(User user) {
        if (user == null) return;
        this.existingUser = user;
        nomField.setText(user.getNom());
        prenomField.setText(user.getPrenom());
        emailField.setText(user.getEmail());
        telField.setText(String.valueOf(user.getNumtel()));
        roleChoice.setValue(user.getRole() == User.Roles.DOCTOR ? "DOCTOR" : "PATIENT");
    }

    /**
     * Set the default role shown in the dialog (for quick add from parent).
     */
    public void setDefaultRole(User.Roles role) {
        this.defaultRole = role;
        if (roleChoice != null && role != null) {
            roleChoice.setValue(role == User.Roles.DOCTOR ? "DOCTOR" : "PATIENT");
            // lock role selection for dialog opened from a specific tab
            roleChoice.setDisable(true);
            // optionally hide role choice to avoid confusion
            roleChoice.setVisible(false);
        }
    }

    private void showAlert(String title, String content) {
        Alert a = new Alert(Alert.AlertType.ERROR, content, ButtonType.OK);
        a.setTitle(title);
        a.showAndWait();
    }

    private void closeWindow() {
        Stage s = (Stage) nomField.getScene().getWindow();
        s.close();
    }
}
