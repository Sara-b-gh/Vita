package Controlers;

import Entites.CompteRendu;
import Services.CompteRenduCRUD;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Scene;

import java.sql.SQLException;
import java.util.List;

public class AfficherCompteRenduController {

    @FXML private ListView<CompteRendu> listView;
    @FXML private Label lblId, lblRdv, lblRedigePar, lblDiagnostic,
            lblContenu, lblTraitement, lblProchainRdv,
            lblConfidentiel, lblDateCreation;

    private CompteRenduCRUD service = new CompteRenduCRUD();
    private ObservableList<CompteRendu> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        chargerListe();

        // Afficher le détail dans le GridPane quand on clique sur un élément
        listView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (newVal != null) afficherDetail(newVal);
                }
        );

        // Affichage du texte dans la ListView
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(CompteRendu cr, boolean empty) {
                super.updateItem(cr, empty);
                if (empty || cr == null) setText(null);
                else setText("CR #" + cr.getId_cr() + " — RDV: " + cr.getId_rdv() + " — " + cr.getDiagnostic());
            }
        });
    }

    private void chargerListe() {
        try {
            data.clear();
            data.addAll(service.afficher());
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
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer CR #" + selected.getId_cr() + " ?");
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
        ouvrirModification(cr);
    }

    @FXML
    private void handleActualiser() {
        chargerListe();
    }

    private void ouvrirModification(CompteRendu cr) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Modifier CR #" + cr.getId_cr());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setStyle("-fx-padding: 20;");

        TextField tfDiagnostic   = new TextField(cr.getDiagnostic());
        TextArea  taContenu      = new TextArea(cr.getContenu());
        TextArea  taTraitement   = new TextArea(cr.getTraitement());
        DatePicker dpProchainRdv = new DatePicker(cr.getProchain_rdv());
        CheckBox  cbConfidentiel = new CheckBox("Confidentiel");
        cbConfidentiel.setSelected(cr.isConfidentiel());

        grid.add(new Label("Diagnostic :"),   0, 0); grid.add(tfDiagnostic,   1, 0);
        grid.add(new Label("Contenu :"),      0, 1); grid.add(taContenu,      1, 1);
        grid.add(new Label("Traitement :"),   0, 2); grid.add(taTraitement,   1, 2);
        grid.add(new Label("Prochain RDV :"), 0, 3); grid.add(dpProchainRdv,  1, 3);
        grid.add(cbConfidentiel,              1, 4);

        Button btnSave = new Button("Enregistrer");
        Button btnAnnuler = new Button("Annuler");
        HBox footer = new HBox(10, btnSave, btnAnnuler);
        grid.add(footer, 1, 5);

        btnAnnuler.setOnAction(e -> stage.close());
        btnSave.setOnAction(e -> {
            try {
                cr.setDiagnostic(tfDiagnostic.getText());
                cr.setContenu(taContenu.getText());
                cr.setTraitement(taTraitement.getText());
                cr.setProchain_rdv(dpProchainRdv.getValue());
                cr.setConfidentiel(cbConfidentiel.isSelected());
                service.modifier(cr);
                stage.close();
                chargerListe();
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, ex.getMessage()).show();
            }
        });

        stage.setScene(new Scene(grid, 420, 350));
        stage.show();
    }

    private void viderDetail() {
        lblId.setText(""); lblRdv.setText(""); lblRedigePar.setText("");
        lblDiagnostic.setText(""); lblContenu.setText(""); lblTraitement.setText("");
        lblProchainRdv.setText(""); lblConfidentiel.setText(""); lblDateCreation.setText("");
    }
}