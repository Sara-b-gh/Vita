package Controlers;

import Entites.CompteRendu;
import Entites.RendezVous;
import Services.CompteRenduCRUD;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Scene;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class AfficherCompteRenduController {

    @FXML private ListView<CompteRendu> listView;
    @FXML private Label lblId, lblRdv, lblRedigePar, lblDiagnostic,
            lblContenu, lblTraitement, lblProchainRdv,
            lblConfidentiel, lblDateCreation;

    private final CompteRenduCRUD service = new CompteRenduCRUD();
    private final ObservableList<CompteRendu> data = FXCollections.observableArrayList();
    private RendezVous currentRdv = null; // null = show all

    /** Called by RendezVousController before the window opens. */
    public void setRendezVous(RendezVous rdv) {
        this.currentRdv = rdv;
        chargerListe(); // reload filtered
    }

    @FXML
    public void initialize() {
        chargerListe();

        listView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (newVal != null) afficherDetail(newVal);
                }
        );

        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(CompteRendu cr, boolean empty) {
                super.updateItem(cr, empty);
                if (empty || cr == null) setText(null);
                else setText("CR #" + cr.getId_cr()
                        + " — RDV: " + cr.getId_rdv()
                        + " — " + cr.getDiagnostic());
            }
        });
    }

    private void chargerListe() {
        try {
            data.clear();
            List<CompteRendu> all = service.afficher();

            if (currentRdv != null) {
                all.removeIf(cr -> cr.getId_rdv() != currentRdv.getId_rdv());
            }

            data.addAll(all);
            listView.setItems(data);
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Erreur chargement : " + e.getMessage()).show();
        }
    }

    private void afficherDetail(CompteRendu cr) {
        lblId.setText(String.valueOf(cr.getId_cr()));
        lblRdv.setText(String.valueOf(cr.getId_rdv()));
        lblRedigePar.setText(String.valueOf(cr.getRedige_par()));
        lblDiagnostic.setText(cr.getDiagnostic() != null ? cr.getDiagnostic() : "—");
        lblContenu.setText(cr.getContenu() != null ? cr.getContenu() : "—");
        lblTraitement.setText(cr.getTraitement() != null ? cr.getTraitement() : "—");
        lblProchainRdv.setText(cr.getProchain_rdv() != null ? cr.getProchain_rdv().toString() : "—");
        lblConfidentiel.setText(cr.isConfidentiel() ? "Oui" : "Non");
        lblDateCreation.setText(cr.getDate_creation() != null ? cr.getDate_creation().toString() : "—");
    }

    @FXML
    private void handleSupprimer() {
        CompteRendu selected = listView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            new Alert(Alert.AlertType.WARNING, "Sélectionnez un compte rendu.").show();
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer CR #" + selected.getId_cr() + " ?");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    service.supprimer(selected.getId_cr());
                    chargerListe();
                    viderDetail();
                } catch (SQLException e) {
                    new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
                }
            }
        });
    }

    @FXML
    private void handleModifier() {
        CompteRendu cr = listView.getSelectionModel().getSelectedItem();
        if (cr == null) {
            new Alert(Alert.AlertType.WARNING, "Sélectionnez un compte rendu.").show();
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ModifierCompteRendu.fxml"));
            Parent root = loader.load();

            ModifierCompteRenduController ctrl = loader.getController();
            ctrl.initData(cr, this::chargerListe);

            Stage stage = new Stage();
            stage.setTitle("Modifier CR #" + cr.getId_cr());
            stage.setScene(new Scene(root, 450, 380));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();

        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR,
                    "Impossible d'ouvrir la fenêtre : " + e.getMessage()).show();
        }
    }

    @FXML
    private void handleActualiser() {
        chargerListe();
    }

    @FXML
    void handleAjouter(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/AjouterCompteRendu.fxml"));
            Parent root = loader.load();

            CompteRenduController ctrl = loader.getController();
            ctrl.setOnSuccess(this::chargerListe);

            // Pre-fill and lock the RDV id if opened from a specific RDV
            if (currentRdv != null) {
                ctrl.setRdvId(currentRdv.getId_rdv());
            }

            Stage stage = new Stage();
            stage.setTitle("Ajouter Compte Rendu"
                    + (currentRdv != null ? " — RDV #" + currentRdv.getId_rdv() : ""));
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void ConsulterRDV() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AfficherRendezVous.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Rendez-vous");
            stage.setScene(new Scene(root));
            stage.show();

            Stage currentStage = (Stage) listView.getScene().getWindow();
            currentStage.close();
        } catch (Exception e) {
            lblContenu.setText("Erreur : " + e.getMessage());
        }
    }
    private void viderDetail() {
        lblId.setText("");      lblRdv.setText("");         lblRedigePar.setText("");
        lblDiagnostic.setText(""); lblContenu.setText(""); lblTraitement.setText("");
        lblProchainRdv.setText(""); lblConfidentiel.setText(""); lblDateCreation.setText("");
    }
}