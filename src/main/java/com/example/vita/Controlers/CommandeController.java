package Controlers;

import Entites.Commande;
import Entites.LigneCommande;
import Entites.medicaments;
import Entites.equipements;
import services.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

public class CommandeController implements Initializable {

    @FXML private ComboBox<String>       typeArticleCombo;
    @FXML private ComboBox<String>       articleCombo;
    @FXML private TextField              quantiteField;
    @FXML private ListView<String>       lignesListView;
    @FXML private Label                  totalLabel, messageLabel, lblTotal;
    @FXML private ComboBox<String>       modePaiementCombo;

    @FXML private TableView<Commande>           commandeTable;
    @FXML private TableColumn<Commande, Integer> colId;
    @FXML private TableColumn<Commande, String>  colDate, colStatut, colMode, colRef;
    @FXML private TableColumn<Commande, Double>  colMontant;

    private final CommandeCRUD   commandeService = new CommandeCRUD();
    private final MedicamentCRUD medService      = new MedicamentCRUD();
    private final EquipementCRUD eqService       = new EquipementCRUD();

    private List<medicaments>  listeMeds = new ArrayList<>();
    private List<equipements>  listeEqs  = new ArrayList<>();
    private final List<LigneCommande> lignesActuelles = new ArrayList<>();
    private double total = 0;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        typeArticleCombo.setItems(FXCollections.observableArrayList(
                "Medicament", "Equipement"));
        typeArticleCombo.valueProperty().addListener((obs, o, n) -> chargerArticles(n));

        modePaiementCombo.setItems(FXCollections.observableArrayList(
                "Carte bancaire", "Especes", "Virement", "Cheque"));

        colId.setCellValueFactory(new PropertyValueFactory<>("id_commande"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colMontant.setCellValueFactory(new PropertyValueFactory<>("montant_total"));
        colMode.setCellValueFactory(new PropertyValueFactory<>("mode_paiement"));
        colRef.setCellValueFactory(new PropertyValueFactory<>("reference_paiement"));

        chargerCommandes();
    }

    private void chargerArticles(String type) {
        articleCombo.getItems().clear();
        try {
            if ("Medicament".equals(type)) {
                listeMeds = medService.afficher();
                listeMeds.forEach(m -> articleCombo.getItems().add(m.getNom()));
            } else {
                listeEqs = eqService.afficher();
                listeEqs.forEach(e -> articleCombo.getItems().add(e.getNom()));
            }
        } catch (SQLException e) {
            showMessage("Erreur chargement articles : " + e.getMessage(), "red");
        }
    }

    @FXML
    public void ajouterLigne() {
        if (articleCombo.getValue() == null) {
            showMessage("Selectionnez un article.", "orange"); return;
        }
        int qte;
        try { qte = Integer.parseInt(quantiteField.getText()); }
        catch (NumberFormatException e) { showMessage("Quantite invalide.", "red"); return; }

        String type = typeArticleCombo.getValue();
        LigneCommande ligne = new LigneCommande();
        ligne.setQuantite(qte);

        if ("Medicament".equals(type)) {
            int idx = articleCombo.getSelectionModel().getSelectedIndex();
            medicaments m = listeMeds.get(idx);
            ligne.setId_medicament(m.getId_medicament());
            ligne.setPrix_unitaire(m.getPrix());
            ligne.setNom_article(m.getNom());
        } else {
            int idx = articleCombo.getSelectionModel().getSelectedIndex();
            equipements e = listeEqs.get(idx);
            ligne.setId_equipement(e.getId_equipement());
            ligne.setPrix_unitaire(50.0); // prix fixe équipement
            ligne.setNom_article(e.getNom());
        }

        lignesActuelles.add(ligne);
        total += ligne.getSousTotal();

        ObservableList<String> items = FXCollections.observableArrayList();
        lignesActuelles.forEach(l -> items.add(l.toString()));
        lignesListView.setItems(items);

        totalLabel.setText(String.format("%.2f DT", total));
        showMessage("Article ajoute.", "green");
    }

    @FXML
    public void payerCommande() {
        if (lignesActuelles.isEmpty()) {
            showMessage("Ajoutez au moins un article.", "orange"); return;
        }
        if (modePaiementCombo.getValue() == null) {
            showMessage("Selectionnez un mode de paiement.", "orange"); return;
        }

        PaiementService.ResultatPaiement resultat =
                PaiementService.simulerPaiement(total, modePaiementCombo.getValue(), null);

        if (resultat.statut == PaiementService.StatutPaiement.SUCCES) {
            try {
                Commande c = new Commande("payee", total, modePaiementCombo.getValue());
                c.setLignes(new ArrayList<>(lignesActuelles));
                commandeService.ajouter(c);
                commandeService.mettreAJourStatut(c.getId_commande(),
                        "payee", resultat.reference);

                showMessage("Paiement OK ! Ref: " + resultat.reference, "green");
                lignesActuelles.clear();
                lignesListView.setItems(FXCollections.observableArrayList());
                total = 0;
                totalLabel.setText("0.00 DT");
                chargerCommandes();
            } catch (SQLException e) {
                showMessage("Erreur BD : " + e.getMessage(), "red");
            }
        } else {
            showMessage("Paiement echoue : " + resultat.message, "red");
        }
    }

    @FXML
    public void chargerCommandes() {
        try {
            List<Commande> list = commandeService.afficher();
            commandeTable.setItems(FXCollections.observableArrayList(list));
            if (lblTotal != null) lblTotal.setText(list.size() + " commande(s)");
        } catch (SQLException e) {
            showMessage("Erreur : " + e.getMessage(), "red");
        }
    }

    @FXML
    public void supprimerCommande() {
        Commande selected = commandeTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showMessage("Selectionnez une commande.", "orange"); return; }
        try {
            commandeService.supprimer(selected.getId_commande());
            showMessage("Commande supprimee.", "green");
            chargerCommandes();
        } catch (SQLException e) {
            showMessage("Erreur : " + e.getMessage(), "red");
        }
    }

    @FXML private void allerAuDashboard()      { naviguer("/Main-view.fxml"); }
    @FXML private void allerAuxMedicaments()   { naviguer("/AjouterMedicament.fxml"); }
    @FXML private void allerAuxEquipements()   { naviguer("/AjouterEquippement.fxml"); }

    private void naviguer(String fxml) {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource(fxml)));
            Stage stage = (Stage) commandeTable.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            showMessage("Erreur navigation : " + e.getMessage(), "red");
        }
    }

    private void showMessage(String msg, String color) {
        if (messageLabel != null) {
            messageLabel.setText(msg);
            messageLabel.setStyle("-fx-text-fill:" + color + ";");
        }
    }
}