package Controlers;

import Entites.CompteRendu;
import Entites.RendezVous;
import Entites.User;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import services.*;
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
import tests.Mainfx;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class AfficherCompteRenduController {

    private static final Logger LOGGER = Logger.getLogger(AfficherCompteRenduController.class.getName());

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
    private final CompteRenduCRUD service;
    private RendezVous currentRdv = null;

    private Node              vueCartes;
    private List<CompteRendu> tousLesCr = new ArrayList<>();

    // Constructeur
    public AfficherCompteRenduController() {
        try {
            this.service = new CompteRenduCRUD();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'initialisation du service", e);
            throw new RuntimeException("Impossible d'initialiser le service CompteRenduCRUD", e);
        }
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
                "Date de création ↓", "Date de création ↑",
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
            if (currentRdv != null) {
                tousLesCr.removeIf(cr -> cr.getId_rdv() != currentRdv.getId_rdv());
            }
            appliquerFiltresEtTri();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du chargement des comptes rendus", e);
            showAlert("Erreur", "Erreur chargement : " + e.getMessage());
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
                    !contientTexte(cr.getDiagnostic(), k) &&
                            !contientTexte(cr.getContenu(), k) &&
                            !contientTexte(cr.getTraitement(), k) &&
                            !nomDocteur(cr.getRedige_par()).toLowerCase().contains(k) &&
                            !String.valueOf(cr.getId_rdv()).contains(k)
            );
        }

        // 2. Filtre confidentialité
        String filtreConf = cbFiltreConfidentiel.getValue();
        if ("Confidentiel".equals(filtreConf)) {
            resultat.removeIf(cr -> !cr.isConfidentiel());
        } else if ("Public".equals(filtreConf)) {
            resultat.removeIf(CompteRendu::isConfidentiel);
        }

        // 3. Filtre prochain RDV
        String filtreRdv = cbFiltreRdv.getValue();
        if ("Avec prochain RDV".equals(filtreRdv)) {
            resultat.removeIf(cr -> cr.getProchain_rdv() == null);
        } else if ("Sans prochain RDV".equals(filtreRdv)) {
            resultat.removeIf(cr -> cr.getProchain_rdv() != null);
        }

        // 4. Tri
        String tri = cbTri.getValue();
        if (tri != null) {
            switch (tri) {
                case "Date de création ↓" -> resultat.sort(Comparator.comparing(
                        CompteRendu::getDate_creation,
                        Comparator.nullsLast(Comparator.reverseOrder())));
                case "Date de création ↑" -> resultat.sort(Comparator.comparing(
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
                default -> {}
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
        for (CompteRendu cr : liste) {
            cardsContainer.getChildren().add(buildCard(cr));
        }
    }

    @FXML
    private void exporterEnPdf(CompteRendu cr) {
        try {
            User docteur = userService.findById(cr.getRedige_par());
            String nomPatient = "Patient_" + cr.getId_rdv();

            String fileName = "CR_" + cr.getId_cr() + "_" + java.time.LocalDate.now() + ".pdf";

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Enregistrer le compte rendu PDF");
            fileChooser.setInitialFileName(fileName);
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier PDF", "*.pdf"));

            File defaultDir = new File(System.getProperty("user.home") + "/Desktop");
            if (defaultDir.exists()) {
                fileChooser.setInitialDirectory(defaultDir);
            }

            File file = fileChooser.showSaveDialog(null);

            if (file != null) {
                File parentDir = file.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    boolean created = parentDir.mkdirs();
                    if (!created) {
                        LOGGER.warning("Impossible de créer le dossier: " + parentDir.getAbsolutePath());
                    }
                }

                PdfService.genererPdfCompteRendu(cr, docteur, nomPatient, file.getAbsolutePath());

                showAlert("Succès", "✅ PDF généré avec succès !\n" + file.getAbsolutePath());
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la génération du PDF", e);
            showAlert("Erreur", "Erreur lors de la génération du PDF :\n" + e.getMessage());
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
        Label title = new Label("CR n°" + cr.getId_cr() + " — RDV n°" + cr.getId_rdv());
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #7a002f;");

        Label badge = new Label(cr.isConfidentiel() ? "🔒 Confidentiel" : "Public");
        String badgeStyle = "-fx-font-size: 11px; -fx-text-fill: white; -fx-padding: 2 8;" +
                "-fx-background-radius: 20;" +
                (cr.isConfidentiel() ? "-fx-background-color: #7a002f;" : "-fx-background-color: #4caf50;");
        badge.setStyle(badgeStyle);

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

        // Boutons
        Button btnConsulter = createButton("Consulter", "btn-save", e -> ouvrirConsultation(cr));
        Button btnModifier = createButton("Modifier", "btn-edit", e -> ouvrirModification(cr));
        Button btnSupprimer = createButton("Supprimer", "btn-delete", e -> supprimerCr(cr));
        Button btnPdf = createButton("📄 PDF", "btn-save", e -> exporterEnPdf(cr));

        HBox actions = new HBox(8, btnConsulter, btnModifier, btnPdf, btnSupprimer);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox card = new VBox(8, header, docteurLabel, diagLabel, contenuLabel, dateLabel, actions);
        card.setPadding(new Insets(14, 18, 14, 18));

        String defaultStyle = "-fx-background-color: white; -fx-background-radius: 12; " +
                "-fx-border-color: #f0e0e8; -fx-border-radius: 12; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 8, 0, 0, 2);";
        String hoverStyle = "-fx-background-color: #fefafa; -fx-background-radius: 12; " +
                "-fx-border-color: #f0e0e8; -fx-border-radius: 12; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.14), 8, 0, 0, 2);";

        card.setStyle(defaultStyle);
        card.setOnMouseEntered(e -> card.setStyle(hoverStyle));
        card.setOnMouseExited(e -> card.setStyle(defaultStyle));

        return card;
    }

    private Button createButton(String text, String styleClass, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button btn = new Button(text);
        btn.getStyleClass().addAll("button", styleClass);
        btn.setOnAction(handler);
        return btn;
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
            if (currentRdv != null) {
                ctrl.setRdvId(currentRdv.getId_rdv());
            }
            ctrl.setOnSuccess(this::handleRetour);

            afficherVue(vue, "Nouveau Compte Rendu");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Impossible d'ouvrir le formulaire d'ajout", e);
            showAlert("Erreur", "Impossible d'ouvrir le formulaire : " + e.getMessage());
        }
    }

    private void ouvrirModification(CompteRendu cr) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ModifierCompteRendu.fxml"));
            Node vue = loader.load();

            ModifierCompteRenduController ctrl = loader.getController();
            ctrl.initData(cr, this::handleRetour);

            afficherVue(vue, "Modifier CR n°" + cr.getId_cr());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Impossible d'ouvrir le formulaire de modification", e);
            showAlert("Erreur", "Impossible d'ouvrir le formulaire : " + e.getMessage());
        }
    }

    private void ouvrirConsultation(CompteRendu cr) {
        afficherVue(buildDetailView(cr), "Détail CR n°" + cr.getId_cr());
    }

    private void supprimerCr(CompteRendu cr) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer CR n°" + cr.getId_cr() + " ? Cette action est irréversible.");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    service.supprimer(cr.getId_cr());
                    chargerCartes();
                    showAlert("Succès", "Compte rendu supprimé avec succès.");
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Erreur lors de la suppression", e);
                    showAlert("Erreur", "Erreur lors de la suppression : " + e.getMessage());
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
            grid.add(createStyledCell(rows[i][0], true, bg), 0, i);
            grid.add(createStyledCell(rows[i][1], false, bg), 1, i);
        }

        VBox wrapper = new VBox(grid);
        String wrapperStyle = "-fx-background-color: white; -fx-background-radius: 12; " +
                "-fx-border-color: #f0e0e8; -fx-border-radius: 12;";
        wrapper.setStyle(wrapperStyle);

        VBox page = new VBox(wrapper);
        page.setPadding(new Insets(24));

        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        return scroll;
    }

    private HBox createStyledCell(String text, boolean bold, String bg) {
        Label lbl = new Label(text != null ? text : "—");
        lbl.setWrapText(true);
        String style = "-fx-font-size: 13px;" +
                (bold ? "-fx-font-weight: bold; -fx-text-fill: #7a002f;" : "-fx-text-fill: #333;");
        lbl.setStyle(style);

        HBox box = new HBox(lbl);
        box.setPadding(new Insets(12, 10, 12, 10));
        String boxStyle = "-fx-background-color: " + bg + "; " +
                "-fx-border-color: transparent transparent #f0e0e8 transparent;";
        box.setStyle(boxStyle);
        HBox.setHgrow(lbl, Priority.ALWAYS);
        return box;
    }

    public void filtrerParRdv(int idRdv) {
        tfRecherche.setText(String.valueOf(idRdv));
    }

    @FXML
    private void consulterRendezVous() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AfficherRendezVous.fxml"));
            Parent vue = loader.load();
            Stage stage = (Stage) mainPane.getScene().getWindow();
            stage.getScene().setRoot(vue);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la navigation vers la vue des rendez-vous", e);
            showAlert("Erreur", "Impossible de charger la vue des rendez-vous");
        }
    }

    private String nomDocteur(int id) {
        User u = userService.findById(id);
        if (u != null) {
            return u.getPrenom() + " " + u.getNom();
        }
        return "Docteur n°" + id;
    }

    private boolean contientTexte(String source, String kw) {
        return source != null && source.toLowerCase().contains(kw);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static class UserEvenementController {

        @FXML private BorderPane rootPane;
        @FXML private HBox userSearchBox;
        @FXML private TextField searchField;
        @FXML private ComboBox<String> forumFilterCombo;
        @FXML private Button listViewBtn;
        @FXML private Button calendarViewBtn;
        @FXML private VBox listView;
        @FXML private VBox calendarView;
        @FXML private Button menuEvenementsBtn;
        @FXML private Button menuRendezVousBtn;
        @FXML private Button menuDossiersBtn;
        @FXML private Button menuQuizBtn;
        @FXML private Button menuMedicamentsBtn;
        @FXML private VBox eventsContainer;
        @FXML private Label monthYearLabel;
        @FXML private GridPane calendarGrid;
        @FXML private Label statsLabel;
        @FXML private Label totalEventsLabel;
        @FXML private Label locationsCountLabel;
        @FXML private Label rateLabel;
        @FXML private Circle donutProgressCircle;
        @FXML private Label donutTotalLabel;
        @FXML private Label donutUpcomingLabel;
        @FXML private Label donutPastLabel;
        @FXML private HBox monthlyBarsContainer;

        private final ServiceEvenn serviceEvenn = new ServiceEvenn();
        private final ServiceRessource serviceRessource = new ServiceRessource();
        private final ServiceReservationPersonne srp = new ServiceReservationPersonne();

        private List<RendezVous.Evenn> allEvents = new ArrayList<>();
        private List<RendezVous.Evenn> filteredEvents = new ArrayList<>();
        private List<RendezVous.Ressource> ressources = new ArrayList<>();
        private YearMonth currentMonth = YearMonth.now();

        private static final Locale FR = Locale.FRENCH;
        private static final DateTimeFormatter FMT_DATE_LONG = DateTimeFormatter.ofPattern("dd MMMM yyyy", FR);
        private static final DateTimeFormatter FMT_TIME = DateTimeFormatter.ofPattern("HH:mm", FR);

        @FXML
        public void initialize() {
            chargerRessources();
            chargerEvenements();
            initialiserListeners();
            appliquerMenuActif(menuEvenementsBtn);
            switchToListView();
            ImageView testQR = QRCodeService.generateReservationQRCode(1);
            if (testQR != null) {
                System.out.println("✅ QRCodeService fonctionne");
            } else {
                System.out.println("❌ QRCodeService ne fonctionne pas");
            }
        }

        private void initialiserListeners() {
            if (searchField != null) {
                searchField.textProperty().addListener((obs, oldValue, newValue) -> {
                    validerRecherche();
                    filtrerEvenements();
                });
            }
            if (forumFilterCombo != null) {
                forumFilterCombo.valueProperty().addListener((obs, oldValue, newValue) -> filtrerEvenements());
            }
        }

        private void chargerRessources() {
            try {
                ressources = serviceRessource.getAll();
                List<String> options = ressources.stream()
                        .filter(Objects::nonNull)
                        .map(RendezVous.Ressource::getNomRessource)
                        .filter(Objects::nonNull)
                        .filter(nom -> !nom.isBlank())
                        .distinct()
                        .sorted()
                        .collect(Collectors.toList());
                options.add(0, "Tous les forums");
                if (forumFilterCombo != null) {
                    forumFilterCombo.setItems(FXCollections.observableArrayList(options));
                    forumFilterCombo.setValue("Tous les forums");
                }
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert("Erreur", "Impossible de charger la liste des forums.");
            }
        }

        private void chargerEvenements() {
            try {
                allEvents = serviceEvenn.getAll();
                filtrerEvenements();
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert("Erreur", "Impossible de charger les événements.");
            }
        }

        private void validerRecherche() {
            if (searchField == null) return;
            String texte = searchField.getText();
            if (texte == null || texte.isBlank()) return;
            String nettoye = texte.replaceAll("[<>{}]", "");
            if (!nettoye.equals(texte)) {
                searchField.setText(nettoye);
            }
            if (nettoye.length() > 60) {
                searchField.setText(nettoye.substring(0, 60));
            }
        }

        private void filtrerEvenements() {
            String recherche = searchField != null && searchField.getText() != null
                    ? searchField.getText().trim().toLowerCase(FR)
                    : "";
            String forumSelectionne = forumFilterCombo != null ? forumFilterCombo.getValue() : "Tous les forums";

            filteredEvents = allEvents.stream()
                    .filter(Objects::nonNull)
                    .filter(event -> {
                        boolean correspondRecherche = recherche.isBlank()
                                || contient(event.getTitre(), recherche)
                                || contient(event.getLieu(), recherche)
                                || contient(event.getDescription(), recherche)
                                || contient(getRessourceName(event.getIdRessource()), recherche);
                        boolean correspondForum = forumSelectionne == null
                                || "Tous les forums".equals(forumSelectionne)
                                || forumSelectionne.equalsIgnoreCase(getRessourceName(event.getIdRessource()));
                        return correspondRecherche && correspondForum;
                    })
                    .sorted(Comparator.comparing(RendezVous.Evenn::getDateEvenement, Comparator.nullsLast(Comparator.naturalOrder())))
                    .collect(Collectors.toList());

            afficherListe(filteredEvents);
            mettreAJourStats(filteredEvents);
            mettreAJourGraphiques(filteredEvents);
            mettreAJourCalendrier();
        }

        private boolean contient(String source, String motCle) {
            return source != null && source.toLowerCase(FR).contains(motCle);
        }

        @FXML
        private void switchToListView() {
            if (listView != null) {
                listView.setVisible(true);
                listView.setManaged(true);
            }
            if (calendarView != null) {
                calendarView.setVisible(false);
                calendarView.setManaged(false);
            }
            setVisibleManaged(userSearchBox, true);
            setVisibleManaged(forumFilterCombo, true);
            actualiserBoutonsVue(true);
        }

        @FXML
        private void switchToCalendarView() {
            if (listView != null) {
                listView.setVisible(false);
                listView.setManaged(false);
            }
            if (calendarView != null) {
                calendarView.setVisible(true);
                calendarView.setManaged(true);
            }
            setVisibleManaged(userSearchBox, false);
            setVisibleManaged(forumFilterCombo, false);
            actualiserBoutonsVue(false);
            mettreAJourCalendrier();
        }

        private void setVisibleManaged(Node node, boolean visible) {
            if (node != null) {
                node.setVisible(visible);
                node.setManaged(visible);
            }
        }

        private void actualiserBoutonsVue(boolean listMode) {
            appliquerClasseSiBesoin(listViewBtn, "btn-tab-active", listMode);
            appliquerClasseSiBesoin(calendarViewBtn, "btn-tab-active", !listMode);
        }

        private void appliquerClasseSiBesoin(Button button, String classe, boolean active) {
            if (button == null) return;
            if (active && !button.getStyleClass().contains(classe)) {
                button.getStyleClass().add(classe);
            } else if (!active) {
                button.getStyleClass().remove(classe);
            }
        }

        private void appliquerMenuActif(Button actif) {
            List<Button> buttons = List.of(
                    menuEvenementsBtn,
                    menuRendezVousBtn,
                    menuDossiersBtn,
                    menuQuizBtn,
                    menuMedicamentsBtn
            );
            for (Button button : buttons) {
                if (button == null) continue;
                button.getStyleClass().remove("menu-btn-active");
                if (button == actif) {
                    button.getStyleClass().add("menu-btn-active");
                }
            }
        }

        private void afficherListe(List<RendezVous.Evenn> events) {
            if (eventsContainer == null) return;
            eventsContainer.getChildren().clear();

            LocalDateTime now = LocalDateTime.now();
            List<RendezVous.Evenn> upcoming = events.stream()
                    .filter(event -> event.getDateEvenement() != null && !event.getDateEvenement().isBefore(now))
                    .sorted(Comparator.comparing(RendezVous.Evenn::getDateEvenement))
                    .collect(Collectors.toList());
            List<RendezVous.Evenn> past = events.stream()
                    .filter(event -> event.getDateEvenement() == null || event.getDateEvenement().isBefore(now))
                    .sorted(Comparator.comparing(RendezVous.Evenn::getDateEvenement, Comparator.nullsLast(Comparator.reverseOrder())))
                    .collect(Collectors.toList());

            if (upcoming.isEmpty() && past.isEmpty()) {
                eventsContainer.getChildren().add(creerCarteVide());
                return;
            }

            if (!upcoming.isEmpty()) {
                eventsContainer.getChildren().add(creerSection("Événements à venir", upcoming, true));
            }
            if (!past.isEmpty()) {
                eventsContainer.getChildren().add(creerSection("Événements passés", past, false));
            }
        }

        private VBox creerSection(String titre, List<RendezVous.Evenn> events, boolean upcoming) {
            VBox section = new VBox(16);
            HBox header = new HBox(8);
            header.setAlignment(Pos.CENTER_LEFT);
            Label dot = new Label("●");
            dot.getStyleClass().add(upcoming ? "dot-upcoming" : "dot-past");
            Label titleLabel = new Label(titre);
            titleLabel.getStyleClass().add("section-title");
            header.getChildren().addAll(dot, titleLabel);

            FlowPane flowPane = new FlowPane();
            flowPane.setHgap(20);
            flowPane.setVgap(20);
            flowPane.setPrefWrapLength(1080);

            for (RendezVous.Evenn event : events) {
                flowPane.getChildren().add(creerCarte(event, upcoming));
            }

            section.getChildren().addAll(header, flowPane);
            return section;
        }

        private VBox creerCarteVide() {
            VBox box = new VBox(10);
            box.setAlignment(Pos.CENTER);
            box.setPadding(new Insets(30));
            box.getStyleClass().add("empty-state-card");
            Label title = new Label("Aucun événement trouvé");
            title.getStyleClass().add("empty-state-title");
            Label message = new Label("Essayez une autre recherche ou changez le filtre du forum.");
            message.getStyleClass().add("empty-state-text");
            box.getChildren().addAll(title, message);
            return box;
        }

        private VBox creerCarte(RendezVous.Evenn event, boolean upcoming) {
            VBox card = new VBox(14);
            card.setPadding(new Insets(22));
            card.setPrefWidth(340);
            card.setMinWidth(320);
            card.getStyleClass().add("event-card");

            HBox top = new HBox();
            top.setAlignment(Pos.CENTER_LEFT);
            Label badge = new Label(upcoming ? "À venir" : "Passé");
            badge.getStyleClass().add(upcoming ? "event-badge-upcoming" : "event-badge-past");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Label icon = new Label("📅");
            icon.getStyleClass().add("event-icon-box");
            top.getChildren().addAll(badge, spacer, icon);

            Label titre = new Label(valeurOuDefaut(event.getTitre(), "Sans titre"));
            titre.setWrapText(true);
            titre.getStyleClass().add("event-title");

            Separator separator = new Separator();
            separator.getStyleClass().add("soft-separator");

            VBox details = new VBox(10);
            details.getChildren().addAll(
                    creerLigneDetail("📅", "Date", event.getDateEvenement() != null ? event.getDateEvenement().format(FMT_DATE_LONG) : "Non précisée"),
                    creerLigneDetail("🕒", "Heure", event.getDateEvenement() != null ? event.getDateEvenement().format(FMT_TIME) : "Non précisée"),
                    creerLigneDetail("📍", "Lieu", valeurOuDefaut(event.getLieu(), "Non précisé")),
                    creerLigneDetail("🛡", "Forum", getRessourceName(event.getIdRessource()))
            );

            Label description = new Label(valeurOuDefaut(event.getDescription(), "Aucune description"));
            description.setWrapText(true);
            description.getStyleClass().add("event-description");

            HBox actions = new HBox(10);

            Button detailsBtn = new Button("Voir détails");
            detailsBtn.getStyleClass().add("btn-primary");
            detailsBtn.setOnAction(e -> afficherDetails(event));

            Button shareBtn = new Button("Partager");
            shareBtn.getStyleClass().add("btn-outline");
            shareBtn.setOnAction(e -> partagerEvenement(event));

            Button agendaBtn = new Button("Ajouter");
            agendaBtn.getStyleClass().add("btn-soft");
            agendaBtn.setOnAction(e -> ajouterAMonAgenda(event));

            Button reserverBtn = new Button("📝 Réserver");
            reserverBtn.getStyleClass().add("btn-success");
            reserverBtn.setOnAction(e -> ouvrirFormulaireReservation(event));

            actions.getChildren().addAll(detailsBtn, shareBtn, agendaBtn, reserverBtn);

            card.getChildren().addAll(top, titre, separator, details, description, actions);
            return card;
        }

        private HBox creerLigneDetail(String iconText, String labelText, String valueText) {
            HBox box = new HBox(8);
            box.setAlignment(Pos.CENTER_LEFT);
            Label icon = new Label(iconText);
            icon.getStyleClass().add("detail-icon");
            Label label = new Label(labelText + " :");
            label.getStyleClass().add("detail-label");
            Label value = new Label(valueText);
            value.setWrapText(true);
            value.getStyleClass().add("detail-value");
            box.getChildren().addAll(icon, label, value);
            return box;
        }

        private String valeurOuDefaut(String valeur, String fallback) {
            return valeur != null && !valeur.isBlank() ? valeur.trim() : fallback;
        }

        private String getRessourceName(int idRessource) {
            return ressources.stream()
                    .filter(Objects::nonNull)
                    .filter(ressource -> ressource.getIdRessource() == idRessource)
                    .map(RendezVous.Ressource::getNomRessource)
                    .findFirst()
                    .orElse("Forum " + idRessource);
        }

        private void ouvrirFormulaireReservation(RendezVous.Evenn event) {
            Dialog<RendezVous.ReservationPersonne> dialog = new Dialog<>();
            dialog.setTitle("Réserver un événement");
            dialog.setHeaderText("Inscription à : " + event.getTitre());

            ButtonType btnReserver = new ButtonType("Réserver", ButtonBar.ButtonData.OK_DONE);
            ButtonType btnAnnuler = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
            dialog.getDialogPane().getButtonTypes().addAll(btnReserver, btnAnnuler);

            GridPane grid = new GridPane();
            grid.setHgap(12);
            grid.setVgap(12);
            grid.setPadding(new Insets(20));

            TextField nomField = new TextField();
            nomField.setPromptText("Nom complet *");
            nomField.setPrefWidth(300);

            TextField emailField = new TextField();
            emailField.setPromptText("Adresse email *");
            emailField.setPrefWidth(300);

            TextField telephoneField = new TextField();
            telephoneField.setPromptText("Numéro de téléphone");
            telephoneField.setPrefWidth(300);

            TextArea commentaireArea = new TextArea();
            commentaireArea.setPromptText("Commentaire / demande particulière");
            commentaireArea.setPrefRowCount(3);
            commentaireArea.setWrapText(true);
            commentaireArea.setPrefWidth(300);

            grid.add(new Label("Nom complet :"), 0, 0);
            grid.add(nomField, 1, 0);
            grid.add(new Label("Email :"), 0, 1);
            grid.add(emailField, 1, 1);
            grid.add(new Label("Téléphone :"), 0, 2);
            grid.add(telephoneField, 1, 2);
            grid.add(new Label("Commentaire :"), 0, 3);
            grid.add(commentaireArea, 1, 3);

            dialog.getDialogPane().setContent(grid);

            Node btnReserverNode = dialog.getDialogPane().lookupButton(btnReserver);
            btnReserverNode.setDisable(true);

            nomField.textProperty().addListener((obs, old, val) ->
                    btnReserverNode.setDisable(val.isBlank() || emailField.getText().isBlank())
            );
            emailField.textProperty().addListener((obs, old, val) ->
                    btnReserverNode.setDisable(nomField.getText().isBlank() || val.isBlank())
            );

            dialog.setResultConverter(buttonType -> {
                if (buttonType == btnReserver) {
                    if (nomField.getText().trim().isEmpty() || emailField.getText().trim().isEmpty()) {
                        showAlert("Champs requis", "Veuillez remplir votre nom et email.", Alert.AlertType.WARNING);
                        return null;
                    }
                    if (!emailField.getText().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                        showAlert("Email invalide", "Veuillez saisir un email valide.", Alert.AlertType.WARNING);
                        return null;
                    }
                    return new RendezVous.ReservationPersonne(
                            event.getId_Evenn(),
                            nomField.getText().trim(),
                            emailField.getText().trim(),
                            telephoneField.getText().trim(),
                            commentaireArea.getText().trim()
                    );
                }
                return null;
            });

            dialog.showAndWait().ifPresent(reservation -> {
                try {
                    srp.add(reservation);
                    showAlert("Réservation confirmée !",
                            "Votre réservation pour l'événement \"" + event.getTitre() + "\" a été enregistrée.\n" +
                                    "Vous recevrez une confirmation par email sous 24h.",
                            Alert.AlertType.INFORMATION);
                } catch (SQLException e) {
                    showAlert("Erreur", "Impossible d'enregistrer la réservation : " + e.getMessage(), Alert.AlertType.ERROR);
                }
            });
        }

        private void afficherDetails(RendezVous.Evenn event) {
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            if (rootPane != null && rootPane.getScene() != null) {
                dialog.initOwner(rootPane.getScene().getWindow());
            }
            dialog.setTitle("Détails de l'événement");

            VBox root = new VBox();
            root.getStyleClass().add("details-modal-root");

            HBox header = new HBox();
            header.setAlignment(Pos.CENTER_LEFT);
            header.setPadding(new Insets(20));
            header.getStyleClass().add("details-modal-header");

            VBox titleBox = new VBox(4);
            Label eyebrow = new Label("Détails de l'événement");
            eyebrow.getStyleClass().add("details-eyebrow");
            Label title = new Label(valeurOuDefaut(event.getTitre(), "Sans titre"));
            title.getStyleClass().add("details-title");
            titleBox.getChildren().addAll(eyebrow, title);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button closeTop = new Button("Fermer ✕");
            closeTop.getStyleClass().add("btn-modal-close");
            closeTop.setOnAction(e -> dialog.close());

            header.getChildren().addAll(titleBox, spacer, closeTop);

            VBox content = new VBox(18);
            content.setPadding(new Insets(24));

            Label status = new Label(event.getDateEvenement() != null && !event.getDateEvenement().isBefore(LocalDateTime.now()) ? "À venir" : "Passé");
            status.getStyleClass().add(event.getDateEvenement() != null && !event.getDateEvenement().isBefore(LocalDateTime.now())
                    ? "event-badge-upcoming"
                    : "event-badge-past");

            VBox infoCard = new VBox(12);
            infoCard.getStyleClass().add("details-card");
            Label infoTitle = new Label("Informations");
            infoTitle.getStyleClass().add("details-section-title");
            infoCard.getChildren().addAll(
                    infoTitle,
                    creerLigneDetail("📅", "Date", event.getDateEvenement() != null ? event.getDateEvenement().format(FMT_DATE_LONG) : "Non précisée"),
                    creerLigneDetail("🕒", "Heure", event.getDateEvenement() != null ? event.getDateEvenement().format(FMT_TIME) : "Non précisée"),
                    creerLigneDetail("📍", "Lieu", valeurOuDefaut(event.getLieu(), "Non précisé")),
                    creerLigneDetail("🛡", "Forum", getRessourceName(event.getIdRessource()))
            );

            VBox descCard = new VBox(12);
            descCard.getStyleClass().add("details-card");
            Label descTitle = new Label("Description");
            descTitle.getStyleClass().add("details-section-title");
            Label descValue = new Label(valeurOuDefaut(event.getDescription(), "Aucune description"));
            descValue.setWrapText(true);
            descValue.getStyleClass().add("details-description");
            descCard.getChildren().addAll(descTitle, descValue);

            HBox actions = new HBox(12);

            Button reserverModalBtn = new Button("📝 Réserver");
            reserverModalBtn.getStyleClass().add("btn-success");
            reserverModalBtn.setOnAction(e -> {
                dialog.close();
                ouvrirFormulaireReservation(event);
            });

            Button addBtn = new Button("Ajouter à mon agenda");
            addBtn.getStyleClass().add("btn-primary");
            addBtn.setOnAction(e -> ajouterAMonAgenda(event));

            Button shareBtn = new Button("Partager");
            shareBtn.getStyleClass().add("btn-outline");
            shareBtn.setOnAction(e -> partagerEvenement(event));

            Button closeBtn = new Button("Fermer");
            closeBtn.getStyleClass().add("btn-soft");
            closeBtn.setOnAction(e -> dialog.close());

            actions.getChildren().addAll(reserverModalBtn, addBtn, shareBtn, closeBtn);

            content.getChildren().addAll(status, infoCard, descCard, actions);

            ScrollPane scrollPane = new ScrollPane(content);
            scrollPane.setFitToWidth(true);
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scrollPane.getStyleClass().add("transparent-scroll");

            root.getChildren().addAll(header, scrollPane);

            Scene scene = new Scene(root, 760, 560, Color.TRANSPARENT);
            URL cssUrl = getClass().getResource("/styles/user-evenement.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }
            dialog.setScene(scene);
            dialog.showAndWait();
        }

        private void partagerEvenement(RendezVous.Evenn event) {
            String resume = String.format(
                    "%s | %s à %s | %s | %s",
                    valeurOuDefaut(event.getTitre(), "Sans titre"),
                    event.getDateEvenement() != null ? event.getDateEvenement().format(FMT_DATE_LONG) : "Date non précisée",
                    event.getDateEvenement() != null ? event.getDateEvenement().format(FMT_TIME) : "Heure non précisée",
                    valeurOuDefaut(event.getLieu(), "Lieu non précisé"),
                    getRessourceName(event.getIdRessource())
            );
            ClipboardContent content = new ClipboardContent();
            content.putString(resume);
            Clipboard.getSystemClipboard().setContent(content);
            showAlert("Partager", "Les détails de l'événement ont été copiés dans le presse-papiers.");
        }

        private void ajouterAMonAgenda(RendezVous.Evenn event) {
            showAlert("Agenda", "L'événement « " + valeurOuDefaut(event.getTitre(), "Sans titre") + " » a été ajouté à votre agenda.");
        }

        @FXML
        private void prevMonth() {
            currentMonth = currentMonth.minusMonths(1);
            mettreAJourCalendrier();
        }

        @FXML
        private void nextMonth() {
            currentMonth = currentMonth.plusMonths(1);
            mettreAJourCalendrier();
        }

        @FXML
        private void goToToday() {
            currentMonth = YearMonth.now();
            mettreAJourCalendrier();
        }

        private void mettreAJourCalendrier() {
            if (calendarGrid == null) return;

            calendarGrid.getChildren().clear();
            calendarGrid.getColumnConstraints().clear();
            calendarGrid.getRowConstraints().clear();

            for (int i = 0; i < 7; i++) {
                ColumnConstraints column = new ColumnConstraints();
                column.setPercentWidth(100.0 / 7.0);
                column.setHgrow(Priority.ALWAYS);
                calendarGrid.getColumnConstraints().add(column);
            }

            for (int i = 0; i < 7; i++) {
                RowConstraints row = new RowConstraints();
                row.setVgrow(Priority.ALWAYS);
                calendarGrid.getRowConstraints().add(row);
            }

            if (monthYearLabel != null) {
                String monthText = currentMonth.getMonth().getDisplayName(TextStyle.FULL, FR);
                monthYearLabel.setText(Character.toUpperCase(monthText.charAt(0)) + monthText.substring(1) + " " + currentMonth.getYear());
            }

            String[] jours = {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"};
            for (int i = 0; i < jours.length; i++) {
                Label header = new Label(jours[i]);
                header.getStyleClass().add("calendar-header");
                header.setMaxWidth(Double.MAX_VALUE);
                header.setAlignment(Pos.CENTER);
                calendarGrid.add(header, i, 0);
            }

            LocalDate firstDay = currentMonth.atDay(1);
            int startColumn = firstDay.getDayOfWeek().getValue() - 1;
            int daysInMonth = currentMonth.lengthOfMonth();
            LocalDate today = LocalDate.now();

            int row = 1;
            int column = startColumn;

            for (int day = 1; day <= daysInMonth; day++) {
                LocalDate currentDate = currentMonth.atDay(day);
                boolean isToday = currentDate.equals(today);
                List<RendezVous.Evenn> dayEvents = filteredEvents.stream()
                        .filter(event -> event.getDateEvenement() != null)
                        .filter(event -> event.getDateEvenement().toLocalDate().equals(currentDate))
                        .collect(Collectors.toList());

                VBox cell = creerCelluleCalendrier(currentDate, isToday, dayEvents);
                calendarGrid.add(cell, column, row);

                column++;
                if (column == 7) {
                    column = 0;
                    row++;
                }
            }
        }

        private VBox creerCelluleCalendrier(LocalDate date, boolean isToday, List<RendezVous.Evenn> events) {
            VBox cell = new VBox(6);
            cell.setPadding(new Insets(10));
            cell.setMinHeight(96);
            cell.getStyleClass().add("calendar-cell");
            if (isToday) {
                cell.getStyleClass().add("calendar-cell-today");
            }

            Label number = new Label(String.valueOf(date.getDayOfMonth()));
            number.getStyleClass().add(isToday ? "calendar-day-number-today" : "calendar-day-number");
            cell.getChildren().add(number);

            for (int i = 0; i < Math.min(events.size(), 2); i++) {
                RendezVous.Evenn event = events.get(i);
                Label chip = new Label(valeurOuDefaut(event.getTitre(), "Sans titre"));
                chip.getStyleClass().add("calendar-event-chip");
                chip.setMaxWidth(Double.MAX_VALUE);
                cell.getChildren().add(chip);
            }

            if (events.size() > 2) {
                Label more = new Label("+" + (events.size() - 2) + " autres");
                more.getStyleClass().add("calendar-more-label");
                cell.getChildren().add(more);
            }

            cell.setOnMouseClicked(e -> {
                if (!events.isEmpty()) {
                    afficherDetails(events.get(0));
                }
            });

            return cell;
        }

        private void mettreAJourStats(List<RendezVous.Evenn> events) {
            long total = events.size();
            long upcoming = events.stream()
                    .filter(event -> event.getDateEvenement() != null && !event.getDateEvenement().isBefore(LocalDateTime.now()))
                    .count();
            long locations = events.stream()
                    .map(RendezVous.Evenn::getLieu)
                    .filter(Objects::nonNull)
                    .filter(lieu -> !lieu.isBlank())
                    .distinct()
                    .count();
            int rate = total > 0 ? (int) Math.round((double) upcoming / total * 100) : 0;

            if (statsLabel != null) statsLabel.setText(String.valueOf(upcoming));
            if (totalEventsLabel != null) totalEventsLabel.setText(String.valueOf(total));
            if (locationsCountLabel != null) locationsCountLabel.setText(String.valueOf(locations));
            if (rateLabel != null) rateLabel.setText(rate + "%");
        }

        private void mettreAJourGraphiques(List<RendezVous.Evenn> events) {
            long total = events.size();
            long upcoming = events.stream()
                    .filter(event -> event.getDateEvenement() != null && !event.getDateEvenement().isBefore(LocalDateTime.now()))
                    .count();
            long past = Math.max(0, total - upcoming);

            if (donutTotalLabel != null) donutTotalLabel.setText(String.valueOf(total));
            if (donutUpcomingLabel != null) donutUpcomingLabel.setText("À venir — " + upcoming);
            if (donutPastLabel != null) donutPastLabel.setText("Passés — " + past);

            if (donutProgressCircle != null) {
                double radius = donutProgressCircle.getRadius();
                double circumference = 2 * Math.PI * radius;
                double progress = total == 0 ? 0 : circumference * ((double) upcoming / total);
                donutProgressCircle.getStrokeDashArray().setAll(progress, Math.max(0, circumference - progress));
            }

            genererBarresMensuelles(events);
        }

        private void genererBarresMensuelles(List<RendezVous.Evenn> events) {
            if (monthlyBarsContainer == null) return;
            monthlyBarsContainer.getChildren().clear();

            YearMonth nowMonth = YearMonth.now();
            List<YearMonth> months = new ArrayList<>();
            for (int i = 5; i >= 0; i--) {
                months.add(nowMonth.minusMonths(i));
            }

            long max = months.stream()
                    .mapToLong(month -> events.stream()
                            .filter(event -> event.getDateEvenement() != null)
                            .filter(event -> YearMonth.from(event.getDateEvenement()).equals(month))
                            .count())
                    .max()
                    .orElse(1);

            for (YearMonth month : months) {
                long count = events.stream()
                        .filter(event -> event.getDateEvenement() != null)
                        .filter(event -> YearMonth.from(event.getDateEvenement()).equals(month))
                        .count();

                VBox column = new VBox(8);
                column.setAlignment(Pos.BOTTOM_CENTER);
                column.setPrefWidth(58);

                Label countLabel = new Label(String.valueOf(count));
                countLabel.getStyleClass().add("bar-count-label");

                Region bar = new Region();
                bar.getStyleClass().add("month-bar");
                double height = 12 + (max == 0 ? 0 : ((double) count / max) * 110);
                bar.setPrefSize(40, height);
                bar.setMinHeight(height);

                Label monthLabel = new Label(capitalize(month.getMonth().getDisplayName(TextStyle.SHORT, FR).replace(".", "")));
                monthLabel.getStyleClass().add("bar-month-label");

                column.getChildren().addAll(countLabel, bar, monthLabel);
                monthlyBarsContainer.getChildren().add(column);
            }
        }

        private String capitalize(String value) {
            if (value == null || value.isBlank()) return "";
            return Character.toUpperCase(value.charAt(0)) + value.substring(1);
        }

        @FXML
        private void menuEvenements() {
            appliquerMenuActif(menuEvenementsBtn);
            switchToListView();
        }

        @FXML
        private void menuRendezVous() {
            appliquerMenuActif(menuRendezVousBtn);
            showAlert("Rendez-vous", "Section prête à être reliée à vos rendez-vous.");
        }

        @FXML
        private void menuDossiers() {
            appliquerMenuActif(menuDossiersBtn);
            showAlert("Dossiers médicaux", "Section prête à être reliée aux dossiers médicaux.");
        }

        @FXML
        private void menuQuiz() {
            appliquerMenuActif(menuQuizBtn);
            showAlert("Quiz communauté", "Section prête à être reliée au quiz communauté.");
        }

        @FXML
        private void menuMedicaments() {
            appliquerMenuActif(menuMedicamentsBtn);
            showAlert("Médicaments", "Section prête à être reliée aux médicaments.");
        }

        @FXML
        private void activerNotifications() {
            showAlert("Notifications", "Les notifications des événements ont été activées.");
        }

        @FXML
        private void exporterMonCalendrier() {
            showAlert("Exporter", "L'export du calendrier est prêt.");
        }

        @FXML
        private void deconnecter() {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Déconnexion");
            confirm.setHeaderText(null);
            confirm.setContentText("Voulez-vous vraiment vous déconnecter ?");
            confirm.showAndWait().ifPresent(result -> {
                if (result == ButtonType.OK) {
                    Stage stage = (Stage) rootPane.getScene().getWindow();
                    stage.close();
                    Mainfx.naviguerVers("/Login.fxml", "Connexion - VITA");
                }
            });
        }

        // ==================== SCANNER QR CODE ====================

        @FXML
        private void ouvrirScannerUser() {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/scanneruser.fxml"));
                AnchorPane root = loader.load();

                RendezVousController.ScannerUserController controller = loader.getController();
                controller.setOnEventFoundCallback(() -> {
                    // Rien à faire ici car la méthode afficherDetails est appelée dans le controller
                });

                Stage stage = new Stage();
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.setTitle("Scanner un événement");
                stage.setScene(new Scene(root));
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
                showAlert("Erreur", "Impossible d'ouvrir le scanner: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }

        // ==================== MES RÉSERVATIONS ====================

        @FXML
        private void ouvrirMesReservations() {
            try {
                String userEmail = getUserEmail();
                List<RendezVous.ReservationPersonne> mesReservations = srp.getByEmail(userEmail);

                if (mesReservations.isEmpty()) {
                    showAlert("Mes réservations", "Vous n'avez aucune réservation.");
                    return;
                }

                Stage stage = new Stage();
                stage.setTitle("Mes réservations - QR Codes");
                stage.initModality(Modality.APPLICATION_MODAL);

                VBox root = new VBox(15);
                root.setPadding(new Insets(20));
                root.setStyle("-fx-background-color: white; -fx-border-radius: 15;");

                Label title = new Label("📱 Mes QR Codes");
                title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #5A1730;");

                ListView<RendezVous.ReservationPersonne> listView = new ListView<>();
                listView.setItems(FXCollections.observableArrayList(mesReservations));
                listView.setCellFactory(lv -> new ListCell<RendezVous.ReservationPersonne>() {
                    @Override
                    protected void updateItem(RendezVous.ReservationPersonne r, boolean empty) {
                        super.updateItem(r, empty);
                        if (empty || r == null) {
                            setText(null);
                            setGraphic(null);
                            return;
                        }

                        VBox card = new VBox(10);
                        card.setPadding(new Insets(15));
                        card.setStyle("-fx-background-color: #fdf5f7; -fx-background-radius: 10; -fx-border-color: #ead8de; -fx-border-radius: 10;");

                        String eventTitle = getEventTitle(r.getIdEvenement());
                        Label eventLabel = new Label(eventTitle);
                        eventLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #5A1730;");

                        Label statusLabel = new Label(getStatutText(r.getStatut()));
                        statusLabel.setStyle(getStatutStyle(r.getStatut()));

                        if ("ACCEPTE".equals(r.getStatut())) {
                            Button qrBtn = new Button("📱 Voir mon QR code");
                            qrBtn.setStyle("-fx-background-color: #6b1a2a; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 8 16; -fx-cursor: hand;");
                            qrBtn.setOnAction(e -> afficherQRCodeUser(r));
                            card.getChildren().addAll(eventLabel, statusLabel, qrBtn);
                        } else {
                            Label infoLabel = new Label(getStatutMessage(r.getStatut()));
                            infoLabel.setStyle("-fx-text-fill: #A36277; -fx-font-size: 11px;");
                            card.getChildren().addAll(eventLabel, statusLabel, infoLabel);
                        }

                        setGraphic(card);
                    }
                });

                Button btnFermer = new Button("Fermer");
                btnFermer.setStyle("-fx-background-color: #8B1538; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16; -fx-cursor: hand;");
                btnFermer.setOnAction(e -> stage.close());

                root.getChildren().addAll(title, listView, btnFermer);

                Scene scene = new Scene(root, 450, 500);
                stage.setScene(scene);
                stage.show();

            } catch (SQLException e) {
                showAlert("Erreur", "Impossible de charger vos réservations: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }

        private String getUserEmail() {
            // À modifier selon votre système d'authentification
            // Pour tester, utilisez un email qui existe dans votre base
            return "test@test.com";
        }


        private String getEventTitle(int eventId) {
            try {
                RendezVous.Evenn ev = serviceEvenn.getById(eventId);
                return ev != null ? ev.getTitre() : "Événement inconnu";
            } catch (SQLException e) {
                return "Événement inconnu";
            }
        }

        private String getStatutText(String statut) {
            switch (statut) {
                case "ACCEPTE": return "✅ Accepté";
                case "REFUSE": return "❌ Refusé";
                default: return "⏳ En attente";
            }
        }

        private String getStatutMessage(String statut) {
            switch (statut) {
                case "ACCEPTE": return "Votre réservation est acceptée. Cliquez sur le bouton pour voir votre QR code.";
                case "REFUSE": return "Votre réservation a été refusée.";
                default: return "Votre réservation est en attente de confirmation.";
            }
        }

        private String getStatutStyle(String statut) {
            switch (statut) {
                case "ACCEPTE": return "-fx-background-color: #d5f5e3; -fx-text-fill: #27ae60; -fx-padding: 4 10; -fx-background-radius: 12; -fx-font-size: 11px;";
                case "REFUSE": return "-fx-background-color: #f5d5dc; -fx-text-fill: #c1283e; -fx-padding: 4 10; -fx-background-radius: 12; -fx-font-size: 11px;";
                default: return "-fx-background-color: #fef5e7; -fx-text-fill: #f39c12; -fx-padding: 4 10; -fx-background-radius: 12; -fx-font-size: 11px;";
            }
        }

        private void afficherQRCodeUser(RendezVous.ReservationPersonne r) {
            Stage stage = new Stage();
            stage.setTitle("Mon QR Code - " + r.getNomComplet());
            stage.initModality(Modality.APPLICATION_MODAL);

            VBox root = new VBox(15);
            root.setPadding(new Insets(20));
            root.setStyle("-fx-background-color: white; -fx-border-radius: 15;");
            root.setAlignment(Pos.CENTER);

            // Rendre qrImage final
            final ImageView qrImage = QRCodeService.generateReservationQRCode(r.getId());
            if (qrImage != null) {
                qrImage.setFitWidth(200);
                qrImage.setFitHeight(200);
                root.getChildren().add(qrImage);
            }

            Label title = new Label(getEventTitle(r.getIdEvenement()));
            title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #5A1730;");
            title.setWrapText(true);
            title.setAlignment(Pos.CENTER);

            Label nameLabel = new Label(r.getNomComplet());
            nameLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #6b1a2a;");

            Label instruction = new Label("Présentez ce QR code à l'entrée de l'événement");
            instruction.setStyle("-fx-font-size: 11px; -fx-text-fill: #A36277;");

            Button btnFermer = new Button("Fermer");
            btnFermer.setStyle("-fx-background-color: #8B1538; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16; -fx-cursor: hand;");
            btnFermer.setOnAction(e -> stage.close());

            root.getChildren().addAll(title, nameLabel, instruction, btnFermer);

            Scene scene = new Scene(root, 350, 500);
            stage.setScene(scene);
            stage.show();
        }

        private void telechargerQRCode(ImageView qrImage, String nom) {
            if (qrImage == null) {
                showAlert("Erreur", "Impossible de télécharger le QR code", Alert.AlertType.ERROR);
                return;
            }

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Enregistrer le QR code");
            fileChooser.setInitialFileName("QR_Code_" + nom.replaceAll(" ", "_") + ".png");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("PNG Image", "*.png")
            );

            File file = fileChooser.showSaveDialog(null);
            if (file != null) {
                try {
                    WritableImage image = qrImage.snapshot(null, null);
                    BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
                    ImageIO.write(bufferedImage, "png", file);
                    showAlert("Succès", "QR code sauvegardé : " + file.getName(), Alert.AlertType.INFORMATION);
                } catch (IOException ex) {
                    showAlert("Erreur", "Impossible de sauvegarder: " + ex.getMessage(), Alert.AlertType.ERROR);
                }
            }
        }

        // ==================== MÉTHODES UTILITAIRES ====================

        private void showAlert(String titre, String contenu) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(titre);
            alert.setHeaderText(null);
            alert.setContentText(contenu);
            alert.showAndWait();
        }

        private void showAlert(String titre, String contenu, Alert.AlertType type) {
            Alert alert = new Alert(type);
            alert.setTitle(titre);
            alert.setHeaderText(null);
            alert.setContentText(contenu);
            alert.showAndWait();
        }
    }
}