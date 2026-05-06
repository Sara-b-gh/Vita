package com.vita.devora.Controlleurs;

import com.vita.devora.Entities.User;
import com.vita.devora.Services.UserService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class AddPatientController {

    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    @FXML private TextField telField;
    @FXML private ComboBox<String> bloodTypeCombo;

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
//        if (user.getBloodType() != null) {
//            bloodTypeCombo.setValue(user.getBloodType());
//        }
    }

    @FXML
    private void handleAdd() {
        if (nomField.getText().trim().isEmpty()) {
            showAlert("Champ manquant", "Le nom est obligatoire."); return;
        }
        if (prenomField.getText().trim().isEmpty()) {
            showAlert("Champ manquant", "Le prénom est obligatoire."); return;
        }
        if (emailField.getText().trim().isEmpty() || !emailField.getText().contains("@")) {
            showAlert("Email invalide", "Veuillez saisir un email valide."); return;
        }
        if (bloodTypeCombo.getValue() == null) {
            showAlert("Champ manquant", "Veuillez sélectionner un type sanguin."); return;
        }

        int tel = 0;
        if (!telField.getText().trim().isEmpty()) {
            try {
                tel = Integer.parseInt(telField.getText().trim().replaceAll("\\s+", ""));
            } catch (NumberFormatException e) {
                showAlert("Téléphone invalide", "Le numéro doit être numérique."); return;
            }
        }

        try {
            if (existingUser != null) {
                existingUser.setNom(nomField.getText().trim());
                existingUser.setPrenom(prenomField.getText().trim());
                existingUser.setEmail(emailField.getText().trim());
                existingUser.setNumtel(tel);
//                existingUser.setBloodType(bloodTypeCombo.getValue());
                userService.modifier(existingUser);
            } else {
                User patient = new User();
                patient.setNom(nomField.getText().trim());
                patient.setPrenom(prenomField.getText().trim());
                patient.setEmail(emailField.getText().trim());
                patient.setNumtel(tel);
//                patient.setBloodType(bloodTypeCombo.getValue());
                patient.setRole(User.Roles.PATIENT);
                userService.ajouter(patient);
            }
            closeWindow();
        } catch (Exception e) {
            showAlert("Erreur", "Opération échouée : " + e.getMessage());
            e.printStackTrace();
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
}