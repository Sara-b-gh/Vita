package Controlers;

import Entites.RendezVous;
import Services.RendezVousCRUD;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ResourceBundle;

public class AjouterRdvController implements Initializable {

    @FXML private TextField tfPatientId, tfMedecinId, tfMotif, tfLieu, tfHeure;
    @FXML private DatePicker dpDate;
    @FXML private ComboBox<String> cbStatut;
    @FXML private TextArea taNotes;
    @FXML private Label lblError;

    private final RendezVousCRUD crud = new RendezVousCRUD();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cbStatut.getItems().addAll("planifie", "confirme", "annule", "termine");
        cbStatut.setValue("planifie");
    }

    @FXML
    private void enregistrer() {
        lblError.setText("");
        try {
            int patientId = Integer.parseInt(tfPatientId.getText().trim());
            int medecinId = Integer.parseInt(tfMedecinId.getText().trim());
            String motif  = tfMotif.getText().trim();
            String statut = cbStatut.getValue();
            String lieu   = tfLieu.getText().trim();
            String notes  = taNotes.getText().trim();

            if (dpDate.getValue() == null) { lblError.setText("Veuillez choisir une date."); return; }
            LocalTime heure = parseHeure(tfHeure.getText().trim());
            LocalDateTime dateRdv = LocalDateTime.of(dpDate.getValue(), heure);

            RendezVous rv = new RendezVous(patientId, medecinId, dateRdv, motif, statut, lieu, notes);
            crud.ajouter(rv);
            fermer();

        } catch (NumberFormatException e) {
            lblError.setText("Patient ID et Médecin ID doivent être des nombres.");
        } catch (Exception e) {
            lblError.setText("Erreur : " + e.getMessage());
        }
    }

    @FXML
    private void annuler() { fermer(); }

    private void fermer() {
        ((Stage) tfMotif.getScene().getWindow()).close();
    }

    private LocalTime parseHeure(String txt) {
        if (txt == null || txt.isEmpty()) return LocalTime.of(8, 0);
        try {
            return LocalTime.parse(txt, DateTimeFormatter.ofPattern("HH:mm"));
        } catch (DateTimeParseException e) {
            lblError.setText("Format heure invalide (HH:mm).");
            return LocalTime.of(8, 0);
        }
    }
}