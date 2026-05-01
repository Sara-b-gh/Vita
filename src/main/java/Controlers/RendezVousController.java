package Controlers;

import Entites.RendezVous;
import Services.RendezVousCRUD;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class RendezVousController implements Initializable {

    @FXML private GridPane gridView;
    @FXML private ListView<RendezVous> listView;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterStatut;
    @FXML private Label lblTotal, lblStatus;
    @FXML private Label lblDetailPatient, lblDetailDate, lblDetailMotif, lblDetailLieu, lblDetailStatut;
    @FXML private VBox detailPane;

    private final RendezVousCRUD crud = new RendezVousCRUD();
    private List<RendezVous> allRdvs;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    @FXML
    private void ouvrirCompteRendus() {
        RendezVous selected = listView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            lblStatus.setText("Sélectionnez un rendez-vous.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ConsulterCompteRendu.fxml"));
            Parent root = loader.load();

            AfficherCompteRenduController ctrl = loader.getController();
            ctrl.setRendezVous(selected); // filter CRs by this RDV

            Stage stage = new Stage();
            stage.setTitle("Comptes Rendus — RDV #" + selected.getId_rdv()
                    + "  " + selected.getMotif());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadData(); // refresh RDV list after returning
        } catch (Exception e) {
            lblStatus.setText("Erreur : " + e.getMessage());
        }
    }
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        filterStatut.getItems().addAll("Tous", "planifie", "confirme", "annule", "termine");
        filterStatut.setValue("Tous");

        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(RendezVous rv, boolean empty) {
                super.updateItem(rv, empty);
                if (empty || rv == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(rv.getMotif() + "   " + (rv.getDate_rdv() != null ? rv.getDate_rdv().format(FMT) : "—"));
                    setStyle("-fx-padding: 6 10; -fx-font-size: 12px;");
                }
            }
        });

        listView.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) showDetails(selected);
        });

        searchField.textProperty().addListener((obs, o, n) -> applyFilters());
        filterStatut.valueProperty().addListener((obs, o, n) -> applyFilters());

        loadData();
    }

    public void loadData() {
        try {
            allRdvs = crud.afficher();
            applyFilters();
        } catch (SQLException e) {
            lblStatus.setText("Erreur chargement : " + e.getMessage());
        }
    }

    private void applyFilters() {
        String search = searchField.getText().toLowerCase().trim();
        String statut = filterStatut.getValue();

        List<RendezVous> filtered = allRdvs.stream()
                .filter(rv -> {
                    boolean matchSearch = search.isEmpty()
                            || (rv.getMotif() != null && rv.getMotif().toLowerCase().contains(search))
                            || (rv.getLieu()  != null && rv.getLieu().toLowerCase().contains(search))
                            || String.valueOf(rv.getPatient_id()).contains(search);
                    boolean matchStatut = "Tous".equals(statut) || rv.getStatut().equals(statut);
                    return matchSearch && matchStatut;
                })
                .collect(Collectors.toList());

        buildGrid(filtered);
        listView.getItems().setAll(filtered);
        lblTotal.setText(filtered.size() + " rendez-vous");
    }

    // ─── Grid ────────────────────────────────────────────────────────────
    private void buildGrid(List<RendezVous> list) {
        gridView.getChildren().clear();
        int col = 0, row = 0;
        final int COLS = 2;

        for (RendezVous rv : list) {
            VBox card = buildCard(rv);
            gridView.add(card, col, row);
            GridPane.setHgrow(card, Priority.ALWAYS);
            col++;
            if (col >= COLS) { col = 0; row++; }
        }
    }

    private VBox buildCard(RendezVous rv) {
        VBox card = new VBox(6);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setStyle(
                "-fx-border-color: #DDDDDD; -fx-border-width: 1; " +
                        "-fx-border-radius: 6; -fx-background-radius: 6; " +
                        "-fx-background-color: #FFFFFF; -fx-padding: 12 14;"
        );

        // Date + statut
        Label lblDate = new Label(rv.getDate_rdv() != null ? rv.getDate_rdv().format(FMT) : "—");
        lblDate.setStyle("-fx-font-size: 11px; -fx-text-fill: #888888;");

        Label lblStatut = new Label(rv.getStatut());
        lblStatut.setStyle("-fx-font-size: 10px; -fx-text-fill: #888888; " +
                "-fx-border-color: #CCCCCC; -fx-border-radius: 4; " +
                "-fx-border-width: 1; -fx-padding: 2 6;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox top = new HBox(8, lblDate, spacer, lblStatut);
        top.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // Motif
        Label lblMotif = new Label(rv.getMotif() != null ? rv.getMotif() : "—");
        lblMotif.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #222222;");
        lblMotif.setWrapText(true);

        // Patient · Médecin
        Label lblPerson = new Label("Patient #" + rv.getPatient_id() + "  ·  Dr #" + rv.getMedecin_id());
        lblPerson.setStyle("-fx-font-size: 11px; -fx-text-fill: #AAAAAA;");

        card.getChildren().addAll(top, lblMotif, lblPerson);

        if (rv.getLieu() != null && !rv.getLieu().isEmpty()) {
            Label lblLieu = new Label(rv.getLieu());
            lblLieu.setStyle("-fx-font-size: 11px; -fx-text-fill: #AAAAAA;");
            card.getChildren().add(lblLieu);
        }

        // Hover
        card.setOnMouseEntered(e ->
                card.setStyle(card.getStyle().replace("#FFFFFF", "#F9F9F9")));
        card.setOnMouseExited(e ->
                card.setStyle(card.getStyle().replace("#F9F9F9", "#FFFFFF")));

        card.setOnMouseClicked(e -> {
            listView.getSelectionModel().select(rv);
            listView.scrollTo(rv);
        });

        return card;
    }

    // ─── Details ─────────────────────────────────────────────────────────
    private void showDetails(RendezVous rv) {
        detailPane.setVisible(true);
        lblDetailPatient.setText("Patient #" + rv.getPatient_id() + "  ·  Dr #" + rv.getMedecin_id());
        lblDetailDate.setText(rv.getDate_rdv() != null ? rv.getDate_rdv().format(FMT) : "—");
        lblDetailMotif.setText(rv.getMotif() != null ? rv.getMotif() : "—");
        lblDetailLieu.setText(rv.getLieu() != null && !rv.getLieu().isEmpty() ? rv.getLieu() : "—");
        lblDetailStatut.setText(rv.getStatut().toUpperCase());
    }

    // ─── Actions ─────────────────────────────────────────────────────────
    @FXML
    private void ouvrirAjout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AjouterRdv.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Nouveau Rendez-Vous");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadData();
        } catch (Exception e) {
            lblStatus.setText("Erreur : " + e.getMessage());
        }
    }
    @FXML
    private void ConsulterComptes() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ConsulterCompteRendu.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Comptes rendus");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadData();
        } catch (Exception e) {
            lblStatus.setText("Erreur : " + e.getMessage());
        }
    }

    @FXML
    private void ouvrirModification() {
        RendezVous selected = listView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            lblStatus.setText("Sélectionnez un rendez-vous à modifier.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ModifierRdv.fxml"));
            Parent root = loader.load();
            ModifierRdvController ctrl = loader.getController();
            ctrl.setRendezVous(selected);
            Stage stage = new Stage();
            stage.setTitle("Modifier le Rendez-Vous");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadData();
        } catch (Exception e) {
            lblStatus.setText("Erreur : " + e.getMessage());
        }
    }

    @FXML
    private void supprimerRdv() {
        RendezVous selected = listView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            lblStatus.setText("Sélectionnez un rendez-vous à supprimer.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer ce rendez-vous ?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    crud.supprimer(selected.getId_rdv());
                    detailPane.setVisible(false);
                    loadData();
                    lblStatus.setText("Rendez-vous supprimé.");
                } catch (SQLException e) {
                    lblStatus.setText("Erreur suppression : " + e.getMessage());
                }
            }
        });
    }
}