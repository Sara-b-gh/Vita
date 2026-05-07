package com.vita.devora.Controlleurs;

import com.vita.devora.Entities.User;
import com.vita.devora.Services.UserService;
import com.vita.devora.utils.EmailSender;
import com.vita.devora.utils.PasswordGenerator;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class AddPatientController {

    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    @FXML private TextField telField;
    @FXML private ComboBox<String> bloodTypeCombo;
    @FXML private TextField cinField;
    @FXML private DatePicker dobPicker;

    private final UserService userService = new UserService();
    private User existingUser = null;

    @FXML
    public void initialize() {
        bloodTypeCombo.getItems().addAll(
                "A+", "A−", "B+", "B−", "AB+", "AB−", "O+", "O−"
        );
    }

    public void setUser(User user) {
        this.existingUser = user;
        nomField.setText(user.getNom());
        prenomField.setText(user.getPrenom());
        emailField.setText(user.getEmail());
        telField.setText(user.getNumtel() != 0
                ? String.valueOf(user.getNumtel()) : "");
       if (user.getBloodType() != null) {
           bloodTypeCombo.setValue(user.getBloodType());
        }
        if(user.getDateNaissance() != null) {
            dobPicker.setValue(user.getDateNaissance());
        }
        else {
            dobPicker.setValue(null); // Clear it if no date exists
        }
        cinField.setText(String.valueOf(user.getId()));
    }

    @FXML
    private void handleAdd() {
        String nom = nomField.getText().trim();
        String prenom = prenomField.getText().trim();
        String email = emailField.getText().trim();
        String telText = telField.getText().trim();
        String bloodType = bloodTypeCombo.getValue();
        String cinText = cinField.getText().trim();
        java.time.LocalDate dob = dobPicker.getValue();

        // ───── VALIDATION ─────
        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || telText.isEmpty() || bloodType == null) {
            showAlert("Erreur", "Tous les champs sont obligatoires !");
            return;
        }

        if (!User.isValidEmail(email)) {
            showAlert("Erreur", "Email invalide !");
            return;
        }

        int numtel;
        try {
            numtel = Integer.parseInt(telText);
        } catch (Exception e) {
            showAlert("Erreur", "Numéro de téléphone invalide !");
            return;
        }

        try {
            int cin = Integer.parseInt(cinText);

            User u;

            // ───── ADD ─────
            if (existingUser == null) {

                String password = PasswordGenerator.generer(8);

                u = new User();
                u.setId(cin);
                u.setNom(nom);
                u.setPrenom(prenom);
                u.setEmail(email);
                u.setPassword(password);
                u.setNumtel(numtel);
                u.setRole(User.Roles.PATIENT);
                u.setBloodType(bloodType);
                u.setDateNaissance(dob);

                userService.ajouter(u);

                try {
                    EmailSender.envoyerCredentials(email, nom + " " + prenom, "PATIENT", password);
                } catch (Exception ex) {
                    System.out.println("Email error: " + ex.getMessage());
                }

                showAlertInfo("Succès", "Patient ajouté avec succès");
            }

            // ───── EDIT ─────
            else {

                u = existingUser;
                u.setId(cin);
                u.setNom(nom);
                u.setPrenom(prenom);
                u.setEmail(email);
                u.setNumtel(numtel);
                u.setBloodType(bloodType);
                if (u.getRole() == null) {
                    u.setRole(User.Roles.PATIENT);
                }
                u.setDateNaissance(dob);

                userService.modifier(u);

                showAlertInfo("Succès", "Utilisateur modifié avec succès");
            }

            closeWindow();

        } catch (Exception e) {
            showAlert("Erreur", e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) nomField.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    private void showAlertInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setContentText(msg);
        a.showAndWait();
    }
}