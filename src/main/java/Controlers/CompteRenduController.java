package Controlers;

import Entites.CompteRendu;
import Services.CompteRenduCRUD;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.time.LocalDate;

public class CompteRenduController {

    @FXML private TextField cpId;
    @FXML private TextField rdvId;
    @FXML private TextField redigePar;
    @FXML private TextArea  contenu;
    @FXML private TextField diagnostic;
    @FXML private TextArea  traitement;
    @FXML private DatePicker prochainRdv;
    @FXML private CheckBox confidentiel;
    private Runnable onSuccess; // callback pour rafraîchir la liste parente

    public void setOnSuccess(Runnable onSuccess) {
        this.onSuccess = onSuccess;
    }
    CompteRenduCRUD service = new CompteRenduCRUD();

    @FXML
    void AJOUTCP(ActionEvent event) throws SQLException {
        try{
        CompteRendu cr = new CompteRendu();
        cr.setId_rdv(Integer.parseInt(rdvId.getText()));
        cr.setRedige_par(Integer.parseInt(redigePar.getText()));
        cr.setContenu(contenu.getText());
        cr.setDiagnostic(diagnostic.getText());
        cr.setTraitement(traitement.getText());
        cr.setProchain_rdv(prochainRdv.getValue());
        cr.setConfidentiel(confidentiel.isSelected());
        new CompteRenduCRUD().ajouter(cr);
        showAlert("Succès", "Compte rendu ajouté avec succès !");
            if (onSuccess != null) onSuccess.run(); // rafraîchit la liste

            Stage stage = (Stage) rdvId.getScene().getWindow();
            stage.close();
    } catch (Exception e) {
        showAlert("Erreur", "Erreur lors de l'ajout : " + e.getMessage());
    }
    }


    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
    public void setRdvId(int id) {
        rdvId.setText(String.valueOf(id));
        rdvId.setEditable(false);   // prevent the user from changing it
        rdvId.setStyle("-fx-background-color: #eeeeee;");
    }

}