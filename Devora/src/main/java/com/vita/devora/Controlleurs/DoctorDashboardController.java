package com.vita.devora.Controlleurs;

import com.vita.devora.Entities.User;
import com.vita.devora.Services.UserService;
import com.vita.devora.utils.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.SQLException;
import java.util.List;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;

public class DoctorDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Label totalPatientsLabel;
    @FXML private Label titleLabel;
    @FXML private javafx.scene.control.Button dashBtn;
    @FXML private javafx.scene.control.Button mesPatientsBtn;
    @FXML private TableView<User> patientTable;
    @FXML private TableColumn<User, String> pColNom;
    @FXML private TableColumn<User, String> pColPrenom;
    @FXML private TableColumn<User, String> pColEmail;
    @FXML private TableColumn<User, Integer> pColTel;

    private final UserService userService = new UserService();
    private ObservableList<User> patientList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // show logged-in doctor name
        if (SessionManager.getCurrentUser() != null) {
            welcomeLabel.setText("Bienvenue Dr. " + SessionManager.getCurrentUser().getNom());
        }

        setupColumns();
        loadPatients();
//        populateProfile();
        if (dashBtn != null) dashBtn.setOnAction(e -> {
            loadPatients();
            dashBtn.setStyle("-fx-background-color: #FF4757; -fx-text-fill: white;");
            if (mesPatientsBtn != null) mesPatientsBtn.setStyle("-fx-background-color: transparent;");
        });
        if (mesPatientsBtn != null) mesPatientsBtn.setOnAction(e -> {
            loadMyPatients();
            mesPatientsBtn.setStyle("-fx-background-color: #FF4757; -fx-text-fill: white;");
            if (dashBtn != null) dashBtn.setStyle("-fx-background-color: transparent;");
        });
    }

    private void loadMyPatients() {
            User doctor = SessionManager.getCurrentUser();
            if (doctor == null) return;
            try {
                List<User> patients = userService.getPatientsByDoctor(doctor.getId());
                patientList.setAll(patients);
                patientTable.setItems(patientList);
                totalPatientsLabel.setText(String.valueOf(patients.size()));
                if (titleLabel != null) titleLabel.setText("Mes patients");
            } catch (SQLException e) {
                totalPatientsLabel.setText("0");
                e.printStackTrace();
            }

    }

    private void setupColumns() {
        pColNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        pColPrenom.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        pColEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        pColTel.setCellValueFactory(new PropertyValueFactory<>("numtel"));
    }

//    private void populateProfile() {
//        var u = SessionManager.getCurrentUser();
//        System.out.println(u);
//        if (u != null) {
//            profileNameLabel.setText(u.getNom() + " " + u.getPrenom());
////            profileEmailLabel.setText(u.getEmail());
////            profileRoleLabel.setText(u.getRole().toString());
//        }
//    }

    private void loadPatients() {
        try {
            // For demo, list all users with role PATIENT
            List<User> patients = userService.getByRole(User.Roles.PATIENT);
            patientList.setAll(patients);
            patientTable.setItems(patientList);
            totalPatientsLabel.setText(String.valueOf(patients.size()));
            if(titleLabel != null){
                titleLabel.setText("Tableau de bord");
            }
        } catch (Exception e) {
            totalPatientsLabel.setText("0");
            e.printStackTrace();
        }
    }

    @FXML
    private javafx.scene.control.Label profileNameLabel;
    @FXML
    private javafx.scene.control.Label profileEmailLabel;
    @FXML
    private javafx.scene.control.Label profileRoleLabel;
    @FXML private javafx.scene.layout.VBox profileBox;

    @FXML
    private void handleProfile() {
//        try {
//            // 1. Load the Profile FXML
//            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/vita/devora/DocteurPassword.fxml"));
//            Parent root = loader.load();
//
//            // 2. Access the controller to pass data (Preserve the User)
//            // Assuming your controller is named ProfileController
//            AddUserController controller = loader.getController();
//
//            // You can get the user from your existing SessionManager
//            User currentUser = SessionManager.getCurrentUser();
//            if (controller != null && currentUser != null) {
//                controller.setUser(currentUser);
//            }
//
//            // 3. Switch the scene
//            // We use patientTable to get the current stage
//            Stage stage = (Stage) patientTable.getScene().getWindow();
//            stage.getScene().setRoot(root);
//
//        } catch (java.io.IOException e) {
//            System.err.println("❌ Could not load Profile scene: " + e.getMessage());
//            e.printStackTrace();
//        }
    }

    @FXML
    private void handleLogout() {
        try {
            SessionManager.clearSession();
            Parent root = FXMLLoader.load(getClass().getResource("/com/vita/devora/LoginTest.fxml"));
            Stage stage = (Stage) patientTable.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
