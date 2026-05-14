package Controlers;

import com.example.vita.Entites.medicaments;
import com.example.vita.services.ImageAPIService;
import com.example.vita.services.MedicamentCRUD;
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
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

public class MedicamentController implements Initializable {

    // ── Formulaire ──
    @FXML private TextField        nomField, descField, dosageField, prixField, stockField;
    @FXML private ComboBox<String> formeCombo, statutCombo;
    @FXML private Label            messageLabel, lblTotal, lblStatus;

    // ── Image formulaire ──
    @FXML private ImageView imgFormulaire;

    // ── Table ──
    @FXML private TableView<medicaments>            medicamentTable;
    @FXML private TableColumn<medicaments, Integer> colId, colStock;
    @FXML private TableColumn<medicaments, String>  colNom, colDosage, colForme, colStatut;
    @FXML private TableColumn<medicaments, Double>  colPrix;
    @FXML private TableColumn<medicaments, Void>    colImage;

    // ── Cards ──
    @FXML private FlowPane     cardsPane;
    @FXML private ToggleButton btnVueTable, btnVueCards;

    // ── Toolbar Main-view ──
    @FXML private ToggleButton  btnVueMedicaments, btnVueEquipements;
    @FXML private TextField     searchField;
    @FXML private ComboBox<String> filterStatut;
    @FXML private GridPane      gridCards;
    @FXML private ScrollPane    scrollCenter, scrollDetail;
    @FXML private VBox          detailContent;

    private final MedicamentCRUD service    = new MedicamentCRUD();
    private int                  selectedId = -1;

    // ─────────────────────────────────────────────────────────────
    //  Initialisation
    // ─────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        // Colonnes table (null-safe car ce FXML peut ne pas avoir de table)
        if (colId     != null) colId.setCellValueFactory(new PropertyValueFactory<>("id_medicament"));
        if (colNom    != null) colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        if (colDosage != null) colDosage.setCellValueFactory(new PropertyValueFactory<>("dosage"));
        if (colForme  != null) colForme.setCellValueFactory(new PropertyValueFactory<>("forme"));
        if (colPrix   != null) colPrix.setCellValueFactory(new PropertyValueFactory<>("prix"));
        if (colStock  != null) colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        if (colStatut != null) colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        // Colonne image tableau
        if (colImage != null) {
            colImage.setCellFactory(col -> new TableCell<>() {
                private final ImageView iv = new ImageView();
                { iv.setFitWidth(60); iv.setFitHeight(45);
                    iv.setPreserveRatio(true); iv.setSmooth(true); }
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                        setGraphic(null); return;
                    }
                    medicaments m = (medicaments) getTableRow().getItem();
                    ImageAPIService.chargerImageDansView(m.getNom(), true, iv, 60, 45);
                    setGraphic(iv);
                }
            });
        }

        // ComboBox formulaire
        if (formeCombo != null)
            formeCombo.setItems(FXCollections.observableArrayList(
                    "comprime", "sirop", "injection", "creme", "capsule"));
        if (statutCombo != null)
            statutCombo.setItems(FXCollections.observableArrayList(
                    "disponible", "epuise", "archive"));

        // Sélection table → remplir formulaire
        if (medicamentTable != null) {
            medicamentTable.getSelectionModel().selectedItemProperty().addListener(
                    (obs, o, m) -> {
                        if (m != null) {
                            remplirFormulaire(m);
                            if (imgFormulaire != null)
                                ImageAPIService.chargerImageDansView(m.getNom(), true, imgFormulaire, 240, 150);
                        }
                    });
        }

        // Cards cachées par défaut
        if (cardsPane != null) {
            cardsPane.setVisible(false);
            cardsPane.setManaged(false);
        }

        // Toggle Médicaments sélectionné par défaut
        if (btnVueMedicaments != null) btnVueMedicaments.setSelected(true);

        chargerMedicaments();
    }

    // ─────────────────────────────────────────────────────────────
    //  Chargement données
    // ─────────────────────────────────────────────────────────────

    @FXML
    public void chargerMedicaments() {
        try {
            List<medicaments> list = service.afficher();
            if (medicamentTable != null)
                medicamentTable.setItems(FXCollections.observableArrayList(list));
            if (lblTotal != null)
                lblTotal.setText(list.size() + " médicament(s)");
            rafraichirCards(list);
            rafraichirGridCards(list);
        } catch (SQLException e) {
            showMessage("Erreur : " + e.getMessage(), "red");
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  GridPane cards (Main-view)
    // ─────────────────────────────────────────────────────────────

    private void rafraichirGridCards(List<medicaments> list) {
        if (gridCards == null) return;
        gridCards.getChildren().clear();
        int col = 0, row = 0;
        for (medicaments m : list) {
            gridCards.add(creerCard(m), col, row);
            col++;
            if (col == 3) { col = 0; row++; }
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  FlowPane cards (Medicament-view)
    // ─────────────────────────────────────────────────────────────

    private void rafraichirCards(List<medicaments> list) {
        if (cardsPane == null) return;
        cardsPane.getChildren().clear();
        for (medicaments m : list)
            cardsPane.getChildren().add(creerCard(m));
    }

    private VBox creerCard(medicaments m) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(14));
        card.setPrefWidth(210);
        card.setStyle("-fx-background-color:white; -fx-background-radius:14;" +
                "-fx-border-radius:14; -fx-border-color:#e8eaf0; -fx-cursor:hand;");

        // Badge statut
        Label badge = new Label(m.getStatut());
        String badgeColor = switch (m.getStatut()) {
            case "disponible" -> "-fx-background-color:#e8f5e9; -fx-text-fill:#27500A;";
            case "epuise"     -> "-fx-background-color:#fce4ec; -fx-text-fill:#6b0d1e;";
            default           -> "-fx-background-color:#fff8e1; -fx-text-fill:#854F0B;";
        };
        badge.setStyle(badgeColor + "-fx-padding:3 10; -fx-background-radius:20;" +
                "-fx-font-size:10px; -fx-font-weight:bold;");

        // Image
        ImageView imageView = new ImageView();
        imageView.setFitWidth(180); imageView.setFitHeight(110);
        imageView.setPreserveRatio(true); imageView.setSmooth(true);
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(180, 110);
        clip.setArcWidth(12); clip.setArcHeight(12);
        imageView.setClip(clip);
        ImageAPIService.chargerImageDansView(m.getNom(), true, imageView, 180, 110);

        // Bouton autre image
        Button btnImg = new Button("🔄 Autre image");
        btnImg.setStyle("-fx-background-color:#f5f7fa; -fx-text-fill:#6b0d1e;" +
                "-fx-border-color:#e5b7c4; -fx-border-radius:8;" +
                "-fx-background-radius:8; -fx-font-size:11px;" +
                "-fx-cursor:hand; -fx-padding:4 10;");
        btnImg.setMaxWidth(Double.MAX_VALUE);
        btnImg.setOnAction(e -> {
            javafx.scene.image.Image img = new javafx.scene.image.Image(
                    "https://picsum.photos/seed/" + m.getNom() + System.currentTimeMillis() + "/400/300",
                    180, 110, true, true, true);
            imageView.setImage(img);
        });

        // Emoji forme
        String emoji = switch (m.getForme() != null ? m.getForme() : "") {
            case "comprime"  -> "💊";
            case "sirop"     -> "🧴";
            case "injection" -> "💉";
            case "creme"     -> "🫧";
            case "capsule"   -> "🔴";
            default          -> "💊";
        };
        Label icone = new Label(emoji);
        icone.setFont(Font.font(28));
        icone.setAlignment(Pos.CENTER);
        icone.setMaxWidth(Double.MAX_VALUE);

        Label nom = new Label(m.getNom());
        nom.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        nom.setTextFill(Color.web("#1a1a2e")); nom.setWrapText(true);

        Label dosage = new Label("Dosage : " + m.getDosage());
        dosage.setFont(Font.font("Segoe UI", 11));
        dosage.setTextFill(Color.web("#64748b"));

        Label prix = new Label("Prix : " + m.getPrix() + " DT");
        prix.setFont(Font.font("Segoe UI", 11));
        prix.setTextFill(Color.web("#6b0d1e"));
        prix.setStyle("-fx-font-weight:bold;");

        Label stock = new Label("Stock : " + m.getStock());
        stock.setFont(Font.font("Segoe UI", 11));
        stock.setTextFill(Color.web("#64748b"));

        card.setOnMouseClicked(e -> {
            if (nomField != null) remplirFormulaire(m);
            if (imgFormulaire != null)
                ImageAPIService.chargerImageDansView(m.getNom(), true, imgFormulaire, 240, 150);
        });
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color:white; -fx-background-radius:14;" +
                        "-fx-border-radius:14; -fx-border-color:#6b0d1e; -fx-cursor:hand;"));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color:white; -fx-background-radius:14;" +
                        "-fx-border-radius:14; -fx-border-color:#e8eaf0; -fx-cursor:hand;"));

        card.getChildren().addAll(badge, imageView, btnImg, icone, nom, dosage, prix, stock);
        return card;
    }

    // ─────────────────────────────────────────────────────────────
    //  Switch de vue (toolbar toggle)
    // ─────────────────────────────────────────────────────────────

    @FXML
    public void switchToMedicaments() {
        if (btnVueMedicaments != null) btnVueMedicaments.setSelected(true);
        if (btnVueEquipements != null) btnVueEquipements.setSelected(false);
        chargerMedicaments();
        if (lblTotal != null) lblTotal.setText("médicaments");
    }

    @FXML
    public void switchToEquipements() {
        if (btnVueEquipements != null) btnVueEquipements.setSelected(true);
        if (btnVueMedicaments != null) btnVueMedicaments.setSelected(false);
        if (lblTotal != null) lblTotal.setText("équipements");
        // naviguer vers la vue équipements si besoin :
        // naviguer("/Equippement-view.fxml");
    }

    @FXML
    public void ouvrirAjoutPopup() {
        // Vider le formulaire pour un nouvel ajout
        clearForm();
        if (scrollDetail != null) {
            scrollDetail.setVisible(true);
            scrollDetail.setManaged(true);
        }
        if (scrollCenter != null) {
            scrollCenter.setVisible(false);
            scrollCenter.setManaged(false);
        }
    }

    @FXML
    public void switchToTable() {
        if (medicamentTable != null) { medicamentTable.setVisible(true); medicamentTable.setManaged(true); }
        if (cardsPane != null)       { cardsPane.setVisible(false);       cardsPane.setManaged(false); }
    }

    @FXML
    public void switchToCards() {
        if (medicamentTable != null) { medicamentTable.setVisible(false); medicamentTable.setManaged(false); }
        if (cardsPane != null)       { cardsPane.setVisible(true);        cardsPane.setManaged(true); }
    }

    // ─────────────────────────────────────────────────────────────
    //  CRUD
    // ─────────────────────────────────────────────────────────────

    @FXML
    public void ajouterMedicament() {
        if (!validerFormulaire()) return;
        try {
            service.ajouter(new medicaments(
                    nomField.getText(), descField.getText(), dosageField.getText(),
                    formeCombo.getValue(),
                    Double.parseDouble(prixField.getText()),
                    Integer.parseInt(stockField.getText()),
                    statutCombo.getValue()
            ));
            showMessage("Médicament ajouté !", "green");
            clearForm();
            chargerMedicaments();
        } catch (Exception ex) {
            showMessage("Erreur : " + ex.getMessage(), "red");
        }
    }

    @FXML
    public void modifierMedicament() {
        if (selectedId == -1) { showMessage("Sélectionnez un médicament.", "orange"); return; }
        if (!validerFormulaire()) return;
        try {
            service.modifier(new medicaments(
                    selectedId, nomField.getText(), descField.getText(),
                    dosageField.getText(), formeCombo.getValue(),
                    Double.parseDouble(prixField.getText()),
                    Integer.parseInt(stockField.getText()),
                    statutCombo.getValue(), null, null
            ));
            showMessage("Médicament modifié !", "green");
            clearForm();
            chargerMedicaments();
        } catch (Exception ex) {
            showMessage("Erreur : " + ex.getMessage(), "red");
        }
    }

    @FXML
    public void supprimerMedicament() {
        if (selectedId == -1) { showMessage("Sélectionnez un médicament.", "orange"); return; }
        new Alert(Alert.AlertType.CONFIRMATION, "Supprimer ?", ButtonType.YES, ButtonType.NO)
                .showAndWait().ifPresent(r -> {
                    if (r == ButtonType.YES) {
                        try {
                            service.supprimer(selectedId);
                            clearForm();
                            chargerMedicaments();
                        } catch (SQLException ex) {
                            showMessage("Erreur : " + ex.getMessage(), "red");
                        }
                    }
                });
    }

    // ─────────────────────────────────────────────────────────────
    //  Navigation
    // ─────────────────────────────────────────────────────────────

    @FXML private void allerAuxEquipements() { naviguer("/Equippement-view.fxml"); }
    @FXML private void allerAuDashboard()    { naviguer("/Main-view.fxml"); }
    @FXML private void allerAuxCommandes()   { naviguer("/Commande-view.fxml"); }

    private void naviguer(String fxml) {
        try {
            Stage stage = null;
            if (medicamentTable != null && medicamentTable.getScene() != null)
                stage = (Stage) medicamentTable.getScene().getWindow();
            else if (cardsPane != null && cardsPane.getScene() != null)
                stage = (Stage) cardsPane.getScene().getWindow();
            else if (nomField != null && nomField.getScene() != null)
                stage = (Stage) nomField.getScene().getWindow();
            if (stage == null) { showMessage("Erreur navigation.", "red"); return; }
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource(fxml)));
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            showMessage("Erreur navigation : " + e.getMessage(), "red");
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  Utilitaires
    // ─────────────────────────────────────────────────────────────

    private void remplirFormulaire(medicaments m) {
        selectedId = m.getId_medicament();
        if (nomField    != null) nomField.setText(m.getNom());
        if (descField   != null) descField.setText(m.getDescription());
        if (dosageField != null) dosageField.setText(m.getDosage());
        if (formeCombo  != null) formeCombo.setValue(m.getForme());
        if (prixField   != null) prixField.setText(String.valueOf(m.getPrix()));
        if (stockField  != null) stockField.setText(String.valueOf(m.getStock()));
        if (statutCombo != null) statutCombo.setValue(m.getStatut());
    }

    private void clearForm() {
        selectedId = -1;
        if (nomField    != null) nomField.clear();
        if (descField   != null) descField.clear();
        if (dosageField != null) dosageField.clear();
        if (prixField   != null) prixField.clear();
        if (stockField  != null) stockField.clear();
        if (formeCombo  != null) formeCombo.setValue(null);
        if (statutCombo != null) statutCombo.setValue(null);
        if (imgFormulaire != null) imgFormulaire.setImage(null);
    }

    private boolean validerFormulaire() {
        if (nomField == null || nomField.getText().isBlank()) {
            showMessage("Nom obligatoire.", "red"); return false;
        }
        try {
            Double.parseDouble(prixField.getText());
            Integer.parseInt(stockField.getText());
        } catch (NumberFormatException e) {
            showMessage("Prix/stock invalides.", "red"); return false;
        }
        if (formeCombo.getValue() == null) {
            showMessage("Forme obligatoire.", "red"); return false;
        }
        if (statutCombo.getValue() == null) {
            showMessage("Statut obligatoire.", "red"); return false;
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