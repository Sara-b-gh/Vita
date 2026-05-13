package com.vita.devora.Controlleurs;

import com.vita.devora.Entities.User;
import com.vita.devora.Services.UserService;
import com.vita.devora.utils.SessionManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class AdminDocteurController {

    @FXML private FlowPane doctorCardsPane;
    @FXML private TextField searchField;
    @FXML private Label totalDoctorsLabel;
    @FXML private ComboBox<String> departementFilter; // ← AJOUTÉ
    @FXML private ComboBox<String> sortOrder;         // ← AJOUTÉ

    private final UserService userService = new UserService();
    private List<User> allDoctors; // ← UNE SEULE liste

    public AdminDocteurController() throws SQLException {
    }

    @FXML
    public void initialize() {
        loadData();

        // Tri
        sortOrder.getItems().addAll("A-Z", "Z-A");
        sortOrder.setValue("A-Z");

        // Listeners
        searchField.textProperty().addListener((obs, old, newVal) -> handleFilter(null));
        sortOrder.valueProperty().addListener((obs, old, newVal) -> handleFilter(null));
        departementFilter.valueProperty().addListener((obs, old, newVal) -> handleFilter(null));
    }

    private void loadData() {
        try {
            allDoctors = userService.getDoctors();

            // Remplir filtre département
            List<String> departements = allDoctors.stream()
                    .map(User::getDepartement)
                    .filter(d -> d != null && !d.isEmpty())
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());

            departementFilter.getItems().clear();
            departementFilter.getItems().add("Tous");
            departementFilter.getItems().addAll(departements);
            departementFilter.setValue("Tous");

            totalDoctorsLabel.setText(String.valueOf(allDoctors.size()));
            buildCards(allDoctors);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleFilter(ActionEvent actionEvent) {
        String search = searchField.getText() == null ? ""
                : searchField.getText().toLowerCase().trim();
        String dept = departementFilter.getValue();
        String sort = sortOrder.getValue();

        List<User> filtered = allDoctors.stream()
                .filter(u -> search.isEmpty()
                        || u.getNom().toLowerCase().contains(search)
                        || u.getPrenom().toLowerCase().contains(search))
                .filter(u -> dept == null
                        || dept.equals("Tous")
                        || dept.equals(u.getDepartement()))
                .sorted((a, b) -> {
                    int cmp = (a.getNom() + a.getPrenom())
                            .compareToIgnoreCase(b.getNom() + b.getPrenom());
                    return "Z-A".equals(sort) ? -cmp : cmp;
                })
                .collect(Collectors.toList());

        totalDoctorsLabel.setText(String.valueOf(filtered.size()));
        buildCards(filtered);
    }

    private void buildCards(List<User> users) {
        doctorCardsPane.getChildren().clear();
        for (User u : users) {
            doctorCardsPane.getChildren().add(createCard(u));
        }
    }

    @FXML
    private void handleAddDoctor() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/vita/devora/AjouterDocteur.fxml"));
            Stage stage = new Stage();
            stage.setTitle("Ajouter un docteur");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
            loadData();
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Impossible d'ouvrir le formulaire d'ajout.").showAndWait();
        }
    }

    private VBox createCard(User u) {
        Label avatar = new Label(
                (u.getPrenom().charAt(0) + "" + u.getNom().charAt(0)).toUpperCase()
        );
        avatar.setStyle("""
            -fx-background-color: #fdedf3;
            -fx-text-fill: #8a0037;
            -fx-font-weight: bold;
            -fx-font-size: 16;
        """);

        StackPane avatarBox = new StackPane(avatar);
        avatarBox.setMinSize(50, 50);
        avatarBox.setStyle("-fx-background-radius: 25; -fx-background-color: #fdedf3;");

        Label name = new Label(u.getPrenom() + " " + u.getNom());
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");

        Label dept = new Label(u.getDepartement() != null ? u.getDepartement() : "Non assigné");
        dept.setStyle("""
            -fx-background-color: #eaf7ff;
            -fx-text-fill: #0077b6;
            -fx-background-radius: 12;
            -fx-padding: 2 10;
            -fx-font-size: 11;
        """);

        VBox info = new VBox(4, name, dept);
        HBox header = new HBox(10, avatarBox, info);

        Label email = new Label("✉ " + u.getEmail());
        Label tel = new Label("☎ " + u.getNumtel());
        VBox body = new VBox(6, email, tel);

        Button edit = new Button("Modifier");
        edit.setStyle("""
            -fx-background-color: #2ecc71;
            -fx-text-fill: white;
            -fx-background-radius: 8;
            -fx-font-size: 12;
            -fx-cursor: hand;
        """);
        edit.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(edit, Priority.ALWAYS);
        edit.setOnAction(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/com/vita/devora/AjouterDocteur.fxml")
                );
                Parent root = loader.load();
                AddUserController controller = loader.getController();
                controller.setUser(u);
                Stage stage = new Stage();
                stage.setTitle("Modifier un docteur");
                stage.setScene(new Scene(root));
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.showAndWait();
                loadData();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        Button delete = new Button("Supprimer");
        delete.setStyle("""
            -fx-background-color: #e74c3c;
            -fx-text-fill: white;
            -fx-background-radius: 8;
            -fx-font-size: 12;
            -fx-cursor: hand;
        """);
        delete.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(delete, Priority.ALWAYS);
        delete.setOnAction(e -> {
            try {
                userService.supprimer(u.getId());
                loadData();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });

        HBox actions = new HBox(8, edit, delete);
        VBox card = new VBox(10, header, body, actions);
        card.getStyleClass().add("vita-card");
        card.setPadding(new Insets(15));
        card.setPrefWidth(240);
        card.setMaxWidth(240);
        return card;
    }

    @FXML
    private void haddledeconnexion() {
        try {
            SessionManager.clearSession();
            Parent root = FXMLLoader.load(getClass().getResource("/com/vita/devora/LoginTest.fxml"));
            Stage stage = (Stage) doctorCardsPane.getScene().getWindow();
            stage.setResizable(false);
            stage.setWidth(1280);
            stage.setHeight(720);
            stage.centerOnScreen();
            stage.setScene(new Scene(root));
            stage.setTitle("Connexion");
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Impossible de se déconnecter.").showAndWait();
        }
    }

    private void switchPage(ActionEvent event, String fxmlPath) {
        try {
            java.net.URL resource = getClass().getResource(fxmlPath);
            if (resource == null) {
                System.err.println("❌ FXML File not found at: " + fxmlPath);
                return;
            }
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(resource);
            javafx.scene.Parent root = loader.load();
            javafx.scene.Node sourceNode = (javafx.scene.Node) event.getSource();
            javafx.stage.Stage stage = (javafx.stage.Stage) sourceNode.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (java.io.IOException e) {
            System.err.println("❌ Critical error loading: " + fxmlPath);
            e.printStackTrace();
        }
    }

    public void toDash(ActionEvent actionEvent) {
        switchPage(actionEvent, "/com/vita/devora/AdminDashbord.fxml");
    }

    public void toPatientDash(ActionEvent actionEvent) {
        switchPage(actionEvent, "/com/vita/devora/AdminPatient.fxml");
    }
}