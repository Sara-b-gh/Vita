package com.example.vita.Controlers;

import com.example.vita.Entites.equipements;
import com.example.vita.services.EquipementCRUD;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.Date;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

public class EquipementController implements Initializable {

    @FXML private TextField nomField, descField, typeField, marqueField, localisationField;
    @FXML private ComboBox<String> etatCombo;
    @FXML private DatePicker       datePicker;
    @FXML private Label            messageLabel, lblTotal;

    // ── Table ──
    @FXML private TableView<equipements>            equipementTable;
    @FXML private TableColumn<equipements, Integer> colId;
    @FXML private TableColumn<equipements, String>  colNom, colType, colMarque, colEtat, colLocalisation;
    @FXML private TableColumn<equipements, Date>    colDate;

    @FXML private FlowPane     cardsPane;
    @FXML private ToggleButton btnVueTable, btnVueCards;

    private final EquipementCRUD service = new EquipementCRUD();
    private int selectedId = -1;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Liaison des colonnes du tableau
        colId.setCellValueFactory(new PropertyValueFactory<>("id_equipement"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colMarque.setCellValueFactory(new PropertyValueFactory<>("marque"));
        colEtat.setCellValueFactory(new PropertyValueFactory<>("etat"));
        colLocalisation.setCellValueFactory(new PropertyValueFactory<>("localisation"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date_acquisition"));

        // Options de l'état
        etatCombo.setItems(FXCollections.observableArrayList(
                "disponible", "en_maintenance", "hors_service"));

        // Sélection d'une ligne du tableau -> Remplir le formulaire
        equipementTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, o, e) -> {
                    if (e != null) {
                        remplirFormulaire(e);
                    }
                });

        // Mode tableau affiché par défaut au démarrage
        switchToTable();
        chargerEquipements();
    }

    // ── Chargement des données ──────────────────────────────

    @FXML
    public void chargerEquipements() {
        try {
            List<equipements> list = service.afficher();
            equipementTable.setItems(FXCollections.observableArrayList(list));
            if (lblTotal != null) {
                lblTotal.setText(list.size() + " equipement(s)");
            }
            rafraichirCards(list);
        } catch (SQLException e) {
            showMessage("Erreur : " + e.getMessage(), "red");
        }
    }

    // ── Génération des Cartes (Sans aucune Image API) ───────

    private void rafraichirCards(List<equipements> list) {
        if (cardsPane == null) return;
        cardsPane.getChildren().clear();
        for (equipements e : list) {
            cardsPane.getChildren().add(creerCard(e));
        }
    }

    private VBox creerCard(equipements e) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setPrefWidth(200);
        card.setAlignment(Pos.TOP_LEFT);
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-radius: 14;" +
                        "-fx-border-color: #e8eaf0;" +
                        "-fx-cursor: hand;"
        );

        // Badge d'état de l'appareil
        Label badge = new Label(e.getEtat() != null ? e.getEtat().toUpperCase() : "INCONNU");
        String badgeStyle = switch (e.getEtat() != null ? e.getEtat().toLowerCase() : "") {
            case "disponible"     -> "-fx-background-color:#e8f5e9; -fx-text-fill:#27500A;";
            case "en_maintenance" -> "-fx-background-color:#fff8e1; -fx-text-fill:#854F0B;";
            default               -> "-fx-background-color:#fce4ec; -fx-text-fill:#6b0d1e;";
        };
        badge.setStyle(badgeStyle + "-fx-padding:4 10; -fx-background-radius:20; -fx-font-size:9px; -fx-font-weight:bold;");

        // Utilisation d'un bel émoji représentatif à la place d'une image API instable
        String emoji = "🔧";
        if (e.getType() != null) {
            String typeLower = e.getType().toLowerCase();
            if (typeLower.contains("imagerie") || typeLower.contains("scanner") || typeLower.contains("irm")) {
                emoji = "🏥";
            } else if (typeLower.contains("diagnostic") || typeLower.contains("tension")) {
                emoji = "🩺";
            } else if (typeLower.contains("urgence") || typeLower.contains("defibrillateur")) {
                emoji = "🚨";
            } else if (typeLower.contains("surveillance") || typeLower.contains("moniteur")) {
                emoji = "📊";
            } else if (typeLower.contains("respirateur") || typeLower.contains("reanimation")) {
                emoji = "🫁";
            }
        }

        Label icone = new Label(emoji);
        icone.setFont(Font.font(40));
        icone.setAlignment(Pos.CENTER);
        icone.setMaxWidth(Double.MAX_VALUE);
        icone.setStyle("-fx-padding: 10 0;");

        // Informations textuelles
        Label nom = new Label(e.getNom());
        nom.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        nom.setTextFill(Color.web("#1a1a2e"));
        nom.setWrapText(true);

        Label type = new Label("Type : " + (e.getType() != null ? e.getType() : "-"));
        type.setFont(Font.font("Segoe UI", 11));
        type.setTextFill(Color.web("#64748b"));

        Label marque = new Label("Marque : " + (e.getMarque() != null ? e.getMarque() : "-"));
        marque.setFont(Font.font("Segoe UI", 11));
        marque.setTextFill(Color.web("#64748b"));

        Label lieu = new Label("Salle : " + (e.getLocalisation() != null ? e.getLocalisation() : "-"));
        lieu.setFont(Font.font("Segoe UI", 11));
        lieu.setTextFill(Color.web("#6b0d1e"));
        lieu.setStyle("-fx-font-weight:bold;");

        // Événements Hover et Clic
        card.setOnMouseClicked(ev -> remplirFormulaire(e));
        card.setOnMouseEntered(ev -> card.setStyle(
                "-fx-background-color:white; -fx-background-radius:14;" +
                        "-fx-border-radius:14; -fx-border-color:#6b0d1e; -fx-cursor:hand;"));
        card.setOnMouseExited(ev -> card.setStyle(
                "-fx-background-color:white; -fx-background-radius:14;" +
                        "-fx-border-radius:14; -fx-border-color:#e8eaf0; -fx-cursor:hand;"));

        card.getChildren().addAll(badge, icone, nom, type, marque, lieu);
        return card;
    }

    // ── Switch de vue (Tableau / Cartes) ─────────────────────

    @FXML
    public void switchToTable() {
        if (equipementTable != null) {
            equipementTable.setVisible(true);
            equipementTable.setManaged(true);
        }
        if (cardsPane != null) {
            cardsPane.setVisible(false);
            cardsPane.setManaged(false);
        }
    }

    @FXML
    public void switchToCards() {
        if (equipementTable != null) {
            equipementTable.setVisible(false);
            equipementTable.setManaged(false);
        }
        if (cardsPane != null) {
            cardsPane.setVisible(true);
            cardsPane.setManaged(true);
        }
    }

    // ── CRUD Équipements ────────────────────────────────────

    @FXML
    public void ajouterEquipement() {
        if (!validerFormulaire()) return;
        try {
            service.ajouter(new equipements(
                    nomField.getText(),
                    descField.getText(),
                    typeField.getText(),
                    marqueField.getText(),
                    etatCombo.getValue(),
                    localisationField.getText(),
                    datePicker.getValue() != null ? Date.valueOf(datePicker.getValue()) : null
            ));
            showMessage("Equipement ajouté !", "green");
            clearForm();
            chargerEquipements();
        } catch (SQLException ex) {
            showMessage("Erreur : " + ex.getMessage(), "red");
        }
    }

    @FXML
    public void modifierEquipement() {
        if (selectedId == -1) {
            showMessage("Sélectionnez un équipement.", "orange");
            return;
        }
        if (!validerFormulaire()) return;
        try {
            service.modifier(new equipements(
                    selectedId,
                    nomField.getText(),
                    descField.getText(),
                    typeField.getText(),
                    marqueField.getText(),
                    etatCombo.getValue(),
                    localisationField.getText(),
                    datePicker.getValue() != null ? Date.valueOf(datePicker.getValue()) : null,
                    null
            ));
            showMessage("Equipement modifié !", "green");
            clearForm();
            chargerEquipements();
        } catch (SQLException ex) {
            showMessage("Erreur : " + ex.getMessage(), "red");
        }
    }

    @FXML
    public void supprimerEquipement() {
        if (selectedId == -1) {
            showMessage("Sélectionnez un équipement.", "orange");
            return;
        }
        new Alert(Alert.AlertType.CONFIRMATION, "Supprimer cet équipement ?", ButtonType.YES, ButtonType.NO)
                .showAndWait().ifPresent(r -> {
                    if (r == ButtonType.YES) {
                        try {
                            service.supprimer(selectedId);
                            showMessage("Équipement supprimé !", "green");
                            clearForm();
                            chargerEquipements();
                        } catch (SQLException ex) {
                            showMessage("Erreur : " + ex.getMessage(), "red");
                        }
                    }
                });
    }

    // ── Navigation ──────────────────────────────────────────

    @FXML private void allerAuDashboard() { naviguer("/Main-view.fxml"); }
    @FXML private void allerAuxMedicaments() { naviguer("/AjouterMedicament.fxml"); }
    @FXML private void allerAuxCommandes() { naviguer("/Commande-view.fxml"); }

    private void naviguer(String fxml) {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource(fxml)));
            Stage stage = (Stage) equipementTable.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            showMessage("Erreur navigation : " + e.getMessage(), "red");
        }
    }

    // ── Utilitaires Formulaire ──────────────────────────────

    private void remplirFormulaire(equipements e) {
        selectedId = e.getId_equipement();
        nomField.setText(e.getNom());
        descField.setText(e.getDescription());
        typeField.setText(e.getType());
        marqueField.setText(e.getMarque());
        etatCombo.setValue(e.getEtat());
        localisationField.setText(e.getLocalisation());
        if (e.getDate_acquisition() != null) {
            datePicker.setValue(e.getDate_acquisition().toLocalDate());
        }
    }

    private void clearForm() {
        selectedId = -1;
        nomField.clear();
        descField.clear();
        typeField.clear();
        marqueField.clear();
        localisationField.clear();
        etatCombo.setValue(null);
        datePicker.setValue(null);
    }

    private boolean validerFormulaire() {
        if (nomField.getText().isBlank()) {
            showMessage("Nom obligatoire.", "red");
            return false;
        }
        if (etatCombo.getValue() == null) {
            showMessage("État obligatoire.", "red");
            return false;
        }
        return true;
    }

    private void showMessage(String msg, String color) {
        if (messageLabel != null) {
            messageLabel.setText(msg);
            messageLabel.setStyle("-fx-text-fill:" + color + ";");
        }
    }
}