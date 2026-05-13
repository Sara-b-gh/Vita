package controles;

import entities.RendezVous;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import services.QRCodeService;
import services.ServiceEvenn;
import services.ServiceReservationPersonne;

import java.sql.SQLException;

public class ScannerQRCodeController {

    @FXML private TextArea scanInput;
    @FXML private VBox resultBox;
    @FXML private Label resultTitle;
    @FXML private Label resultDetail;
    @FXML private Button confirmBtn;

    private final ServiceReservationPersonne srp = new ServiceReservationPersonne();
    private final ServiceEvenn se = new ServiceEvenn();
    private Runnable onCheckinCallback;

    public void setOnCheckinCallback(Runnable callback) {
        this.onCheckinCallback = callback;
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

        if ("CHECKIN".equals(qrData.type)) {
            verifierReservation(qrData.id);
        } else if ("EVENT".equals(qrData.type)) {
            verifierEvenement(qrData.id);
        }
    }

    private void verifierReservation(int reservationId) {
        try {
            RendezVous.ReservationPersonne r = srp.getById(reservationId);
            if (r == null) {
                showAlert("Erreur", "Réservation non trouvée", Alert.AlertType.ERROR);
                return;
            }

            resultBox.setVisible(true);
            resultBox.setManaged(true);

            if (r.isPresent()) {
                resultTitle.setText("⚠️ Déjà vérifié !");
                resultTitle.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                resultDetail.setText(r.getNomComplet() + " a déjà été enregistré(e) pour cet événement.");
                confirmBtn.setVisible(false);
            } else if ("ACCEPTE".equals(r.getStatut())) {
                resultTitle.setText("✅ Réservation valide !");
                resultTitle.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                resultDetail.setText(
                        "Nom: " + r.getNomComplet() + "\n" +
                                "Email: " + r.getEmail() + "\n" +
                                "Téléphone: " + (r.getTelephone() != null ? r.getTelephone() : "Non renseigné") + "\n" +
                                "ID Réservation: " + r.getId()
                );
                confirmBtn.setVisible(true);
                confirmBtn.setOnAction(e -> validerPresence(reservationId, r));
            } else if ("REFUSE".equals(r.getStatut())) {
                resultTitle.setText("❌ Réservation refusée !");
                resultTitle.setStyle("-fx-text-fill: #c1283e; -fx-font-weight: bold;");
                resultDetail.setText("La réservation de " + r.getNomComplet() + " a été refusée.");
                confirmBtn.setVisible(false);
            } else {
                resultTitle.setText("⏳ En attente !");
                resultTitle.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                resultDetail.setText("La réservation de " + r.getNomComplet() + " n'a pas encore été acceptée.");
                confirmBtn.setVisible(false);
            }
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur base de données: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void verifierEvenement(int eventId) {
        try {
            RendezVous.Evenn ev = se.getById(eventId);
            if (ev == null) {
                showAlert("Erreur", "Événement non trouvé", Alert.AlertType.ERROR);
                return;
            }

            resultBox.setVisible(true);
            resultBox.setManaged(true);
            resultTitle.setText("📅 Événement trouvé !");
            resultTitle.setStyle("-fx-text-fill: #8B1538; -fx-font-weight: bold;");
            resultDetail.setText(
                    "Titre: " + ev.getTitre() + "\n" +
                            "Date: " + ev.getDateEvenement() + "\n" +
                            "Lieu: " + ev.getLieu() + "\n\n" +
                            "Ce QR code est pour l'événement, pas pour une réservation."
            );
            confirmBtn.setVisible(false);
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur base de données: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void validerPresence(int reservationId, RendezVous.ReservationPersonne r) {
        try {
            srp.updatePresence(reservationId, true);
            showAlert("Succès", "✅ Présence validée pour " + r.getNomComplet(), Alert.AlertType.INFORMATION);

            resultTitle.setText("✅ Validé !");
            resultTitle.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
            resultDetail.setText(r.getNomComplet() + " est maintenant présent(e) à l'événement.");
            confirmBtn.setVisible(false);

            if (onCheckinCallback != null) {
                onCheckinCallback.run();
            }

            // Optionnel : vider le champ après validation
            scanInput.clear();

        } catch (SQLException e) {
            showAlert("Erreur", "Erreur lors de la validation: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void effacer() {
        scanInput.clear();
        resultBox.setVisible(false);
        resultBox.setManaged(false);
    }

    @FXML
    private void copierInfos() {
        if (resultDetail.getText() != null && !resultDetail.getText().isEmpty()) {
            ClipboardContent content = new ClipboardContent();
            content.putString(resultDetail.getText());
            Clipboard.getSystemClipboard().setContent(content);
            showAlert("Copié", "Informations copiées dans le presse-papier", Alert.AlertType.INFORMATION);
        }
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
