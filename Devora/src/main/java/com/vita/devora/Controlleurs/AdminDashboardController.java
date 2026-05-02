package com.vita.devora.Controlleurs;

import com.vita.devora.Entities.User;
import com.vita.devora.Services.UserService;
import com.vita.devora.utils.SessionManager;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;
import javafx.scene.layout.HBox;

import java.sql.SQLException;
import java.util.List;

public class AdminDashboardController {

    @FXML private TableView<User> doctorTable;
    @FXML private TableColumn<User, String> colNom;
    @FXML private TableColumn<User, String> colPrenom;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, Integer> colTel;
    @FXML private TableColumn<User, Void> colActions;
    @FXML private Label totalDoctorsLabel;
    @FXML private Label profileNameLabel;
    @FXML private Label profileEmailLabel;
    @FXML private Label profileRoleLabel;
    @FXML private javafx.scene.layout.VBox profileBox;
    @FXML private javafx.scene.control.Button profileBtn;
    @FXML private javafx.scene.control.Button dashBtn;
    @FXML private javafx.scene.control.Button doctorsBtn;
    @FXML private javafx.scene.control.Button patientsBtn;
    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private Button addBtn;
    @FXML private javafx.scene.layout.BorderPane rootPane;

    @FXML
    private void handleProfile() {
        if (profileBox == null) return;
        String cur = profileBox.getStyle();
        if (cur != null && cur.contains("-fx-border-color")) {
            profileBox.setStyle("");
        } else {
            profileBox.setStyle("-fx-border-color: #FF4757; -fx-border-width: 2;");
        }
    }

    private final UserService userService = new UserService();
    private ObservableList<User> doctorList = FXCollections.observableArrayList();
    private User.Roles currentRole = User.Roles.DOCTOR;
    private Node mainContent;

    @FXML
    public void initialize() {
        mainContent = rootPane.getCenter();
        setupColumns();
        // default show doctors
        loadData(User.Roles.DOCTOR);
        // wire sidebar filters
        if (dashBtn != null) dashBtn.setOnAction(e -> loadData(null));
        if (doctorsBtn != null) doctorsBtn.setOnAction(e -> loadData(User.Roles.DOCTOR));
        if (patientsBtn != null) patientsBtn.setOnAction(e -> loadData(User.Roles.PATIENT));
        if (profileBtn != null) profileBtn.setOnAction(e -> handleOpenProfile());
        populateProfile();
    }

    private void populateProfile() {
        var u = SessionManager.getCurrentUser();
        if (u != null) {
            if (profileNameLabel != null) profileNameLabel.setText(u.getNom() + " " + u.getPrenom());
            if (profileEmailLabel != null) profileEmailLabel.setText(u.getEmail());
            if (profileRoleLabel != null) profileRoleLabel.setText(u.getRole().toString());
        }
    }

    private void setupColumns() {
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colPrenom.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colTel.setCellValueFactory(new PropertyValueFactory<>("numtel"));

        // Custom Actions Column (Edit/Delete)
        Callback<TableColumn<User, Void>, TableCell<User, Void>> cellFactory = param -> new TableCell<>() {
            private final Button btnEdit = new Button("Modifier");
            private final Button btnDelete = new Button("Supprimer");
            private final HBox box = new HBox(8, btnEdit, btnDelete);
            {
                btnEdit.setStyle("-fx-background-color: transparent; -fx-text-fill: #2d98f0;");
                btnDelete.setStyle("-fx-text-fill: #ff4757; -fx-background-color: transparent;");

                btnEdit.setOnAction(event -> {
                    int idx = getIndex();
                    if (idx >= 0 && idx < getTableView().getItems().size()) {
                        User user = getTableView().getItems().get(idx);
                        handleEditUser(user);
                    }
                });

                btnDelete.setOnAction(event -> {
                    int idx = getIndex();
                    if (idx >= 0 && idx < getTableView().getItems().size()) {
                        User user = getTableView().getItems().get(idx);
                        handleDeleteDoctor(user);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        };
        colActions.setCellFactory(cellFactory);
    }

    private void loadData(User.Roles role) {
        if (mainContent != null && rootPane.getCenter() != mainContent) {
            rootPane.setCenter(mainContent);
        }
        try {
            List<User> users;
            if (role == null) {
                users = userService.getAllUsers();
            } else {
                users = userService.getByRole(role);
            }
            doctorList.setAll(users);
            doctorTable.setItems(doctorList);
            totalDoctorsLabel.setText(String.valueOf(users.size()));
                // remember current role for add/refresh actions
                currentRole = role == null ? null : role;
            // update title/subtitle and add button depending on role
            if (role == User.Roles.DOCTOR) {
                if (titleLabel != null) titleLabel.setText("Gestion des médecins");
                if (subtitleLabel != null) subtitleLabel.setText("Ajouter, modifier ou supprimer des médecins");
                if (addBtn != null) { addBtn.setVisible(true); addBtn.setText("+ Ajouter un médecin"); }
                if (doctorsBtn != null) doctorsBtn.setStyle("-fx-background-color: #FF4757; -fx-text-fill: white;");
                if (patientsBtn != null) patientsBtn.setStyle("-fx-background-color: transparent;");
            } else if (role == User.Roles.PATIENT) {
                if (titleLabel != null) titleLabel.setText("Gestion des patients");
                if (subtitleLabel != null) subtitleLabel.setText("Voir et gérer les patients");
                    if (addBtn != null) { addBtn.setVisible(true); addBtn.setText("+ Ajouter un patient"); }
                if (patientsBtn != null) patientsBtn.setStyle("-fx-background-color: #FF4757; -fx-text-fill: white;");
                if (doctorsBtn != null) doctorsBtn.setStyle("-fx-background-color: transparent;");
            } else {
                if (titleLabel != null) titleLabel.setText("Tableau de bord");
                if (subtitleLabel != null) subtitleLabel.setText("");
                if (addBtn != null) addBtn.setVisible(false);
                if (doctorsBtn != null) doctorsBtn.setStyle("-fx-background-color: transparent;");
                if (patientsBtn != null) patientsBtn.setStyle("-fx-background-color: transparent;");
            }
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les utilisateurs : " + e.getMessage());
        }
    }

    @FXML
    private void handleAddDoctor() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/vita/devora/AjouterDocteur.fxml"));
            javafx.scene.Parent root = loader.load();
            // set default role in controller according to currentRole
            com.vita.devora.Controlleurs.AddUserController controller = loader.getController();
            controller.setDefaultRole(currentRole == null ? User.Roles.DOCTOR : currentRole);
            // show as modal
            Stage dialog = new Stage();
            dialog.initOwner(totalDoctorsLabel.getScene().getWindow());
            dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialog.setScene(new javafx.scene.Scene(root));
                dialog.setTitle(currentRole == User.Roles.PATIENT ? "Ajouter un patient" : "Ajouter un médecin");
            dialog.showAndWait();
            // after dialog closed, refresh data
                loadData(currentRole);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir le formulaire d'ajout: " + e.getMessage());
        }
    }

    private void handleDeleteDoctor(User user) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer " + user.getNom() + " ?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    userService.supprimer(user.getId());
                    loadData(currentRole); // Refresh list with current filter
                } catch (SQLException e) {
                    showAlert("Erreur", "Échec de suppression: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    private void handleEditUser(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/vita/devora/AjouterDocteur.fxml"));
            Parent root = loader.load();
            com.vita.devora.Controlleurs.AddUserController controller = loader.getController();
            controller.setUser(user);
            Stage dialog = new Stage();
            dialog.initOwner(totalDoctorsLabel.getScene().getWindow());
            dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialog.setScene(new javafx.scene.Scene(root));
            dialog.setTitle("Modifier utilisateur");
            dialog.showAndWait();
            loadData(currentRole);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir le formulaire de modification: " + e.getMessage());
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    private void handleOpenProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/vita/devora/views/ProfileView.fxml"));
            Parent root = loader.load();
            // place profile view in center
            javafx.scene.layout.BorderPane bp = (javafx.scene.layout.BorderPane) rootPane;
            bp.setCenter(root);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir le profil: " + e.getMessage());
        }
    }

    @FXML
    private void handleLogout() {
        try {
            SessionManager.clearSession();
            Parent root = FXMLLoader.load(getClass().getResource("/com/vita/devora/LoginTest.fxml"));
            javafx.stage.Window window = null;
            if (rootPane != null && rootPane.getScene() != null) window = rootPane.getScene().getWindow();
            else if (doctorTable != null && doctorTable.getScene() != null) window = doctorTable.getScene().getWindow();
            else if (totalDoctorsLabel != null && totalDoctorsLabel.getScene() != null) window = totalDoctorsLabel.getScene().getWindow();

            if (window instanceof Stage) {
                Stage stage = (Stage) window;
                stage.getScene().setRoot(root);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}