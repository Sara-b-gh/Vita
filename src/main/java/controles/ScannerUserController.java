package controles;

import entities.RendezVous;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import services.QRCodeService;
import services.ServiceEvenn;

import java.sql.SQLException;

public class ScannerUserController {

    @FXML private TextArea scanInput;
    @FXML private VBox resultBox;
    @FXML private Label resultTitle;
    @FXML private Label resultDetail;
    @FXML private Button voirDetailsBtn;

    private final ServiceEvenn se = new ServiceEvenn();
    private RendezVous.Evenn evenementTrouve;
    private Runnable onEventFoundCallback;

    public void setOnEventFoundCallback(Runnable callback) {
        this.onEventFoundCallback = callback;
    }

    @FXML
    private void scanner() {
        String data = scanInput.getText().trim();
        if (data.isEmpty()) {
            showAlert("Erreur", "Veuillez entrer ou coller un QR code", Alert.AlertType.WARNING);
            return;
        }

        QRCodeService.QRData qrData = QRCodeService.parseQRCode(data);

        if (qrData == null) {
            showAlert("QR Code invalide", "Ce QR code n'est pas reconnu par le système", Alert.AlertType.ERROR);
            return;
        }

        if ("EVENT".equals(qrData.type)) {
            verifierEvenement(qrData.id);
        } else if ("CHECKIN".equals(qrData.type)) {
            afficherMessageCheckin();
        } else {
            showAlert("Erreur", "Type de QR code non reconnu", Alert.AlertType.ERROR);
        }
    }

    private void verifierEvenement(int eventId) {
        try {
            RendezVous.Evenn ev = se.getById(eventId);
            if (ev == null) {
                showAlert("Erreur", "Événement non trouvé", Alert.AlertType.ERROR);
                return;
            }

            evenementTrouve = ev;
            resultBox.setVisible(true);
            resultBox.setManaged(true);
            resultTitle.setText("📅 " + ev.getTitre());
            resultTitle.setStyle("-fx-text-fill: #8B1538; -fx-font-weight: bold; -fx-font-size: 14px;");

            String dateStr = ev.getDateEvenement() != null ? ev.getDateEvenement().toString() : "Date non définie";
            String lieuStr = ev.getLieu() != null ? ev.getLieu() : "Lieu non défini";
            String descStr = ev.getDescription() != null ?
                    (ev.getDescription().length() > 100 ? ev.getDescription().substring(0, 100) + "..." : ev.getDescription()) :
                    "Aucune description";

            resultDetail.setText(
                    "📅 Date: " + dateStr + "\n" +
                            "📍 Lieu: " + lieuStr + "\n" +
                            "📝 Description: " + descStr
            );
            resultDetail.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

            voirDetailsBtn.setVisible(true);
            voirDetailsBtn.setOnAction(e -> {
                fermer();
                if (onEventFoundCallback != null) {
                    onEventFoundCallback.run();
                }
                afficherDetailsComplets(ev);
            });

        } catch (SQLException e) {
            showAlert("Erreur", "Erreur base de données: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void afficherMessageCheckin() {
        resultBox.setVisible(true);
        resultBox.setManaged(true);
        resultTitle.setText("🎟️ Code de présence");
        resultTitle.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
        resultDetail.setText("Ce QR code est pour valider votre présence à l'événement.\nPrésentez-le à l'entrée.");
        voirDetailsBtn.setVisible(false);
    }

    private void afficherDetailsComplets(RendezVous.Evenn ev) {
        Stage stage = new Stage();
        stage.setTitle("Détails - " + ev.getTitre());
        stage.initModality(Modality.APPLICATION_MODAL);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: white; -fx-border-radius: 15;");

        Label title = new Label(ev.getTitre());
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #5A1730;");

        Label dateLabel = new Label("📅 Date: " + (ev.getDateEvenement() != null ? ev.getDateEvenement().toString() : "Non définie"));
        dateLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");

        Label lieuLabel = new Label("📍 Lieu: " + (ev.getLieu() != null ? ev.getLieu() : "Non défini"));
        lieuLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");

        Label descLabel = new Label("📝 Description: " + (ev.getDescription() != null ? ev.getDescription() : "Aucune description"));
        descLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");
        descLabel.setWrapText(true);

        Button btnFermer = new Button("Fermer");
        btnFermer.setStyle("-fx-background-color: #8B1538; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16; -fx-cursor: hand;");
        btnFermer.setOnAction(e -> stage.close());

        root.getChildren().addAll(title, dateLabel, lieuLabel, descLabel, btnFermer);

        Scene scene = new Scene(root, 400, 350);
        stage.setScene(scene);
        stage.show();
    }

    @FXML
    private void effacer() {
        scanInput.clear();
        resultBox.setVisible(false);
        resultBox.setManaged(false);
        resultTitle.setText("");
        resultDetail.setText("");
        evenementTrouve = null;
    }

    @FXML
    private void fermer() {
        Stage stage = (Stage) scanInput.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
