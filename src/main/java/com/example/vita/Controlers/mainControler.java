package Controlers;

import com.example.vita.Entites.equipements;
import com.example.vita.Entites.medicaments;
import com.example.vita.services.ChatbotService;
import com.example.vita.services.EquipementCRUD;
import com.example.vita.services.MedicamentCRUD;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class mainControler {

    @FXML private Label userNameLabel, clockLabel, lastUpdateLabel;
    @FXML private Label kpiMedTotalVal, kpiMedDispoVal, kpiEqTotalVal, kpiEqMaintVal;
    @FXML private Label medTotalMini, medDispoMini, medEpuisMini;
    @FXML private Label eqTotalMini,  eqDispoMini,  eqMaintMini;

    // ── Images locales PNG pour le Tableau de bord ──
    @FXML private ImageView medicationRealImage;
    @FXML private ImageView equipmentRealImage;

    // ── Chatbot ──
    @FXML private VBox             chatbotPanel;
    @FXML private ListView<String> chatMessages;
    @FXML private TextField        chatInput;

    private final MedicamentCRUD medicamentService = new MedicamentCRUD();
    private final EquipementCRUD equipementService = new EquipementCRUD();

    private static final DateTimeFormatter FMT_TIME =
            DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FMT_FULL =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // =========================================================
    //  INITIALIZE
    // =========================================================

    @FXML
    public void initialize() {
        if (userNameLabel != null)
            userNameLabel.setText("Administrateur");

        // Horloge
        Timeline clock = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> {
                    if (clockLabel != null)
                        clockLabel.setText(
                                LocalDateTime.now().format(FMT_TIME));
                }));
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();

        // Chatbot caché par défaut
        if (chatbotPanel != null) {
            chatbotPanel.setVisible(false);
            chatbotPanel.setManaged(false);
        }

        // Chargement des images locales au format PNG
        chargerImagesInventaire();

        // Stats
        actualiserStats();
    }

    // =========================================================
    //  IMAGES (CORRIGÉ POUR VOS IMAGES LOCALES PNG)
    // =========================================================

    private void chargerImagesInventaire() {
        // 1. Chargement de votre image physique locale "medicament.png"
        if (medicationRealImage != null) {
            try {
                URL medUrl = getClass().getResource("/images/medicament.png");
                if (medUrl != null) {
                    medicationRealImage.setImage(new Image(medUrl.toExternalForm()));
                    System.out.println("[VITA] Image locale chargée : /images/medicament.png");
                } else {
                    System.err.println("[ERREUR VITA] Fichier introuvable : /images/medicament.png");
                }
            } catch (Exception e) {
                System.err.println("Erreur image locale medicament : " + e.getMessage());
            }
        }

        // 2. Chargement de votre image physique locale "equipement.png"
        if (equipmentRealImage != null) {
            try {
                URL eqUrl = getClass().getResource("/images/equipement.png");
                if (eqUrl != null) {
                    equipmentRealImage.setImage(new Image(eqUrl.toExternalForm()));
                    System.out.println("[VITA] Image locale chargée : /images/equipement.png");
                } else {
                    System.err.println("[ERREUR VITA] Fichier introuvable : /images/equipement.png");
                }
            } catch (Exception e) {
                System.err.println("Erreur image locale equipement : " + e.getMessage());
            }
        }
    }

    // =========================================================
    //  STATS
    // =========================================================

    @FXML
    public void actualiserStats() {
        chargerStatsMedicaments();
        chargerStatsEquipements();
        if (lastUpdateLabel != null)
            lastUpdateLabel.setText(
                    "Mis a jour : " + LocalDateTime.now().format(FMT_FULL));
    }

    private void chargerStatsMedicaments() {
        try {
            List<medicaments> list = medicamentService.afficher();
            long total   = list.size();
            long dispo   = list.stream()
                    .filter(m -> "disponible"
                            .equalsIgnoreCase(m.getStatut())).count();
            long epuises = list.stream()
                    .filter(m -> "epuise"
                            .equalsIgnoreCase(m.getStatut())).count();
            setText(kpiMedTotalVal, String.valueOf(total));
            setText(kpiMedDispoVal, String.valueOf(dispo));
            setText(medTotalMini,   String.valueOf(total));
            setText(medDispoMini,   String.valueOf(dispo));
            setText(medEpuisMini,   String.valueOf(epuises));
        } catch (SQLException e) {
            System.err.println(
                    "Erreur stats medicaments : " + e.getMessage());
        }
    }

    private void chargerStatsEquipements() {
        try {
            List<equipements> list = equipementService.afficher();
            long total       = list.size();
            long dispo       = list.stream()
                    .filter(e -> "disponible"
                            .equalsIgnoreCase(e.getEtat())).count();
            long maintenance = list.stream()
                    .filter(e -> "en_maintenance"
                            .equalsIgnoreCase(e.getEtat())).count();
            setText(kpiEqTotalVal, String.valueOf(total));
            setText(kpiEqMaintVal, String.valueOf(maintenance));
            setText(eqTotalMini,   String.valueOf(total));
            setText(eqDispoMini,   String.valueOf(dispo));
            setText(eqMaintMini,   String.valueOf(maintenance));
        } catch (SQLException e) {
            System.err.println(
                    "Erreur stats equipements : " + e.getMessage());
        }
    }

    // =========================================================
    //  CHATBOT
    // =========================================================

    @FXML
    public void toggleChatbot() {
        if (chatbotPanel == null) return;
        boolean visible = !chatbotPanel.isVisible();
        chatbotPanel.setVisible(visible);
        chatbotPanel.setManaged(visible);
        if (visible && chatMessages != null
                && chatMessages.getItems().isEmpty()) {
            chatMessages.getItems().add(
                    "Bonjour ! Je suis l'assistant VITA. "
                            + "Comment puis-je vous aider ?");
        }
    }

    @FXML
    public void envoyerMessage() {
        if (chatInput == null || chatInput.getText().isBlank()) return;

        String question = chatInput.getText().trim();
        if (chatMessages != null)
            chatMessages.getItems().add("Vous : " + question);
        chatInput.clear();
        if (chatMessages != null)
            chatMessages.scrollTo(chatMessages.getItems().size() - 1);

        chatInput.setDisable(true);

        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                return ChatbotService.repondre(question);
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            if (chatMessages != null)
                chatMessages.getItems().add("VITA : " + task.getValue());
            chatInput.setDisable(false);
            chatInput.requestFocus();
            if (chatMessages != null)
                chatMessages.scrollTo(
                        chatMessages.getItems().size() - 1);
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            if (chatMessages != null)
                chatMessages.getItems().add(
                        "VITA : [Erreur] Service indisponible.");
            chatInput.setDisable(false);
        }));

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    // =========================================================
    //  NAVIGATION
    // =========================================================

    @FXML
    public void ouvrirLoginAdmin() {
        ouvrirFenetre("/LoginAdmin-view.fxml",
                "Espace Administrateur VITA", 500, 400);
    }

    @FXML
    private void abrirMedicaments() { // fxml utilise "ouvrirMedicaments" dans votre controlleur
        ouvrirMedicaments();
    }

    @FXML
    private void ouvrirMedicaments() {
        ouvrirFenetre("/AjouterMedicament.fxml",
                "Gestion des Medicaments VITA", 1150, 740);
    }

    @FXML
    private void ouvrirEquipements() {
        ouvrirFenetre("/AjouterEquippement.fxml",
                "Gestion des Equipements VITA", 1150, 740);
    }

    @FXML
    private void ouvrirUserMedicaments() {
        ouvrirFenetre("/AjouterMedicament.fxml",
                "Medicaments Espace Utilisateur", 1200, 750);
    }

    @FXML
    private void ouvrirUserEquipements() {
        ouvrirFenetre("/AjouterEquippement.fxml",
                "Equipements Espace Utilisateur", 1150, 740);
    }

    @FXML private void ouvrirQuizz()                    { System.out.println("Quizz..."); }
    @FXML private void ouvrirEvenements()               { System.out.println("Evenements..."); }
    @FXML private void ouvrirPosts(ActionEvent event)   {}
    @FXML private void ouvrirCommentaires(ActionEvent event) {}

    @FXML
    private void deconnecter() {
        Alert confirm = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Voulez-vous vraiment vous deconnecter ?",
                ButtonType.OK, ButtonType.CANCEL);
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK && userNameLabel != null)
                ((Stage) userNameLabel.getScene().getWindow()).close();
        });
    }

    // =========================================================
    //  ouvrirFenetre
    // =========================================================

    private void ouvrirFenetre(String fxmlPath, String titre,
                               double width, double height) {
        try {
            URL res = getClass().getResource(fxmlPath);
            if (res == null) {
                montrerAlerte("Erreur",
                        "Fichier FXML introuvable : " + fxmlPath,
                        Alert.AlertType.ERROR);
                return;
            }

            FXMLLoader loader = new FXMLLoader(res);
            Parent root = loader.load();

            Scene scene = new Scene(root, width, height);

            URL cssUrl = getClass().getResource("/styles/style.css");
            if (cssUrl != null)
                scene.getStylesheets().add(cssUrl.toExternalForm());

            Stage stage = new Stage();
            stage.setTitle(titre);
            stage.setScene(scene);
            stage.initModality(Modality.NONE);
            stage.centerOnScreen();
            stage.setOnHidden(e -> actualiserStats());
            stage.show();

        } catch (IOException e) {
            montrerAlerte("Erreur de navigation",
                    "Detail : " + e.getMessage(),
                    Alert.AlertType.ERROR);
        }
    }

    // =========================================================
    //  UTILITAIRES
    // =========================================================

    private void setText(Label label, String text) {
        if (label != null) label.setText(text);
    }

    private void montrerAlerte(String titre, String contenu,
                               Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(contenu);
        alert.showAndWait();
    }
    @FXML private ToggleButton btnVueMedicaments;
    @FXML private ToggleButton btnVueEquipements;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterStatut;
    @FXML private ScrollPane scrollCenter;
    @FXML private ScrollPane scrollDetail;
    @FXML private VBox detailContent;
    @FXML private Label lblTotal;
    @FXML private Label lblStatus;

    @FXML
    private void switchToMedicaments() {
        lblTotal.setText("0 médicaments");
    }

    @FXML
    private void switchToEquipements() {
        lblTotal.setText("0 équipements");
    }

    @FXML
    private void ouvrirAjoutPopup() {
        // ouvrir formulaire d'ajout
    }
}