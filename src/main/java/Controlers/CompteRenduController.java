package Controlers;

import Entites.CompteRendu;
import Entites.User;
import Services.CompteRenduCRUD;
import Services.UserService;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class CompteRenduController {

    @FXML private TextField  rdvId;
    @FXML private ComboBox<User> cbDocteur;   // ← remplace redigePar TextField
    @FXML private TextArea   contenu;
    @FXML private TextField  diagnostic;
    @FXML private TextArea   traitement;
    @FXML private DatePicker prochainRdv;
    @FXML private CheckBox   confidentiel;

    private Runnable onSuccess;
    private final CompteRenduCRUD service     = new CompteRenduCRUD();
    private final UserService     userService = new UserService();

    public void setOnSuccess(Runnable onSuccess) {
        this.onSuccess = onSuccess;
    }

    @FXML
    public void initialize() {
        chargerDocteurs();
    }

    // ── Remplit le ComboBox avec les docteurs ───────────────
    private void chargerDocteurs() {
        try {
            List<User> tous = userService.afficherTous(); // voir §4 ci-dessous
            List<User> docteurs = tous.stream()
                    .filter(u -> u.getRole() == User.Roles.DOCTOR)
                    .collect(Collectors.toList());

            cbDocteur.setItems(FXCollections.observableArrayList(docteurs));

            // Affiche "Prénom Nom" dans le ComboBox
            cbDocteur.setConverter(new StringConverter<>() {
                @Override public String toString(User u) {
                    return u == null ? "" : u.getPrenom() + " " + u.getNom();
                }
                @Override public User fromString(String s) { return null; }
            });

        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR,
                    "Impossible de charger les docteurs : " + e.getMessage()).show();
        }
    }

    // ── Ajouter ─────────────────────────────────────────────
    @FXML
    void AJOUTCP(ActionEvent event) {
        try {
            User docteur = cbDocteur.getValue();
            if (docteur == null) {
                showAlert("Erreur", "Veuillez sélectionner un docteur.");
                return;
            }

            CompteRendu cr = new CompteRendu();
            cr.setId_rdv(Integer.parseInt(rdvId.getText()));
            cr.setRedige_par(docteur.getId());          // ← on prend l'ID du User sélectionné
            cr.setContenu(contenu.getText());
            cr.setDiagnostic(diagnostic.getText());
            cr.setTraitement(traitement.getText());
            cr.setProchain_rdv(prochainRdv.getValue());
            cr.setConfidentiel(confidentiel.isSelected());

            service.ajouter(cr);
            showAlert("Succès", "Compte rendu ajouté avec succès !");
            if (onSuccess != null) onSuccess.run();

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
        rdvId.setEditable(false);
        rdvId.setStyle("-fx-background-color: #eeeeee;");
    }
}