package controles;

import entities.CompteRendu;
import entities.RendezVous;
import entities.User;
import services.CompteRenduCRUD;
import services.PdfService;
import services.UserService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AfficherCompteRenduController {

    // ── FXML ───────────────────────────────────────────────────
    @FXML private BorderPane  mainPane;
    @FXML private ScrollPane  cardsScrollPane;
    @FXML private VBox        cardsContainer;
    @FXML private Label       lblPageTitle;
    @FXML private Button      btnRetour;

    // ── Barre recherche/filtre/tri ──────────────────────────────
    @FXML private TextField         tfRecherche;
    @FXML private ComboBox<String>  cbFiltreConfidentiel;
    @FXML private ComboBox<String>  cbFiltreRdv;
    @FXML private ComboBox<String>  cbTri;
    @FXML private Label             lblCount;
    @FXML private HBox              searchBar;

    // ── Services & état ────────────────────────────────────────
    private final UserService     userService = new UserService();
    private final CompteRenduCRUD service     = new CompteRenduCRUD();
    private RendezVous currentRdv = null;

    private Node              vueCartes;
    private List<CompteRendu> tousLesCr = new ArrayList<>();

    public AfficherCompteRenduController() throws SQLException {
    }

    // ══════════════════════════════════════════════════════════
    //  INIT
    // ══════════════════════════════════════════════════════════

    public void setRendezVous(RendezVous rdv) {
        this.currentRdv = rdv;
        chargerCartes();
    }

    @FXML
    public void initialize() {
        vueCartes = cardsScrollPane;

        cbFiltreConfidentiel.setItems(FXCollections.observableArrayList(
                "Tous", "Confidentiel", "Public"));

        cbFiltreRdv.setItems(FXCollections.observableArrayList(
                "Tous", "Avec prochain RDV", "Sans prochain RDV"));

        cbTri.setItems(FXCollections.observableArrayList(
                "Date création ↓", "Date création ↑",
                "Diagnostic A→Z", "Diagnostic Z→A",
                "Prochain RDV ↑", "Prochain RDV ↓"));

        tfRecherche.textProperty()
                .addListener((obs, o, n) -> appliquerFiltresEtTri());
        cbFiltreConfidentiel.valueProperty()
                .addListener((obs, o, n) -> appliquerFiltresEtTri());
        cbFiltreRdv.valueProperty()
                .addListener((obs, o, n) -> appliquerFiltresEtTri());
        cbTri.valueProperty()
                .addListener((obs, o, n) -> appliquerFiltresEtTri());

        chargerCartes();
    }

    // ══════════════════════════════════════════════════════════
    //  SWAP DU CENTER
    // ══════════════════════════════════════════════════════════

    private void afficherVue(Node vue, String titre) {
        mainPane.setCenter(vue);
        lblPageTitle.setText(titre);
        btnRetour.setVisible(true);
        btnRetour.setManaged(true);
        searchBar.setVisible(false);
        searchBar.setManaged(false);
    }

    @FXML
    private void handleRetour() {
        mainPane.setCenter(vueCartes);
        lblPageTitle.setText("Comptes Rendus");
        btnRetour.setVisible(false);
        btnRetour.setManaged(false);
        searchBar.setVisible(true);
        searchBar.setManaged(true);
        chargerCartes();
    }

    // ══════════════════════════════════════════════════════════
    //  CHARGEMENT DEPUIS LA BD
    // ══════════════════════════════════════════════════════════

    private void chargerCartes() {
        try {
            tousLesCr = service.afficher();
            if (currentRdv != null)
                tousLesCr.removeIf(cr -> cr.getId_rdv() != currentRdv.getId_rdv());
            appliquerFiltresEtTri();
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Erreur chargement : " + e.getMessage()).show();
        }
    }

    // ══════════════════════════════════════════════════════════
    //  FILTRE + TRI + RENDU
    // ══════════════════════════════════════════════════════════

    private void appliquerFiltresEtTri() {
        List<CompteRendu> resultat = new ArrayList<>(tousLesCr);

        // 1. Recherche texte libre + id_rdv
        String kw = tfRecherche.getText();
        if (kw != null && !kw.isBlank()) {
            String k = kw.toLowerCase().trim();
            resultat.removeIf(cr ->
                    !contains(cr.getDiagnostic(), k)
                            && !contains(cr.getContenu(), k)
                            && !contains(cr.getTraitement(), k)
                            && !nomDocteur(cr.getRedige_par()).toLowerCase().contains(k)
                            && !String.valueOf(cr.getId_rdv()).contains(k)  // ← recherche par id_rdv
            );
        }

        // 2. Filtre confidentialité
        String filtreConf = cbFiltreConfidentiel.getValue();
        if ("Confidentiel".equals(filtreConf))
            resultat.removeIf(cr -> !cr.isConfidentiel());
        else if ("Public".equals(filtreConf))
            resultat.removeIf(CompteRendu::isConfidentiel);

        // 3. Filtre prochain RDV
        String filtreRdv = cbFiltreRdv.getValue();
        if ("Avec prochain RDV".equals(filtreRdv))
            resultat.removeIf(cr -> cr.getProchain_rdv() == null);
        else if ("Sans prochain RDV".equals(filtreRdv))
            resultat.removeIf(cr -> cr.getProchain_rdv() != null);

        // 4. Tri
        String tri = cbTri.getValue();
        if (tri != null) {
            switch (tri) {
                case "Date création ↓" -> resultat.sort(Comparator.comparing(
                        CompteRendu::getDate_creation,
                        Comparator.nullsLast(Comparator.reverseOrder())));
                case "Date création ↑" -> resultat.sort(Comparator.comparing(
                        CompteRendu::getDate_creation,
                        Comparator.nullsLast(Comparator.naturalOrder())));
                case "Diagnostic A→Z" -> resultat.sort(Comparator.comparing(
                        cr -> cr.getDiagnostic() != null ? cr.getDiagnostic() : ""));
                case "Diagnostic Z→A" -> resultat.sort(Comparator.comparing(
                        (CompteRendu cr) -> cr.getDiagnostic() != null ? cr.getDiagnostic() : "",
                        Comparator.reverseOrder()));
                case "Prochain RDV ↑" -> resultat.sort(Comparator.comparing(
                        CompteRendu::getProchain_rdv,
                        Comparator.nullsLast(Comparator.naturalOrder())));
                case "Prochain RDV ↓" -> resultat.sort(Comparator.comparing(
                        CompteRendu::getProchain_rdv,
                        Comparator.nullsLast(Comparator.reverseOrder())));
            }
        }

        // 5. Rendu
        afficherCartes(resultat);
    }

    private void afficherCartes(List<CompteRendu> liste) {
        cardsContainer.getChildren().clear();
        lblCount.setText(liste.size() + " résultat" + (liste.size() > 1 ? "s" : ""));

        if (liste.isEmpty()) {
            Label empty = new Label("Aucun compte rendu trouvé.");
            empty.setStyle("-fx-text-fill: #999; -fx-font-size: 15px;");
            cardsContainer.getChildren().add(empty);
            return;
        }
        for (CompteRendu cr : liste)
            cardsContainer.getChildren().add(buildCard(cr));
    }
    @FXML
    private void exporterEnPdf(CompteRendu cr) {
        try {
            User docteur = userService.findById(cr.getRedige_par());
            String nomPatient = "Patient_" + cr.getId_rdv();

            String fileName = "CR_" + cr.getId_cr() + "_" + java.time.LocalDate.now() + ".pdf";

            // Utilisation de FileChooser (beaucoup mieux pour l'utilisateur)
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Enregistrer le compte rendu PDF");
            fileChooser.setInitialFileName(fileName);
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier PDF", "*.pdf"));

            // Dossier par défaut : Bureau
            File defaultDir = new File(System.getProperty("user.home") + "/Desktop");
            if (defaultDir.exists()) {
                fileChooser.setInitialDirectory(defaultDir);
            }

            File file = fileChooser.showSaveDialog(null);

            if (file != null) {
                // Créer les dossiers parents si nécessaire
                File parentDir = file.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }

                PdfService.genererPdfCompteRendu(cr, docteur, nomPatient, file.getAbsolutePath());

                new Alert(Alert.AlertType.INFORMATION,
                        "✅ PDF généré avec succès !\n" + file.getAbsolutePath()).showAndWait();
            }

        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR,
                    "Erreur lors de la génération du PDF :\n" + e.getMessage()).showAndWait();
            e.printStackTrace();
        }
    }
    @FXML
    private void handleResetFiltres() {
        tfRecherche.clear();
        cbFiltreConfidentiel.setValue(null);
        cbFiltreRdv.setValue(null);
        cbTri.setValue(null);
    }

    // ══════════════════════════════════════════════════════════
    //  CONSTRUCTION D'UNE CARTE
    // ══════════════════════════════════════════════════════════

    private VBox buildCard(CompteRendu cr) {
        Label title = new Label("CR #" + cr.getId_cr() + " — RDV #" + cr.getId_rdv());
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #7a002f;");

        Label badge = new Label(cr.isConfidentiel() ? "🔒 Confidentiel" : "Public");
        badge.setStyle("-fx-font-size: 11px; -fx-text-fill: white; -fx-padding: 2 8;" +
                "-fx-background-radius: 20;" +
                (cr.isConfidentiel() ? "-fx-background-color: #7a002f;" : "-fx-background-color: #4caf50;"));

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        HBox header = new HBox(10, title, sp, badge);
        header.setAlignment(Pos.CENTER_LEFT);

        Label docteurLabel = new Label("Dr. " + nomDocteur(cr.getRedige_par()));
        docteurLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7a002f; -fx-font-style: italic;");

        Label diagLabel = new Label("Diagnostic : " + (cr.getDiagnostic() != null ? cr.getDiagnostic() : "—"));
        diagLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #333;");
        diagLabel.setWrapText(true);

        Label contenuLabel = new Label("Contenu : " + (cr.getContenu() != null ? cr.getContenu() : "—"));
        contenuLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
        contenuLabel.setWrapText(true);

        Label dateLabel = new Label("Créé le : " + (cr.getDate_creation() != null ? cr.getDate_creation().toLocalDate() : "—"));
        dateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #aaa;");

        // ==================== BOUTONS ====================
        Button btnConsulter = new Button("Consulter");
        btnConsulter.getStyleClass().addAll("button", "btn-save");
        btnConsulter.setOnAction(e -> ouvrirConsultation(cr));

        Button btnModifier = new Button("Modifier");
        btnModifier.getStyleClass().addAll("button", "btn-edit");
        btnModifier.setOnAction(e -> ouvrirModification(cr));

        Button btnSupprimer = new Button("Supprimer");
        btnSupprimer.getStyleClass().addAll("button", "btn-delete");
        btnSupprimer.setOnAction(e -> supprimerCr(cr));

        Button btnPdf = new Button("📄 PDF");
        btnPdf.getStyleClass().addAll("button", "btn-save");   // ou un autre style si tu veux
        btnPdf.setOnAction(e -> exporterEnPdf(cr));

        HBox actions = new HBox(8, btnConsulter, btnModifier, btnPdf, btnSupprimer);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox card = new VBox(8, header, docteurLabel, diagLabel, contenuLabel, dateLabel, actions);
        card.setPadding(new Insets(14, 18, 14, 18));
        card.setStyle(cardStyle("0.07"));
        card.setOnMouseEntered(e -> card.setStyle(cardStyle("0.14")));
        card.setOnMouseExited(e -> card.setStyle(cardStyle("0.07")));

        return card;
    }

    private String cardStyle(String shadow) {
        return "-fx-background-color: white;"
                + "-fx-background-radius: 12;"
                + "-fx-border-color: #f0e0e8;"
                + "-fx-border-radius: 12;"
                + "-fx-effect: dropshadow(gaussian,rgba(0,0,0," + shadow + "),8,0,0,2);";
    }

    // ══════════════════════════════════════════════════════════
    //  ACTIONS
    // ══════════════════════════════════════════════════════════

    @FXML
    private void handleAjouter() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/AjouterCompteRendu.fxml"));
            Node vue = loader.load();

            CompteRenduController ctrl = loader.getController();
            if (currentRdv != null) ctrl.setRdvId(currentRdv.getId_rdv());
            ctrl.setOnSuccess(this::handleRetour);

            afficherVue(vue, "Nouveau Compte Rendu");
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR,
                    "Impossible d'ouvrir le formulaire : " + e.getMessage()).show();
        }
    }

    private void ouvrirModification(CompteRendu cr) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ModifierCompteRendu.fxml"));
            Node vue = loader.load();

            ModifierCompteRenduController ctrl = loader.getController();
            ctrl.initData(cr, this::handleRetour);

            afficherVue(vue, "Modifier CR #" + cr.getId_cr());
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR,
                    "Impossible d'ouvrir le formulaire : " + e.getMessage()).show();
        }
    }

    private void ouvrirConsultation(CompteRendu cr) {
        afficherVue(buildDetailView(cr), "Détail CR #" + cr.getId_cr());
    }

    private void supprimerCr(CompteRendu cr) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer CR #" + cr.getId_cr() + " ?");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    service.supprimer(cr.getId_cr());
                    chargerCartes();
                } catch (SQLException e) {
                    new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
                }
            }
        });
    }

    @FXML
    private void handleActualiser() {
        chargerCartes();
    }

    // ══════════════════════════════════════════════════════════
    //  VUE DÉTAIL
    // ══════════════════════════════════════════════════════════

    private ScrollPane buildDetailView(CompteRendu cr) {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(0);
        grid.setPadding(new Insets(24));

        ColumnConstraints c1 = new ColumnConstraints(150);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c1, c2);

        String[][] rows = {
                {"RDV ID",       String.valueOf(cr.getId_rdv())},
                {"Rédigé par",   nomDocteur(cr.getRedige_par())},
                {"Diagnostic",   cr.getDiagnostic()},
                {"Contenu",      cr.getContenu()},
                {"Traitement",   cr.getTraitement()},
                {"Prochain RDV", cr.getProchain_rdv() != null
                        ? cr.getProchain_rdv().toString() : "—"},
                {"Confidentiel", cr.isConfidentiel() ? "Oui" : "Non"},
                {"Créé le",      cr.getDate_creation() != null
                        ? cr.getDate_creation().toString() : "—"}
        };

        for (int i = 0; i < rows.length; i++) {
            String bg = (i % 2 == 1) ? "#fdf5f8" : "white";
            grid.add(styledCell(rows[i][0], true,  bg), 0, i);
            grid.add(styledCell(rows[i][1], false, bg), 1, i);
        }

        VBox wrapper = new VBox(grid);
        wrapper.setStyle("-fx-background-color: white;"
                + "-fx-background-radius: 12;"
                + "-fx-border-color: #f0e0e8;"
                + "-fx-border-radius: 12;");

        VBox page = new VBox(wrapper);
        page.setPadding(new Insets(24));
        page.getStyleClass().add("page");

        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("list-view");
        return scroll;
    }
    private RendezVousController parentController;

    public void setParentController(RendezVousController ctrl) {
        this.parentController = ctrl;
    }
    private HBox styledCell(String text, boolean bold, String bg) {
        Label lbl = new Label(text != null ? text : "—");
        lbl.setWrapText(true);
        lbl.setStyle("-fx-font-size: 13px;"
                + (bold ? "-fx-font-weight: bold; -fx-text-fill: #7a002f;"
                : "-fx-text-fill: #333;"));
        HBox box = new HBox(lbl);
        box.setPadding(new Insets(12, 10, 12, 10));
        box.setStyle("-fx-background-color: " + bg + ";"
                + "-fx-border-color: transparent transparent #f0e0e8 transparent;");
        HBox.setHgrow(lbl, Priority.ALWAYS);
        return box;
    }


    public void filtrerParRdv(int idRdv) {
        tfRecherche.setText(String.valueOf(idRdv));
        // le listener sur tfRecherche déclenche appliquerFiltresEtTri() automatiquement
    }

    @FXML
    private void ConsulterRDV() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AfficherRendezVous.fxml"));
            Parent vue = loader.load();
            Stage stage = (Stage) mainPane.getScene().getWindow();
            stage.getScene().setRoot(vue);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String nomDocteur(int id) {
        User u = userService.findById(id);
        if (u != null) return u.getPrenom() + " " + u.getNom();
        return "Docteur #" + id;
    }

    private boolean contains(String source, String kw) {
        return source != null && source.toLowerCase().contains(kw);
    }
}