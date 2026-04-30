package Controlers;

import Entites.CompteRendu;
import Services.CompteRenduCRUD;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.SQLException;

public class ModifierCompteRenduController {

    @FXML private TextField  tfDiagnostic;
    @FXML private TextArea   taContenu;
    @FXML private TextArea   taTraitement;
    @FXML private DatePicker dpProchainRdv;
    @FXML private CheckBox   cbConfidentiel;

    private CompteRendu      compteRendu;
    private CompteRenduCRUD  service = new CompteRenduCRUD();
    private Runnable         onSuccess;   // callback → rafraîchit la liste parente

    public void initData(CompteRendu cr, Runnable onSuccess) {
        this.compteRendu = cr;
        this.onSuccess   = onSuccess;

        tfDiagnostic.setText(cr.getDiagnostic());
        taContenu.setText(cr.getContenu());
        taTraitement.setText(cr.getTraitement());
        dpProchainRdv.setValue(cr.getProchain_rdv());
        cbConfidentiel.setSelected(cr.isConfidentiel());
    }

    @FXML
    private void handleEnregistrer() {
        try {
            compteRendu.setDiagnostic(tfDiagnostic.getText());
            compteRendu.setContenu(taContenu.getText());
            compteRendu.setTraitement(taTraitement.getText());
            compteRendu.setProchain_rdv(dpProchainRdv.getValue());
            compteRendu.setConfidentiel(cbConfidentiel.isSelected());

            service.modifier(compteRendu);

            if (onSuccess != null) onSuccess.run();
            fermer();

        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Erreur : " + e.getMessage()).show();
        }
    }

    @FXML
    private void handleAnnuler() {
        fermer();
    }

    private void fermer() {
        ((Stage) tfDiagnostic.getScene().getWindow()).close();
    }
}