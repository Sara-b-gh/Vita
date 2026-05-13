package controles;

import entities.Evenn;
import entities.ReservationPersonne;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import services.QRCodeService;
import services.ServiceEvenn;
import services.ServiceReservationPersonne;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class ReservationPanelController {

    // ===== FXML DECLARATIONS =====
    @FXML private Label evenementLabel;
    @FXML private ComboBox<String> statutFilter;
    @FXML private ListView<ReservationPersonne> reservationListView;
    @FXML private Label statAttente;
    @FXML private Label statAccepte;
    @FXML private Label statRefuse;

    private final ServiceReservationPersonne srp = new ServiceReservationPersonne();
    private final ServiceEvenn se = new ServiceEvenn();
    private final ObservableList<ReservationPersonne> reservations = FXCollections.observableArrayList();
    private Evenn evenement;
    private Runnable onRefreshCallback;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public void setEvenement(Evenn evenement, Runnable onRefresh) {
        this.evenement = evenement;
        this.onRefreshCallback = onRefresh;
        if (evenementLabel != null) {
            evenementLabel.setText("Événement : " + evenement.getTitre());
        }
        chargerReservations();
    }

    @FXML
    public void initialize() {
        // Configuration du filtre
        if (statutFilter != null) {
            statutFilter.setItems(FXCollections.observableArrayList("TOUS", "EN_ATTENTE", "ACCEPTE", "REFUSE"));
            statutFilter.setValue("EN_ATTENTE");
            statutFilter.valueProperty().addListener((a, b, c) -> filtrerReservations());
        }

        // Configuration de la liste des réservations
        reservationListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ReservationPersonne r, boolean empty) {
                super.updateItem(r, empty);
                if (empty || r == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                VBox card = new VBox(10);
                card.setPadding(new Insets(14));
                card.setStyle(getCardStyle(r.getStatut()));
                card.setMaxWidth(Double.MAX_VALUE);

                // En-tête avec nom et statut
                HBox header = new HBox(10);
                header.setAlignment(Pos.CENTER_LEFT);

                Label nameLabel = new Label(r.getNomComplet());
                nameLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
                nameLabel.setTextFill(Color.web("#1e1e2e"));

                Label statutLabel = new Label(getStatutText(r.getStatut()));
                statutLabel.setStyle(getStatutStyle(r.getStatut()));

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                header.getChildren().addAll(nameLabel, spacer, statutLabel);

                // Informations de contact
                VBox infoBox = new VBox(4);
                Label emailLabel = new Label("✉️ " + r.getEmail());
                emailLabel.setFont(Font.font("Segoe UI", 11));
                emailLabel.setTextFill(Color.web("#64748b"));

                Label phoneLabel = new Label("📞 " + (r.getTelephone() != null && !r.getTelephone().isEmpty() ? r.getTelephone() : "Non renseigné"));
                phoneLabel.setFont(Font.font("Segoe UI", 11));
                phoneLabel.setTextFill(Color.web("#64748b"));

                Label dateLabel = new Label("📅 Réservé le : " + r.getDateReservation().format(FMT));
                dateLabel.setFont(Font.font("Segoe UI", 11));
                dateLabel.setTextFill(Color.web("#64748b"));

                infoBox.getChildren().addAll(emailLabel, phoneLabel, dateLabel);

                // Commentaire
                VBox commentBox = null;
                if (r.getCommentaire() != null && !r.getCommentaire().isBlank()) {
                    commentBox = new VBox(4);
                    Label commentTitle = new Label("💬 Commentaire :");
                    commentTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
                    commentTitle.setTextFill(Color.web("#5A1730"));

                    Label commentText = new Label(r.getCommentaire());
                    commentText.setFont(Font.font("Segoe UI", 11));
                    commentText.setTextFill(Color.web("#64748b"));
                    commentText.setWrapText(true);
                    commentText.setStyle("-fx-background-color: #fdf5f7; -fx-padding: 8; -fx-background-radius: 8;");

                    commentBox.getChildren().addAll(commentTitle, commentText);
                }

                // Boutons d'action
                HBox actionsBox = new HBox(8);
                actionsBox.setAlignment(Pos.CENTER_RIGHT);
                actionsBox.setPadding(new Insets(8, 0, 0, 0));

                if ("EN_ATTENTE".equals(r.getStatut())) {
                    Button accepterBtn = new Button("✅ Accepter");
                    accepterBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 6 12; -fx-font-size: 11px; -fx-cursor: hand;");
                    accepterBtn.setOnAction(e -> accepterReservation(r));

                    Button refuserBtn = new Button("❌ Refuser");
                    refuserBtn.setStyle("-fx-background-color: #c1283e; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 6 12; -fx-font-size: 11px; -fx-cursor: hand;");
                    refuserBtn.setOnAction(e -> refuserReservation(r));

                    actionsBox.getChildren().addAll(accepterBtn, refuserBtn);
                } else {
                    Label infoLabel = new Label(r.getStatut().equals("ACCEPTE") ? "✓ Réservation acceptée" : "✗ Réservation refusée");
                    infoLabel.setStyle(r.getStatut().equals("ACCEPTE")
                            ? "-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-font-size: 11px;"
                            : "-fx-text-fill: #c1283e; -fx-font-weight: bold; -fx-font-size: 11px;");
                    actionsBox.getChildren().add(infoLabel);
                }

                Button supprimerBtn = new Button("🗑 Supprimer");
                supprimerBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-border-color: #e2e8f0; -fx-border-radius: 6; -fx-padding: 6 12; -fx-font-size: 11px; -fx-cursor: hand;");
                supprimerBtn.setOnAction(e -> supprimerReservation(r));
                actionsBox.getChildren().add(supprimerBtn);

                // Bouton QR Code
                Button qrBtn = new Button("📱 QR Code");
                qrBtn.setStyle("-fx-background-color: #6b1a2a; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 6 12; -fx-font-size: 11px; -fx-cursor: hand;");
                qrBtn.setOnAction(e -> afficherQRCodeReservation(r));
                actionsBox.getChildren().add(qrBtn);

                card.getChildren().addAll(header, infoBox);
                if (commentBox != null) {
                    card.getChildren().add(commentBox);
                }
                card.getChildren().add(actionsBox);

                setGraphic(card);
            }
        });
    }

    private String getCardStyle(String statut) {
        String baseStyle = "-fx-background-color: white; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-width: 1px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 5, 0, 0, 1);";
        switch (statut) {
            case "ACCEPTE":
                return baseStyle + " -fx-border-color: #27ae60;";
            case "REFUSE":
                return baseStyle + " -fx-border-color: #c1283e;";
            default:
                return baseStyle + " -fx-border-color: #f39c12;";
        }
    }

    private String getStatutText(String statut) {
        switch (statut) {
            case "ACCEPTE":
                return "✓ Accepté";
            case "REFUSE":
                return "✗ Refusé";
            default:
                return "⏳ En attente";
        }
    }

    private String getStatutStyle(String statut) {
        switch (statut) {
            case "ACCEPTE":
                return "-fx-background-color: #d5f5e3; -fx-text-fill: #27ae60; -fx-padding: 4 10; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;";
            case "REFUSE":
                return "-fx-background-color: #f5d5dc; -fx-text-fill: #c1283e; -fx-padding: 4 10; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;";
            default:
                return "-fx-background-color: #fef5e7; -fx-text-fill: #f39c12; -fx-padding: 4 10; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;";
        }
    }

    private void chargerReservations() {
        if (evenement == null) return;
        try {
            List<ReservationPersonne> list = srp.getByEvenement(evenement.getId_Evenn());
            reservations.setAll(list);
            mettreAJourStatistiques();
            filtrerReservations();
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les réservations : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void mettreAJourStatistiques() {
        long attente = reservations.stream().filter(r -> "EN_ATTENTE".equals(r.getStatut())).count();
        long accepte = reservations.stream().filter(r -> "ACCEPTE".equals(r.getStatut())).count();
        long refuse = reservations.stream().filter(r -> "REFUSE".equals(r.getStatut())).count();

        if (statAttente != null) {
            statAttente.setText("⏳ En attente: " + attente);
        }
        if (statAccepte != null) {
            statAccepte.setText("✅ Acceptés: " + accepte);
        }
        if (statRefuse != null) {
            statRefuse.setText("❌ Refusés: " + refuse);
        }
    }

    @FXML
    private void filtrerReservations() {
        String filter = statutFilter.getValue();
        if ("TOUS".equals(filter)) {
            reservationListView.setItems(FXCollections.observableArrayList(reservations));
        } else {
            List<ReservationPersonne> filtered = reservations.stream()
                    .filter(r -> r.getStatut().equals(filter))
                    .collect(Collectors.toList());
            reservationListView.setItems(FXCollections.observableArrayList(filtered));
        }
    }

    private void accepterReservation(ReservationPersonne r) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Accepter la réservation");
        confirm.setContentText("Voulez-vous accepter la réservation de " + r.getNomComplet() + " ?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    srp.updateStatut(r.getId(), "ACCEPTE");
                    chargerReservations();
                    if (onRefreshCallback != null) {
                        onRefreshCallback.run();
                    }
                    showAlert("Succès", "Réservation acceptée pour " + r.getNomComplet(), Alert.AlertType.INFORMATION);
                } catch (SQLException e) {
                    showAlert("Erreur", "Erreur : " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    private void refuserReservation(ReservationPersonne r) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Refuser la réservation");
        confirm.setContentText("Voulez-vous refuser la réservation de " + r.getNomComplet() + " ?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    srp.updateStatut(r.getId(), "REFUSE");
                    chargerReservations();
                    if (onRefreshCallback != null) {
                        onRefreshCallback.run();
                    }
                    showAlert("Succès", "Réservation refusée pour " + r.getNomComplet(), Alert.AlertType.INFORMATION);
                } catch (SQLException e) {
                    showAlert("Erreur", "Erreur : " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    private void supprimerReservation(ReservationPersonne r) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer la réservation");
        confirm.setContentText("Voulez-vous supprimer définitivement la réservation de " + r.getNomComplet() + " ?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    srp.delete(r);
                    chargerReservations();
                    if (onRefreshCallback != null) {
                        onRefreshCallback.run();
                    }
                    showAlert("Succès", "Réservation supprimée", Alert.AlertType.INFORMATION);
                } catch (SQLException e) {
                    showAlert("Erreur", "Erreur : " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    private void afficherQRCodeReservation(ReservationPersonne r) {
        Stage stage = new Stage();
        stage.setTitle("QR Code - " + r.getNomComplet());

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: white; -fx-border-radius: 15;");
        root.setAlignment(Pos.CENTER);

        ImageView qrImage = QRCodeService.generateReservationQRCode(r.getId());
        if (qrImage != null) {
            qrImage.setFitWidth(250);
            qrImage.setFitHeight(250);
            root.getChildren().add(qrImage);
        }

        Label nameLabel = new Label(r.getNomComplet());
        nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #5A1730;");

        Label eventLabel = new Label("Réservation #" + r.getId());
        eventLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #A36277;");

        Label instruction = new Label("Présentez ce QR code à l'entrée de l'événement");
        instruction.setStyle("-fx-font-size: 11px; -fx-text-fill: #B78A98;");

        Button btnFermer = new Button("Fermer");
        btnFermer.setOnAction(e -> stage.close());

        root.getChildren().addAll(nameLabel, eventLabel, instruction, btnFermer);

        Scene scene = new Scene(root, 350, 450);
        stage.setScene(scene);
        stage.show();
    }

    @FXML
    private void accepterTout() {
        List<ReservationPersonne> pending = reservations.stream()
                .filter(r -> "EN_ATTENTE".equals(r.getStatut()))
                .collect(Collectors.toList());
        if (pending.isEmpty()) {
            showAlert("Info", "Aucune réservation en attente", Alert.AlertType.INFORMATION);
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Accepter toutes les réservations");
        confirm.setContentText("Voulez-vous accepter les " + pending.size() + " réservation(s) en attente ?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    for (ReservationPersonne r : pending) {
                        srp.updateStatut(r.getId(), "ACCEPTE");
                    }
                    chargerReservations();
                    if (onRefreshCallback != null) {
                        onRefreshCallback.run();
                    }
                    showAlert("Succès", pending.size() + " réservation(s) acceptée(s)", Alert.AlertType.INFORMATION);
                } catch (SQLException e) {
                    showAlert("Erreur", "Erreur : " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    @FXML
    private void refuserTout() {
        List<ReservationPersonne> pending = reservations.stream()
                .filter(r -> "EN_ATTENTE".equals(r.getStatut()))
                .collect(Collectors.toList());
        if (pending.isEmpty()) {
            showAlert("Info", "Aucune réservation en attente", Alert.AlertType.INFORMATION);
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Refuser toutes les réservations");
        confirm.setContentText("Voulez-vous refuser les " + pending.size() + " réservation(s) en attente ?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    for (ReservationPersonne r : pending) {
                        srp.updateStatut(r.getId(), "REFUSE");
                    }
                    chargerReservations();
                    if (onRefreshCallback != null) {
                        onRefreshCallback.run();
                    }
                    showAlert("Succès", pending.size() + " réservation(s) refusée(s)", Alert.AlertType.INFORMATION);
                } catch (SQLException e) {
                    showAlert("Erreur", "Erreur : " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    @FXML
    private void exporterCSV() {
        Stage stage = (Stage) reservationListView.getScene().getWindow();
        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter les réservations");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier CSV", "*.csv"));
        fc.setInitialFileName("reservations_" + evenement.getTitre().replaceAll("\\s+", "_") + "_" + LocalDate.now() + ".csv");

        File file = fc.showSaveDialog(stage);
        if (file == null) return;

        try (FileWriter fw = new FileWriter(file)) {
            fw.write("ID;Nom complet;Email;Téléphone;Date réservation;Statut;Commentaire\n");
            for (ReservationPersonne r : reservations) {
                fw.write(r.getId() + ";" +
                        r.getNomComplet() + ";" +
                        r.getEmail() + ";" +
                        (r.getTelephone() != null ? r.getTelephone() : "") + ";" +
                        r.getDateReservation().format(FMT) + ";" +
                        r.getStatut() + ";" +
                        (r.getCommentaire() != null ? r.getCommentaire().replace(";", ",").replace("\n", " ") : "") + "\n");
            }
            showAlert("Export réussi", "Fichier CSV sauvegardé avec succès !", Alert.AlertType.INFORMATION);
        } catch (IOException e) {
            showAlert("Erreur", "Erreur d'export : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void fermer() {
        Stage stage = (Stage) reservationListView.getScene().getWindow();
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