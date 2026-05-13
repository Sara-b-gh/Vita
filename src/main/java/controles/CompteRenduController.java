package controles;

import entities.CompteRendu;
import entities.User;
import services.CompteRenduCRUD;
import services.UserService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class CompteRenduController {

    private static final Logger LOGGER = Logger.getLogger(CompteRenduController.class.getName());

    @FXML
    private TextField rdvId;

    @FXML
    private ComboBox<User> cbDocteur;

    @FXML
    private TextArea contenu;

    @FXML
    private TextField diagnostic;

    @FXML
    private TextArea traitement;

    @FXML
    private DatePicker prochainRdv;

    @FXML
    private CheckBox confidentiel;

    private Runnable onSuccess;
    private CompteRenduCRUD service;
    private UserService userService;

    public void setOnSuccess(Runnable onSuccess) {
        this.onSuccess = onSuccess;
    }

    @FXML
    public void initialize() {
        try {
            // Initialisation des services
            service = new CompteRenduCRUD();
            userService = new UserService();
            chargerDocteurs();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'initialisation", e);
            showAlert("Erreur", "Impossible d'initialiser l'application : " + e.getMessage());
        }
    }

    /**
     * Remplit le ComboBox avec les docteurs
     */
    private void chargerDocteurs() {
        try {
            List<User> tous = userService.afficherTous();
            List<User> docteurs = tous.stream()
                    .filter(u -> u.getRole() == User.Roles.DOCTOR)
                    .collect(Collectors.toList());

            cbDocteur.setItems(FXCollections.observableArrayList(docteurs));

            // Configure l'affichage "Prénom Nom" dans le ComboBox
            cbDocteur.setConverter(new StringConverter<>() {
                @Override
                public String toString(User u) {
                    return u == null ? "" : u.getPrenom() + " " + u.getNom();
                }

                @Override
                public User fromString(String s) {
                    return null;
                }
            });

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du chargement des docteurs", e);
            showAlert("Erreur", "Impossible de charger la liste des docteurs : " + e.getMessage());
            cbDocteur.setItems(FXCollections.observableArrayList()); // Liste vide par défaut
        }
    }

    /**
     * Ajoute un compte rendu
     */
    @FXML
    void ajouterCompteRendu() {
        try {
            // Validation et création du compte rendu
            CompteRendu cr = creerCompteRenduDepuisFormulaire();

            // Ajout dans la base de données
            service.ajouter(cr);

            // Message de succès
            showAlert("Succès", "Compte rendu ajouté avec succès !");

            // Callback si défini
            if (onSuccess != null) {
                onSuccess.run();
            }

            // Nettoyage du formulaire
            viderChamps();

        } catch (ValidationException e) {
            showAlert("Erreur de validation", e.getMessage());
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Format d'ID invalide", e);
            showAlert("Erreur", "L'ID du rendez-vous doit être un nombre valide.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur inattendue", e);
            showAlert("Erreur", "Erreur inattendue : " + e.getMessage());
        }
    }

    /**
     * Crée un objet CompteRendu à partir des données du formulaire
     */
    private CompteRendu creerCompteRenduDepuisFormulaire() throws ValidationException {
        // Validation des champs obligatoires
        User docteur = cbDocteur.getValue();
        if (docteur == null) {
            throw new ValidationException("Veuillez sélectionner un docteur.");
        }

        String rdvIdText = rdvId.getText();
        if (rdvIdText == null || rdvIdText.trim().isEmpty()) {
            throw new ValidationException("L'ID du rendez-vous est requis.");
        }

        // Création du compte rendu
        CompteRendu cr = new CompteRendu();
        cr.setId_rdv(Integer.parseInt(rdvIdText.trim()));
        cr.setRedige_par(docteur.getId());
        cr.setContenu(contenu.getText() != null ? contenu.getText() : "");
        cr.setDiagnostic(diagnostic.getText() != null ? diagnostic.getText() : "");
        cr.setTraitement(traitement.getText() != null ? traitement.getText() : "");
        cr.setProchain_rdv(prochainRdv.getValue());
        cr.setConfidentiel(confidentiel.isSelected());

        return cr;
    }

    /**
     * Vide tous les champs du formulaire
     */
    private void viderChamps() {
        rdvId.clear();
        cbDocteur.setValue(null);
        contenu.clear();
        diagnostic.clear();
        traitement.clear();
        prochainRdv.setValue(null);
        confidentiel.setSelected(false);
    }

    /**
     * Affiche une boîte de dialogue d'alerte
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Définit l'ID du rendez-vous dans le champ correspondant
     */
    public void setRdvId(int id) {
        rdvId.setText(String.valueOf(id));
        rdvId.setEditable(false);
        rdvId.setStyle("-fx-background-color: #eeeeee;");
    }

    /**
     * Exception personnalisée pour les erreurs de validation
     */
    private static class ValidationException extends Exception {
        public ValidationException(String message) {
            super(message);
        }
    }
}