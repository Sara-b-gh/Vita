package controles;

import entities.Evenn;
import entities.Ressource;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import services.ServiceEvenn;
import services.ServiceRessource;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class GestionRessourceController {

    private static final Logger logger = Logger.getLogger(GestionRessourceController.class.getName());

    /* ========================================================= */
    /* FXML                                                      */
    /* ========================================================= */

    @FXML private Label adminNameLabel;

    @FXML private TextField searchField;

    /*
     * Nouveau filtre optionnel.
     * Si tu ne l’as pas encore dans le FXML, ce champ restera null sans erreur.
     */
    @FXML private ComboBox<String> typeFilterCombo;

    @FXML private ComboBox<String> eventFilterCombo;
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private Button exportBtn;

    @FXML private Button btnVueListe;
    @FXML private Button btnVueStats;
    @FXML private Button btnVueGrille;

    @FXML private VBox sectionListe;
    @FXML private VBox sectionStats;
    @FXML private VBox sectionGrille;

    @FXML private Label statTotalLabel;
    @FXML private Label statCoutLabel;
    @FXML private Label statManquantLabel;
    @FXML private Label tauxDispoLabel;
    @FXML private Label tendanceTotalLabel;
    @FXML private Label coutMoyenLabel;
    @FXML private Label evtImpactesLabel;
    @FXML private Label irlLabel;
    @FXML private Label countLabel;
    @FXML private Label listCountBadge;
    @FXML private Label gridCountBadge;
    @FXML private ProgressBar dispoProgress;

    @FXML private HBox alertBand;
    @FXML private Label alertTitleLabel;
    @FXML private Label alertSubLabel;

    @FXML private Canvas repartitionCanvas;
    @FXML private Canvas risqueCanvas;

    /*
     * Important :
     * On n’affiche plus directement Ressource dans la ListView.
     * On affiche RessourceRow, pour pouvoir insérer des titres de groupe.
     */
    @FXML private ListView<RessourceRow> ressourceListView;

    @FXML private FlowPane ressourceGridPane;

    @FXML private StackPane notificationOverlay;
    @FXML private VBox notificationPanel;
    @FXML private VBox notifListContainer;
    @FXML private Label notifTotalBadge;
    @FXML private Label notifCountBadge;
    @FXML private Label notifSubheaderLabel;

    @FXML private VBox toastContainer;

    /* ========================================================= */
    /* SERVICES                                                  */
    /* ========================================================= */

    private final ServiceRessource sr = new ServiceRessource();
    private final ServiceEvenn se = new ServiceEvenn();

    private List<Ressource> allRessources = List.of();
    private List<Ressource> currentRessources = List.of();
    private List<Evenn> allEvenements = List.of();

    /* ========================================================= */
    /* THEME                                                     */
    /* ========================================================= */

    private static final String BORDEAUX = "#9B0F3E";
    private static final String BORDEAUX_DARK = "#4B1026";
    private static final String TEXT_MUTED = "#8D5267";
    private static final String GREEN = "#2E7D32";
    private static final String ORANGE = "#D98217";
    private static final String RED = "#C8204F";
    private static final String BLUE = "#1565C0";

    /* ========================================================= */
    /* ETATS                                                     */
    /* ========================================================= */

    private enum ViewMode {
        LISTE,
        STATS,
        GRILLE
    }

    private ViewMode currentView = ViewMode.LISTE;

    /*
     * Types métiers.
     */
    private enum ResourceType {
        MATERIELLE("Matérielle"),
        LOGICIELLE("Logicielle"),
        HUMAINE("Humaine"),
        NON_CLASSEE("Non classée");

        private final String label;

        ResourceType(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }

        static ResourceType fromLabel(String value) {
            if (value == null) return NON_CLASSEE;

            String v = value.trim().toLowerCase(Locale.ROOT);

            if (v.contains("mat")) return MATERIELLE;
            if (v.contains("log")) return LOGICIELLE;
            if (v.contains("hum")) return HUMAINE;

            return NON_CLASSEE;
        }
    }

    /*
     * Ligne affichée dans la ListView :
     * soit un header de groupe, soit une ressource.
     */
    private static class RessourceRow {
        private final boolean header;
        private final String headerTitle;
        private final Ressource ressource;

        private RessourceRow(boolean header, String headerTitle, Ressource ressource) {
            this.header = header;
            this.headerTitle = headerTitle;
            this.ressource = ressource;
        }

        static RessourceRow header(String title) {
            return new RessourceRow(true, title, null);
        }

        static RessourceRow resource(Ressource ressource) {
            return new RessourceRow(false, null, ressource);
        }

        boolean isHeader() {
            return header;
        }

        String getHeaderTitle() {
            return headerTitle;
        }

        Ressource getRessource() {
            return ressource;
        }
    }

    /* ========================================================= */
    /* INITIALISATION                                            */
    /* ========================================================= */

    @FXML
    public void initialize() {
        Locale.setDefault(Locale.FRANCE);

        if (adminNameLabel != null) {
            adminNameLabel.setText("Administrateur");
        }

        initialiserFiltres();
        initialiserNotifications();
        configurerListe();

        afficherVueListe();
        chargerDonnees();
    }

    private void initialiserFiltres() {
        if (typeFilterCombo != null) {
            typeFilterCombo.setItems(FXCollections.observableArrayList(
                    "Tous les types",
                    "Matérielle",
                    "Logicielle",
                    "Humaine",
                    "Non classée"
            ));
            typeFilterCombo.setValue("Tous les types");
            typeFilterCombo.valueProperty().addListener((obs, oldValue, newValue) -> appliquerFiltres());
        }

        if (eventFilterCombo != null) {
            eventFilterCombo.setItems(FXCollections.observableArrayList(
                    "Tous les événements",
                    "Non affectées"
            ));
            eventFilterCombo.setValue("Tous les événements");
            eventFilterCombo.valueProperty().addListener((obs, oldValue, newValue) -> appliquerFiltres());
        }

        if (statusFilterCombo != null) {
            statusFilterCombo.setItems(FXCollections.observableArrayList(
                    "Tous",
                    "Disponibles",
                    "Affectées",
                    "Non affectées",
                    "En tension",
                    "Confirmée",
                    "En attente",
                    "Livrée",
                    "Non disponible"
            ));
            statusFilterCombo.setValue("Tous");
            statusFilterCombo.valueProperty().addListener((obs, oldValue, newValue) -> appliquerFiltres());
        }

        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldValue, newValue) -> appliquerFiltres());
        }
    }

    private void initialiserNotifications() {
        setVisibleManaged(notificationOverlay, false);
    }

    /* ========================================================= */
    /* VUES                                                      */
    /* ========================================================= */

    @FXML
    public void afficherVueListe() {
        currentView = ViewMode.LISTE;

        setVisibleManaged(sectionListe, true);
        setVisibleManaged(sectionStats, false);
        setVisibleManaged(sectionGrille, false);

        activerBoutonVue(btnVueListe);
        appliquerFiltres();
    }

    @FXML
    public void afficherVueStats() {
        currentView = ViewMode.STATS;

        setVisibleManaged(sectionListe, false);
        setVisibleManaged(sectionStats, true);
        setVisibleManaged(sectionGrille, false);

        activerBoutonVue(btnVueStats);
        appliquerFiltres();
        mettreAJourKPI();
        rafraichirGraphiques();
    }

    @FXML
    public void afficherVueGrille() {
        currentView = ViewMode.GRILLE;

        setVisibleManaged(sectionListe, false);
        setVisibleManaged(sectionStats, false);
        setVisibleManaged(sectionGrille, true);

        activerBoutonVue(btnVueGrille);
        appliquerFiltres();
        afficherRessourcesGrille(currentRessources);
    }

    private void activerBoutonVue(Button activeButton) {
        for (Button button : new Button[]{btnVueStats, btnVueListe, btnVueGrille}) {
            if (button == null) continue;

            button.getStyleClass().removeAll("segmented", "segmented-active");
            button.getStyleClass().add(button == activeButton ? "segmented-active" : "segmented");
        }
    }

    /* ========================================================= */
    /* NAVIGATION                                                */
    /* ========================================================= */

    @FXML public void allerTableauBord()      { naviguerVers("/TableauBord.fxml"); }
    @FXML public void allerEvenements()       { naviguerVers("/AdminEvenement.fxml"); }
    @FXML public void allerRessources()       { chargerDonnees(); }
    @FXML public void allerRendezVous()       { naviguerVers("/RendezVous.fxml"); }
    @FXML public void allerDossiersMedicaux() { naviguerVers("/DossiersMedicaux.fxml"); }
    @FXML public void allerQuiz()             { naviguerVers("/Quiz.fxml"); }
    @FXML public void allerMedicaments()      { naviguerVers("/Medicaments.fxml"); }
    @FXML public void allerEquipements()      { naviguerVers("/Equipements.fxml"); }

    private void naviguerVers(String fxmlPath) {
        try {
            Stage stage = getStage();
            if (stage == null) return;

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Impossible de charger : " + fxmlPath, e);
            montrerToast("error", "Navigation", "Page non trouvée : " + fxmlPath);
        }
    }

    /* ========================================================= */
    /* CHARGEMENT ET FILTRAGE                                    */
    /* ========================================================= */

    private void chargerDonnees() {
        try {
            allEvenements = Optional.ofNullable(se.getAll()).orElse(List.of());
            allRessources = Optional.ofNullable(sr.getAll()).orElse(List.of());

            remplirFiltreEvenements();
            appliquerFiltres();

            mettreAJourKPI();
            rafraichirGraphiques();
            mettreAJourBadgeNotifications();

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur chargement ressources", e);
            montrerToast("error", "Erreur", "Impossible de charger les ressources.");
        }
    }

    private void remplirFiltreEvenements() {
        if (eventFilterCombo == null) return;

        String selected = eventFilterCombo.getValue();

        List<String> titres = allEvenements.stream()
                .map(Evenn::getTitre)
                .filter(t -> t != null && !t.isBlank())
                .distinct()
                .sorted()
                .collect(Collectors.toCollection(ArrayList::new));

        titres.add(0, "Non affectées");
        titres.add(0, "Tous les événements");

        eventFilterCombo.setItems(FXCollections.observableArrayList(titres));

        if (selected != null && titres.contains(selected)) {
            eventFilterCombo.setValue(selected);
        } else {
            eventFilterCombo.setValue("Tous les événements");
        }
    }

    private void appliquerFiltres() {
        if (allRessources == null) return;

        String query = searchField == null || searchField.getText() == null
                ? ""
                : searchField.getText().trim().toLowerCase(Locale.ROOT);

        String typeFilter = typeFilterCombo == null || typeFilterCombo.getValue() == null
                ? "Tous les types"
                : typeFilterCombo.getValue();

        String eventFilter = eventFilterCombo == null || eventFilterCombo.getValue() == null
                ? "Tous les événements"
                : eventFilterCombo.getValue();

        String statusFilter = statusFilterCombo == null || statusFilterCombo.getValue() == null
                ? "Tous"
                : statusFilterCombo.getValue();

        List<Ressource> filtered = allRessources.stream()
                .filter(r -> matchesSearch(r, query))
                .filter(r -> matchesType(r, typeFilter))
                .filter(r -> matchesEvent(r, eventFilter))
                .filter(r -> matchesStatus(r, statusFilter))
                .sorted(Comparator
                        .comparing((Ressource r) -> getTypeRessource(r).ordinal())
                        .thenComparing(this::scoreOccupationRisque, Comparator.reverseOrder())
                        .thenComparing(r -> safe(r.getNomRessource()).toLowerCase(Locale.ROOT)))
                .collect(Collectors.toList());

        currentRessources = filtered;

        /*
         * LISTE GROUPÉE
         */
        if (ressourceListView != null) {
            ressourceListView.setItems(
                    FXCollections.observableArrayList(construireListeGroupee(filtered))
            );
        }

        afficherRessourcesGrille(filtered);
        mettreAJourCompteurs();
        mettreAJourKPI();

        if (currentView == ViewMode.STATS) {
            rafraichirGraphiques();
        }
    }

    private boolean matchesSearch(Ressource r, String query) {
        if (r == null) return false;
        if (query == null || query.isBlank()) return true;

        return safe(r.getNomRessource()).toLowerCase(Locale.ROOT).contains(query)
                || safe(r.getResponsable()).toLowerCase(Locale.ROOT).contains(query)
                || safe(r.getEvenementTitre()).toLowerCase(Locale.ROOT).contains(query)
                || safe(r.getStatut()).toLowerCase(Locale.ROOT).contains(query)
                || getTypeRessource(r).label().toLowerCase(Locale.ROOT).contains(query);
    }

    private boolean matchesType(Ressource r, String typeFilter) {
        if (typeFilter == null || "Tous les types".equals(typeFilter)) return true;
        return getTypeRessource(r).label().equals(typeFilter);
    }

    private boolean matchesEvent(Ressource r, String eventFilter) {
        if (eventFilter == null || "Tous les événements".equals(eventFilter)) return true;

        if ("Non affectées".equals(eventFilter)) {
            return !estAffectee(r);
        }

        return safe(r.getEvenementTitre()).equals(eventFilter);
    }

    private boolean matchesStatus(Ressource r, String statusFilter) {
        if (statusFilter == null || "Tous".equals(statusFilter)) return true;

        return switch (statusFilter) {
            case "Disponibles" -> quantiteRestante(r) > 0 && !estEnTension(r);
            case "Affectées" -> estAffectee(r);
            case "Non affectées" -> !estAffectee(r);
            case "En tension" -> estEnTension(r);
            default -> safe(r.getStatut()).equals(statusFilter);
        };
    }

    private void mettreAJourCompteurs() {
        int nb = currentRessources == null ? 0 : currentRessources.size();

        if (countLabel != null) {
            countLabel.setText(nb + " ressource" + (nb > 1 ? "s" : ""));
        }

        if (gridCountBadge != null) {
            gridCountBadge.setText(nb + " ressource" + (nb > 1 ? "s" : ""));
        }

        if (listCountBadge != null) {
            long mat = currentRessources.stream()
                    .filter(r -> getTypeRessource(r) == ResourceType.MATERIELLE)
                    .count();

            long log = currentRessources.stream()
                    .filter(r -> getTypeRessource(r) == ResourceType.LOGICIELLE)
                    .count();

            long hum = currentRessources.stream()
                    .filter(r -> getTypeRessource(r) == ResourceType.HUMAINE)
                    .count();

            listCountBadge.setText("Mat. " + mat + " · Log. " + log + " · Hum. " + hum);
        }
    }

    /* ========================================================= */
    /* LISTE GROUPÉE                                             */
    /* ========================================================= */

    private List<RessourceRow> construireListeGroupee(List<Ressource> ressources) {
        List<RessourceRow> rows = new ArrayList<>();

        if (ressources == null || ressources.isEmpty()) {
            return rows;
        }

        Map<ResourceType, List<Ressource>> groupes = new LinkedHashMap<>();
        groupes.put(ResourceType.MATERIELLE, new ArrayList<>());
        groupes.put(ResourceType.LOGICIELLE, new ArrayList<>());
        groupes.put(ResourceType.HUMAINE, new ArrayList<>());
        groupes.put(ResourceType.NON_CLASSEE, new ArrayList<>());

        for (Ressource r : ressources) {
            groupes.get(getTypeRessource(r)).add(r);
        }

        ajouterGroupe(rows, "Ressources matérielles", groupes.get(ResourceType.MATERIELLE));
        ajouterGroupe(rows, "Ressources logicielles", groupes.get(ResourceType.LOGICIELLE));
        ajouterGroupe(rows, "Ressources humaines", groupes.get(ResourceType.HUMAINE));
        ajouterGroupe(rows, "Ressources non classées", groupes.get(ResourceType.NON_CLASSEE));

        return rows;
    }

    private void ajouterGroupe(List<RessourceRow> rows, String titre, List<Ressource> ressources) {
        if (ressources == null || ressources.isEmpty()) return;

        rows.add(RessourceRow.header(titre + " — " + ressources.size()));

        for (Ressource r : ressources) {
            rows.add(RessourceRow.resource(r));
        }
    }

    private void configurerListe() {
        if (ressourceListView == null) return;

        ressourceListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(RessourceRow row, boolean empty) {
                super.updateItem(row, empty);

                if (empty || row == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                if (row.isHeader()) {
                    setText(null);
                    setGraphic(creerHeaderGroupe(row.getHeaderTitle()));
                    return;
                }

                setText(null);
                setGraphic(creerCarteRessourceListe(row.getRessource()));
            }
        });
    }

    private HBox creerHeaderGroupe(String title) {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(14, 4, 8, 4));

        Label icon = new Label("▣");
        icon.setStyle(
                "-fx-background-color: #F8E9EE;" +
                        "-fx-text-fill: #9B0F3E;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: 900;" +
                        "-fx-padding: 6 10;" +
                        "-fx-background-radius: 10;"
        );

        Label label = new Label(title);
        label.setStyle(
                "-fx-text-fill: #4B1026;" +
                        "-fx-font-size: 17px;" +
                        "-fx-font-weight: 900;"
        );

        Region line = new Region();
        line.setStyle("-fx-background-color: #F1DBE4;");
        line.setPrefHeight(1);
        HBox.setHgrow(line, Priority.ALWAYS);

        box.getChildren().addAll(icon, label, line);

        return box;
    }

    private VBox creerCarteRessourceListe(Ressource r) {
        if (r == null) {
            return new VBox();
        }

        ResourceType type = getTypeRessource(r);

        int capacite = quantiteCapacite(r);
        int affectee = quantiteAffectee(r);
        int disponible = quantiteRestante(r);

        double occupation = tauxOccupation(r);
        int pct = (int) Math.round(occupation * 100);

        boolean tension = estEnTension(r);

        String progressColor = tension
                ? RED
                : occupation >= 0.75 ? ORANGE : GREEN;

        VBox card = new VBox(12);
        card.getStyleClass().add("resource-card");
        card.setMaxWidth(Double.MAX_VALUE);

        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(6);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        Label nom = new Label(safe(r.getNomRessource(), "Sans nom"));
        nom.getStyleClass().add("resource-title");
        nom.setWrapText(true);

        HBox chips = new HBox(8);
        chips.setAlignment(Pos.CENTER_LEFT);

        Label typeBadge = creerTypeBadge(type);

        Label eventBadge = new Label(
                estAffectee(r)
                        ? "Événement : " + safe(r.getEvenementTitre())
                        : "Non affectée"
        );
        eventBadge.getStyleClass().add("resource-event-pill");
        eventBadge.setMaxWidth(360);

        chips.getChildren().addAll(typeBadge, eventBadge);

        titleBox.getChildren().addAll(nom, chips);

        Label statut = creerBadgeEtatRessource(r, tension, disponible);

        Button btnModif = new Button("✎ Modifier");
        btnModif.getStyleClass().add("btn-edit");
        btnModif.setMinWidth(112);
        btnModif.setOnAction(event -> {
            event.consume();
            ouvrirFormulaire(r);
        });

        Button btnSupp = new Button("🗑 Supprimer");
        btnSupp.getStyleClass().add("btn-delete");
        btnSupp.setMinWidth(122);
        btnSupp.setOnAction(event -> {
            event.consume();
            supprimerRessource(r);
        });

        HBox actions = new HBox(10, btnModif, btnSupp);
        actions.setAlignment(Pos.CENTER_RIGHT);

        topRow.getChildren().addAll(titleBox, statut, actions);

        GridPane details = new GridPane();
        details.setHgap(18);
        details.setVgap(8);

        details.add(creerMeta("Capacité : ", String.valueOf(capacite)), 0, 0);
        details.add(creerMeta("Affectée : ", String.valueOf(affectee)), 1, 0);
        details.add(creerMeta("Disponible : ", String.valueOf(Math.max(0, disponible))), 2, 0);

        details.add(creerMeta("Responsable : ", safe(r.getResponsable(), "Non assigné")), 0, 1);
        details.add(creerMeta("Statut : ", safe(r.getStatut(), "—")), 1, 1);
        details.add(creerMeta("Coût : ", String.format("%.0f DT", coutTotal(r))), 2, 1);

        HBox bottom = new HBox(12);
        bottom.setAlignment(Pos.CENTER_LEFT);

        ProgressBar progress = new ProgressBar(occupation);
        progress.setPrefWidth(280);
        progress.setMaxWidth(380);
        progress.getStyleClass().add("stock-progress");
        progress.setStyle("-fx-accent: " + progressColor + ";");

        Label pctLabel = new Label(pct + "% occupé");
        pctLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        pctLabel.setTextFill(Color.web(progressColor));

        Label tensionLabel = new Label(
                tension
                        ? "⚠ Sur-occupation : " + Math.abs(disponible)
                        : "Disponible : " + Math.max(0, disponible)
        );
        tensionLabel.getStyleClass().add(tension ? "shortage-pill" : "risk-pill");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        bottom.getChildren().addAll(progress, pctLabel, tensionLabel, spacer);

        card.getChildren().addAll(topRow, details, bottom);

        return card;
    }

    private HBox creerMeta(String label, String value) {
        HBox box = new HBox(4);
        box.setAlignment(Pos.CENTER_LEFT);

        Label l = new Label(label);
        l.getStyleClass().add("meta-label");

        Label v = new Label(value);
        v.getStyleClass().add("meta-strong");

        box.getChildren().addAll(l, v);

        return box;
    }

    private Label creerTypeBadge(ResourceType type) {
        Label badge = new Label(type.label());
        badge.getStyleClass().add("status-pill");

        switch (type) {
            case MATERIELLE -> badge.setStyle(
                    "-fx-background-color:#F8E9EE;" +
                            "-fx-text-fill:#9B0F3E;"
            );

            case LOGICIELLE -> badge.setStyle(
                    "-fx-background-color:#E6F1FB;" +
                            "-fx-text-fill:#064B7A;"
            );

            case HUMAINE -> badge.setStyle(
                    "-fx-background-color:#EAF3DE;" +
                            "-fx-text-fill:#27500A;"
            );

            default -> badge.setStyle(
                    "-fx-background-color:#F2E5EA;" +
                            "-fx-text-fill:#8D5267;"
            );
        }

        return badge;
    }

    private Label creerBadgeEtatRessource(Ressource r, boolean tension, int disponible) {
        Label badge = new Label();
        badge.getStyleClass().add("status-pill");

        if (tension) {
            badge.setText("En tension");
            badge.setStyle(
                    "-fx-background-color:#FFEAF0;" +
                            "-fx-text-fill:#9B0F3E;"
            );
        } else if (!estAffectee(r)) {
            badge.setText("Libre");
            badge.setStyle(
                    "-fx-background-color:#F2E5EA;" +
                            "-fx-text-fill:#8D5267;"
            );
        } else if (disponible > 0) {
            badge.setText("Partiellement occupée");
            badge.setStyle(
                    "-fx-background-color:#FAEEDA;" +
                            "-fx-text-fill:#633806;"
            );
        } else {
            badge.setText("Occupée");
            badge.setStyle(
                    "-fx-background-color:#EAF3DE;" +
                            "-fx-text-fill:#27500A;"
            );
        }

        return badge;
    }

    /* ========================================================= */
    /* GRILLE                                                    */
    /* ========================================================= */

    private void afficherRessourcesGrille(List<Ressource> ressources) {
        if (ressourceGridPane == null) return;

        ressourceGridPane.getChildren().clear();

        if (ressources == null || ressources.isEmpty()) {
            Label empty = new Label("Aucune ressource trouvée");
            empty.getStyleClass().add("section-subtitle");
            ressourceGridPane.getChildren().add(empty);
            return;
        }

        for (Ressource r : ressources) {
            ressourceGridPane.getChildren().add(creerCarteGrille(r));
        }
    }

    private VBox creerCarteGrille(Ressource r) {
        ResourceType type = getTypeRessource(r);

        int capacite = quantiteCapacite(r);
        int affectee = quantiteAffectee(r);
        int restante = quantiteRestante(r);

        double occupation = tauxOccupation(r);
        int pct = (int) Math.round(occupation * 100);

        boolean tension = estEnTension(r);

        String pColor = tension
                ? RED
                : occupation >= 0.75 ? ORANGE : GREEN;

        VBox card = new VBox(10);
        card.getStyleClass().add("resource-grid-card");
        card.setPrefWidth(320);
        card.setMinWidth(320);
        card.setMaxWidth(320);

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label nom = new Label(safe(r.getNomRessource(), "Sans nom"));
        nom.getStyleClass().add("resource-title");
        nom.setWrapText(true);
        HBox.setHgrow(nom, Priority.ALWAYS);

        Label statut = creerBadgeEtatRessource(r, tension, restante);

        header.getChildren().addAll(nom, statut);

        HBox chips = new HBox(8);
        chips.setAlignment(Pos.CENTER_LEFT);
        chips.getChildren().add(creerTypeBadge(type));

        Label evt = new Label(estAffectee(r) ? safe(r.getEvenementTitre(), "-") : "Non affectée");
        evt.getStyleClass().add("resource-event-pill");
        evt.setMaxWidth(Double.MAX_VALUE);

        ProgressBar stock = new ProgressBar(occupation);
        stock.setMaxWidth(Double.MAX_VALUE);
        stock.getStyleClass().add("stock-progress");
        stock.setStyle("-fx-accent: " + pColor + ";");

        HBox stockRow = new HBox(8);
        stockRow.setAlignment(Pos.CENTER_LEFT);

        Label pctLabel = new Label(pct + "% occupé");
        pctLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        pctLabel.setTextFill(Color.web(pColor));

        Label resteLabel = new Label(
                tension
                        ? "Tension : " + Math.abs(restante)
                        : "Libre : " + Math.max(0, restante)
        );
        resteLabel.getStyleClass().add(tension ? "shortage-pill" : "risk-pill");

        stockRow.getChildren().addAll(pctLabel, resteLabel);

        VBox meta = new VBox(5,
                creerMeta("Capacité : ", String.valueOf(capacite)),
                creerMeta("Affectée : ", String.valueOf(affectee)),
                creerMeta("Responsable : ", safe(r.getResponsable(), "Non assigné")),
                creerMeta("Coût : ", String.format("%.0f DT", coutTotal(r)))
        );

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button btnModif = new Button("Modifier");
        btnModif.getStyleClass().add("btn-edit");
        btnModif.setOnAction(event -> ouvrirFormulaire(r));

        Button btnSupp = new Button("Supprimer");
        btnSupp.getStyleClass().add("btn-delete");
        btnSupp.setOnAction(event -> supprimerRessource(r));

        actions.getChildren().addAll(btnModif, btnSupp);

        card.getChildren().addAll(header, chips, evt, stock, stockRow, meta, actions);

        return card;
    }

    /* ========================================================= */
    /* KPI / STATISTIQUES                                        */
    /* ========================================================= */

    private void mettreAJourKPI() {
        List<Ressource> base = currentRessources == null ? List.of() : currentRessources;

        int total = base.size();

        long affectees = base.stream().filter(this::estAffectee).count();

        long disponibles = base.stream()
                .filter(r -> quantiteRestante(r) > 0 && !estEnTension(r))
                .count();

        long tensions = base.stream()
                .filter(this::estEnTension)
                .count();

        int capaciteTotale = base.stream()
                .mapToInt(this::quantiteCapacite)
                .sum();

        int quantiteAffectee = base.stream()
                .mapToInt(this::quantiteAffectee)
                .sum();

        double tauxOccupation = capaciteTotale > 0
                ? Math.min(1.0, (double) quantiteAffectee / capaciteTotale)
                : 0.0;

        long eventsTotal = allEvenements == null ? 0 : allEvenements.stream()
                                                       .filter(e -> e.getTitre() != null && !e.getTitre().isBlank())
                                                       .count();

        long eventsCouverts = base.stream()
                .filter(this::estAffectee)
                .map(Ressource::getEvenementTitre)
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .count();

        double couverture = eventsTotal > 0
                ? (double) eventsCouverts / eventsTotal
                : 0.0;

        if (statTotalLabel != null) {
            statTotalLabel.setText(String.valueOf(total));
        }

        /*
         * Ton FXML appelle encore ce label statCoutLabel.
         * On le réutilise pour afficher les ressources disponibles.
         */
        if (statCoutLabel != null) {
            statCoutLabel.setText(String.valueOf(disponibles));
        }

        /*
         * Ton FXML appelle encore ce label statManquantLabel.
         * On le réutilise pour afficher les ressources affectées.
         */
        if (statManquantLabel != null) {
            statManquantLabel.setText(String.valueOf(affectees));
        }

        if (tauxDispoLabel != null) {
            tauxDispoLabel.setText(Math.round(tauxOccupation * 100) + "%");
        }

        if (irlLabel != null) {
            irlLabel.setText(Math.round(couverture * 100) + "%");
        }

        if (dispoProgress != null) {
            dispoProgress.setProgress(tauxOccupation);
        }

        if (coutMoyenLabel != null) {
            coutMoyenLabel.setText(tensions + " ressource(s) en tension");
        }

        if (evtImpactesLabel != null) {
            evtImpactesLabel.setText(eventsCouverts + " événement(s) couvert(s)");
        }

        if (tendanceTotalLabel != null) {
            tendanceTotalLabel.setText(noteCouverture(couverture, tensions));
        }

        mettreAJourBandeauAlerte(tensions, eventsCouverts);
    }

    private String noteCouverture(double couverture, long tensions) {
        if (tensions > 0 && couverture < 0.5) return "risque élevé";
        if (tensions > 0) return "surveillance nécessaire";
        if (couverture >= 0.8) return "couverture excellente";
        if (couverture >= 0.5) return "couverture correcte";
        return "affectation insuffisante";
    }

    private void mettreAJourBandeauAlerte(long tensions, long eventsCouverts) {
        if (alertBand == null) return;

        if (tensions <= 0) {
            setVisibleManaged(alertBand, false);
            return;
        }

        List<Ressource> critiques = currentRessources.stream()
                .filter(this::estEnTension)
                .sorted(Comparator.comparing(this::scoreOccupationRisque).reversed())
                .limit(3)
                .collect(Collectors.toList());

        String noms = critiques.stream()
                .map(Ressource::getNomRessource)
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(", "));

        setVisibleManaged(alertBand, true);

        if (alertTitleLabel != null) {
            alertTitleLabel.setText(tensions + " ressource(s) en tension");
        }

        if (alertSubLabel != null) {
            alertSubLabel.setText(noms + " — " + eventsCouverts + " événement(s) couvert(s).");
        }
    }

    /* ========================================================= */
    /* GRAPHIQUES                                                */
    /* ========================================================= */

    private void rafraichirGraphiques() {
        dessinerRepartitionParType();
        dessinerOccupationParEvenement();
    }

    private void dessinerRepartitionParType() {
        if (repartitionCanvas == null) return;

        List<Ressource> base = currentRessources == null ? List.of() : currentRessources;

        GraphicsContext gc = repartitionCanvas.getGraphicsContext2D();

        double w = repartitionCanvas.getWidth();
        double h = repartitionCanvas.getHeight();

        gc.clearRect(0, 0, w, h);

        long materiel = base.stream()
                .filter(r -> getTypeRessource(r) == ResourceType.MATERIELLE)
                .count();

        long logiciel = base.stream()
                .filter(r -> getTypeRessource(r) == ResourceType.LOGICIELLE)
                .count();

        long humain = base.stream()
                .filter(r -> getTypeRessource(r) == ResourceType.HUMAINE)
                .count();

        long nonClasse = base.stream()
                .filter(r -> getTypeRessource(r) == ResourceType.NON_CLASSEE)
                .count();

        long total = Math.max(1, base.size());

        double cx = 130;
        double cy = 115;
        double radius = 78;
        double stroke = 24;
        double start = 90;

        gc.setLineWidth(stroke);
        gc.setLineCap(StrokeLineCap.ROUND);

        gc.setStroke(Color.web("#EDD2DC"));
        gc.strokeOval(cx - radius, cy - radius, radius * 2, radius * 2);

        double aMateriel = 360.0 * materiel / total;
        double aLogiciel = 360.0 * logiciel / total;
        double aHumain = 360.0 * humain / total;
        double aNonClasse = 360.0 * nonClasse / total;

        gc.setStroke(Color.web(BORDEAUX));
        gc.strokeArc(cx - radius, cy - radius, radius * 2, radius * 2, start, -aMateriel, ArcType.OPEN);

        gc.setStroke(Color.web(BLUE));
        gc.strokeArc(cx - radius, cy - radius, radius * 2, radius * 2, start - aMateriel, -aLogiciel, ArcType.OPEN);

        gc.setStroke(Color.web(GREEN));
        gc.strokeArc(cx - radius, cy - radius, radius * 2, radius * 2, start - aMateriel - aLogiciel, -aHumain, ArcType.OPEN);

        gc.setStroke(Color.web(ORANGE));
        gc.strokeArc(cx - radius, cy - radius, radius * 2, radius * 2, start - aMateriel - aLogiciel - aHumain, -aNonClasse, ArcType.OPEN);

        gc.setFill(Color.web(TEXT_MUTED));
        gc.setFont(Font.font("Segoe UI", 13));
        gc.fillText("Total", cx - 18, cy - 8);

        gc.setFill(Color.web(BORDEAUX_DARK));
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 30));
        gc.fillText(String.valueOf(base.size()), cx - 12, cy + 28);

        double lx = Math.min(310, w - 220);

        drawLegend(gc, lx, 58, BORDEAUX, "Matérielles — " + materiel);
        drawLegend(gc, lx, 98, BLUE, "Logicielles — " + logiciel);
        drawLegend(gc, lx, 138, GREEN, "Humaines — " + humain);
        drawLegend(gc, lx, 178, ORANGE, "Non classées — " + nonClasse);
    }

    private void dessinerOccupationParEvenement() {
        if (risqueCanvas == null) return;

        List<Ressource> base = currentRessources == null ? List.of() : currentRessources;

        GraphicsContext gc = risqueCanvas.getGraphicsContext2D();

        double w = risqueCanvas.getWidth();
        double h = risqueCanvas.getHeight();

        gc.clearRect(0, 0, w, h);

        Map<String, List<Ressource>> grouped = base.stream()
                .filter(this::estAffectee)
                .collect(Collectors.groupingBy(r -> safe(r.getEvenementTitre(), "Sans événement")));

        List<Map.Entry<String, List<Ressource>>> top = grouped.entrySet().stream()
                .sorted((a, b) -> Double.compare(
                        tauxOccupationEvenement(b.getValue()),
                        tauxOccupationEvenement(a.getValue())
                ))
                .limit(6)
                .collect(Collectors.toList());

        double baseY = h - 42;
        double maxH = h - 90;
        double barW = 54;
        double gap = top.size() <= 1
                ? 38
                : Math.max(28, (w - 80 - (top.size() * barW)) / Math.max(1, top.size() - 1));

        double startX = 34;

        gc.setStroke(Color.web("#F1DBE4"));
        gc.setLineWidth(1);
        gc.strokeLine(20, baseY, w - 20, baseY);

        if (top.isEmpty()) {
            gc.setFill(Color.web(TEXT_MUTED));
            gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
            gc.fillText("Aucune ressource affectée", 40, 110);
            return;
        }

        for (int i = 0; i < top.size(); i++) {
            Map.Entry<String, List<Ressource>> entry = top.get(i);

            double occupation = tauxOccupationEvenement(entry.getValue());
            double bh = maxH * Math.min(1.0, occupation);

            double x = startX + i * (barW + gap);
            double y = baseY - bh;

            String color = occupation >= 1.0
                    ? RED
                    : occupation >= 0.75 ? ORANGE : BORDEAUX;

            gc.setFill(Color.web(color));
            gc.fillRoundRect(x, y, barW, bh, 16, 16);

            gc.setFill(Color.web(TEXT_MUTED));
            gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
            gc.fillText(Math.round(occupation * 100) + "%", x + 8, y - 8);

            String nom = entry.getKey();
            if (nom.length() > 10) {
                nom = nom.substring(0, 10) + "…";
            }

            gc.setFill(Color.web(BORDEAUX_DARK));
            gc.setFont(Font.font("Segoe UI", 11));
            gc.fillText(nom, x - 8, baseY + 20);
        }
    }

    private double tauxOccupationEvenement(List<Ressource> ressources) {
        int cap = ressources.stream()
                .mapToInt(this::quantiteCapacite)
                .sum();

        int aff = ressources.stream()
                .mapToInt(this::quantiteAffectee)
                .sum();

        return cap > 0 ? (double) aff / cap : 0;
    }

    private void drawLegend(GraphicsContext gc, double x, double y, String color, String text) {
        gc.setFill(Color.web(color));
        gc.fillRoundRect(x, y - 12, 18, 18, 8, 8);

        gc.setFill(Color.web(BORDEAUX_DARK));
        gc.setFont(Font.font("Segoe UI", 14));
        gc.fillText(text, x + 30, y + 2);
    }

    /* ========================================================= */
    /* FORMULAIRE                                                */
    /* ========================================================= */

    @FXML
    public void ouvrirFormulaire() {
        ouvrirFormulaire(null);
    }

    private void ouvrirFormulaire(Ressource resEdit) {
        Stage ownerStage = getStage();

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);

        if (ownerStage != null) {
            stage.initOwner(ownerStage);
        }

        stage.setTitle(resEdit == null ? "Nouvelle ressource" : "Modifier la ressource");

        VBox root = createModalRoot();

        HBox header = createModalHeader(
                resEdit == null ? "＋ Nouvelle ressource" : "✎ Modifier la ressource",
                "Classez, affectez et dimensionnez la ressource pour les événements.",
                stage
        );

        ComboBox<String> cbType = new ComboBox<>();
        cbType.setItems(FXCollections.observableArrayList(
                "Matérielle",
                "Logicielle",
                "Humaine",
                "Non classée"
        ));
        cbType.setValue(resEdit == null ? "Matérielle" : getTypeRessource(resEdit).label());
        cbType.setMaxWidth(Double.MAX_VALUE);
        cbType.setStyle(inputStyle());

        TextField tfNom = champTexte("Nom de la ressource");
        tfNom.setText(resEdit != null ? safe(resEdit.getNomRessource()) : "");

        ComboBox<String> cbEvt = new ComboBox<>();

        List<String> events = allEvenements.stream()
                .map(Evenn::getTitre)
                .filter(t -> t != null && !t.isBlank())
                .distinct()
                .sorted()
                .collect(Collectors.toCollection(ArrayList::new));

        events.add(0, "Non affectée");

        cbEvt.setItems(FXCollections.observableArrayList(events));
        cbEvt.setMaxWidth(Double.MAX_VALUE);
        cbEvt.setStyle(inputStyle());

        if (resEdit != null && estAffectee(resEdit)) {
            cbEvt.setValue(resEdit.getEvenementTitre());
        } else {
            cbEvt.setValue("Non affectée");
        }

        TextField tfCapacite = champTexte("Capacité disponible");
        tfCapacite.setText(resEdit != null ? String.valueOf(quantiteCapacite(resEdit)) : "1");

        TextField tfAffectee = champTexte("Quantité affectée");
        tfAffectee.setText(resEdit != null ? String.valueOf(quantiteAffectee(resEdit)) : "0");

        TextField tfCout = champTexte("Coût unitaire");
        tfCout.setText(resEdit != null ? String.valueOf(getCoutUnitaireSafe(resEdit)) : "0");

        TextField tfResp = champTexte("Responsable");
        tfResp.setText(resEdit != null ? safe(resEdit.getResponsable()) : "");

        TextField tfDelai = champTexte("Délai réapprovisionnement en jours");
        tfDelai.setText(resEdit != null ? String.valueOf(getDelaiSafe(resEdit)) : "3");

        ComboBox<String> cbStatut = new ComboBox<>();
        cbStatut.setItems(FXCollections.observableArrayList(
                "Confirmée",
                "En attente",
                "Livrée",
                "Non disponible"
        ));
        cbStatut.setValue(resEdit != null && resEdit.getStatut() != null
                ? resEdit.getStatut()
                : "En attente");
        cbStatut.setMaxWidth(Double.MAX_VALUE);
        cbStatut.setStyle(inputStyle());

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(14);
        grid.setPadding(new Insets(22));

        ColumnConstraints c1 = new ColumnConstraints();
        c1.setMinWidth(180);

        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);

        grid.getColumnConstraints().addAll(c1, c2);

        grid.add(labelForm("Type de ressource *", true), 0, 0);
        grid.add(cbType, 1, 0);

        grid.add(labelForm("Nom *", true), 0, 1);
        grid.add(tfNom, 1, 1);

        grid.add(labelForm("Affectation événement", false), 0, 2);
        grid.add(cbEvt, 1, 2);

        grid.add(labelForm("Capacité disponible *", true), 0, 3);
        grid.add(tfCapacite, 1, 3);

        grid.add(labelForm("Quantité affectée", false), 0, 4);
        grid.add(tfAffectee, 1, 4);

        grid.add(labelForm("Coût unitaire (DT)", false), 0, 5);
        grid.add(tfCout, 1, 5);

        grid.add(labelForm("Responsable", false), 0, 6);
        grid.add(tfResp, 1, 6);

        grid.add(labelForm("Statut", false), 0, 7);
        grid.add(cbStatut, 1, 7);

        grid.add(labelForm("Délai réapprovisionnement", false), 0, 8);
        grid.add(tfDelai, 1, 8);

        Button btnCancel = boutonSecondaire("Annuler");
        btnCancel.setOnAction(e -> stage.close());

        Button btnSave = boutonPrimaire("Enregistrer");
        btnSave.setOnAction(e -> {
            try {
                if (tfNom.getText() == null || tfNom.getText().isBlank()) {
                    montrerToast("warning", "Validation", "Le nom de la ressource est obligatoire.");
                    return;
                }

                int capacite = Integer.parseInt(tfCapacite.getText().trim());
                int affectee = Integer.parseInt(tfAffectee.getText().trim());
                double cout = Double.parseDouble(tfCout.getText().trim().replace(',', '.'));
                int delai = Integer.parseInt(tfDelai.getText().trim());

                if (capacite < 0 || affectee < 0) {
                    montrerToast("warning", "Validation", "Les quantités ne peuvent pas être négatives.");
                    return;
                }

                Ressource res = resEdit != null ? resEdit : new Ressource();

                res.setNomRessource(tfNom.getText().trim());
                res.setQuantiteDisponible(capacite);
                res.setQuantiteRequise(affectee);
                res.setResponsable(tfResp.getText().trim().isBlank()
                        ? "Non assigné"
                        : tfResp.getText().trim());
                res.setStatut(cbStatut.getValue());

                setCoutUnitaireSafe(res, cout);
                setDelaiSafe(res, delai);

                String eventValue = cbEvt.getValue();

                if (eventValue == null || eventValue.equals("Non affectée")) {
                    res.setIdEvenement(0);
                    res.setEvenementTitre("");
                } else {
                    int idEvt = allEvenements.stream()
                            .filter(ev -> safe(ev.getTitre()).equals(eventValue))
                            .findFirst()
                            .map(Evenn::getId_Evenn)
                            .orElse(0);

                    res.setIdEvenement(idEvt);
                    res.setEvenementTitre(eventValue);
                }

                boolean typeSaved = setTypeRessource(res, cbType.getValue());

                if (resEdit == null) {
                    sr.add(res);
                    montrerToast("success", "Ajouté", "« " + res.getNomRessource() + " » ajoutée.");
                } else {
                    sr.update(res);
                    montrerToast("success", "Modifié", "« " + res.getNomRessource() + " » mise à jour.");
                }

                if (!typeSaved) {
                    montrerToast(
                            "warning",
                            "Type non persistant",
                            "Ajoutez typeRessource dans l’entité Ressource pour sauvegarder la catégorie."
                    );
                }

                stage.close();
                chargerDonnees();

            } catch (NumberFormatException ex) {
                montrerToast("error", "Erreur", "Veuillez vérifier les nombres : capacité, affectation, coût ou délai.");
            } catch (SQLException ex) {
                logger.log(Level.SEVERE, "Erreur CRUD", ex);
                montrerToast("error", "Erreur base de données", ex.getMessage());
            }
        });

        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(0, 22, 22, 22));
        footer.getChildren().addAll(btnCancel, btnSave);

        root.getChildren().addAll(header, grid, footer);

        Scene scene = new Scene(root, 690, 660);
        copierStyles(scene);

        stage.setScene(scene);
        stage.showAndWait();
    }

    private TextField champTexte(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setStyle(inputStyle());
        return field;
    }

    private Label labelForm(String text, boolean required) {
        Label l = new Label(text);
        l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        l.setTextFill(Color.web(required ? RED : BORDEAUX_DARK));
        return l;
    }

    private String inputStyle() {
        return "-fx-background-color: #FFF8FA;"
                + "-fx-border-color: #EDD4DC;"
                + "-fx-border-radius: 14;"
                + "-fx-background-radius: 14;"
                + "-fx-padding: 9 12;"
                + "-fx-font-size: 13px;"
                + "-fx-text-fill: #4B1026;";
    }

    /* ========================================================= */
    /* SUPPRESSION                                               */
    /* ========================================================= */

    private void supprimerRessource(Ressource res) {
        if (res == null) return;

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);

        Stage owner = getStage();
        if (owner != null) stage.initOwner(owner);

        stage.setTitle("Suppression ressource");

        VBox root = createModalRoot();

        HBox header = createModalHeader(
                "🗑 Supprimer la ressource",
                "Cette action retirera la ressource de la gestion logistique.",
                stage
        );

        VBox content = new VBox(16);
        content.setPadding(new Insets(24));

        HBox warning = new HBox(14);
        warning.setAlignment(Pos.CENTER_LEFT);
        warning.setPadding(new Insets(16));
        warning.setStyle(
                "-fx-background-color:#FFF1F2;" +
                        "-fx-border-color:#FECDD3;" +
                        "-fx-border-radius:18;" +
                        "-fx-background-radius:18;"
        );

        Label icon = new Label("⚠️");
        icon.setStyle("-fx-font-size:34px;");

        VBox txt = new VBox(6);

        Label title = new Label("Confirmation de suppression");
        title.setStyle(
                "-fx-text-fill:#991B1B;" +
                        "-fx-font-size:16px;" +
                        "-fx-font-weight:900;"
        );

        Label msg = new Label("Voulez-vous vraiment supprimer :\n« "
                + safe(res.getNomRessource(), "cette ressource")
                + " » ?");
        msg.setWrapText(true);
        msg.setStyle("-fx-text-fill:#7F1D1D; -fx-font-size:13px;");

        txt.getChildren().addAll(title, msg);
        warning.getChildren().addAll(icon, txt);

        Label info = new Label("Si cette ressource est affectée à un événement, l’affectation sera également supprimée.");
        info.setWrapText(true);
        info.setStyle("-fx-text-fill:#8D5267; -fx-font-size:12px;");

        content.getChildren().addAll(warning, info);

        Button cancel = boutonSecondaire("Annuler");
        cancel.setOnAction(e -> stage.close());

        Button delete = boutonDanger("Supprimer définitivement");
        delete.setOnAction(e -> {
            try {
                sr.delete(res);
                stage.close();
                montrerToast("success", "Supprimé", "« " + safe(res.getNomRessource()) + " » supprimée.");
                chargerDonnees();
            } catch (SQLException ex) {
                logger.log(Level.SEVERE, "Erreur suppression", ex);
                montrerToast("error", "Erreur", ex.getMessage());
            }
        });

        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(0, 24, 24, 24));
        footer.getChildren().addAll(cancel, delete);

        root.getChildren().addAll(header, content, footer);

        Scene scene = new Scene(root, 560, 360);
        copierStyles(scene);

        stage.setScene(scene);
        stage.showAndWait();
    }

    /* ========================================================= */
    /* NOTIFICATIONS                                             */
    /* ========================================================= */

    @FXML
    public void ouvrirNotifications() {
        rafraichirNotifications();
        setVisibleManaged(notificationOverlay, true);
    }

    @FXML
    public void fermerNotifications() {
        setVisibleManaged(notificationOverlay, false);
    }

    @FXML
    public void voirRessourcesManquantes() {
        ouvrirNotifications();
    }

    @FXML
    public void marquerNotificationsLues() {
        fermerNotifications();
    }

    private void rafraichirNotifications() {
        if (notifListContainer == null) return;

        notifListContainer.getChildren().clear();

        List<Ressource> tensions = allRessources.stream()
                .filter(this::estEnTension)
                .sorted(Comparator.comparing(this::scoreOccupationRisque).reversed())
                .collect(Collectors.toList());

        if (notifTotalBadge != null) {
            notifTotalBadge.setText(String.valueOf(tensions.size()));
        }

        if (notifSubheaderLabel != null) {
            notifSubheaderLabel.setText(tensions.size() + " ressource(s) en tension");
        }

        if (tensions.isEmpty()) {
            Label empty = new Label("Aucune ressource en tension");
            empty.setStyle("-fx-font-size:14px; -fx-text-fill:#27AE60; -fx-padding:20;");
            notifListContainer.getChildren().add(empty);
            return;
        }

        for (Ressource r : tensions) {
            int manque = Math.abs(quantiteRestante(r));

            VBox card = new VBox(6);
            card.setStyle(
                    "-fx-background-color:#FFF3F5;" +
                            "-fx-padding:12;" +
                            "-fx-background-radius:10;" +
                            "-fx-border-color:#EAD8DE;" +
                            "-fx-border-radius:10;"
            );
            card.setMaxWidth(Double.MAX_VALUE);

            Label nL = new Label(r.getNomRessource());
            nL.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#5A1730;");

            Label type = new Label("Type : " + getTypeRessource(r).label());
            type.setStyle("-fx-font-size:12px; -fx-text-fill:#8D5267;");

            Label eL = new Label("Événement : " + safe(r.getEvenementTitre(), "Non affectée"));
            eL.setStyle("-fx-font-size:12px; -fx-text-fill:#A36277;");

            Label meta = new Label("Sur-occupation : -" + manque + " unité(s)");
            meta.setStyle("-fx-font-size:11px; -fx-text-fill:#C1283E; -fx-font-weight:bold;");

            card.getChildren().addAll(nL, type, eL, meta);
            notifListContainer.getChildren().add(card);
        }
    }

    private void mettreAJourBadgeNotifications() {
        long count = allRessources.stream()
                .filter(this::estEnTension)
                .count();

        if (notifCountBadge != null) {
            notifCountBadge.setText(String.valueOf(count));
            setVisibleManaged(notifCountBadge, count > 0);
        }

        if (notifTotalBadge != null) {
            notifTotalBadge.setText(String.valueOf(count));
        }
    }

    /* ========================================================= */
    /* EXPORT PDF                                                */
    /* ========================================================= */

    @FXML
    public void exporterCSV() {
        if (exportBtn == null) return;

        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter les ressources en PDF");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        fc.setInitialFileName("ressources_vita_" + LocalDate.now() + ".pdf");

        File file = fc.showSaveDialog(exportBtn.getScene().getWindow());
        if (file == null) return;

        try {
            ecrirePdfRessources(file, currentRessources == null ? List.of() : currentRessources);
            montrerToast("success", "Export", "Export PDF terminé.");
        } catch (IOException e) {
            montrerToast("error", "Erreur export", e.getMessage());
        }
    }

    private void ecrirePdfRessources(File file, List<Ressource> ressources) throws IOException {
        final int lignesParPage = 38;
        int pageCount = Math.max(1, (int) Math.ceil(ressources.size() / (double) lignesParPage));

        List<byte[]> objects = new ArrayList<>();
        StringBuilder kids = new StringBuilder();

        for (int page = 0; page < pageCount; page++) {
            int pageObjectId = 4 + page * 2;
            int contentObjectId = pageObjectId + 1;

            kids.append(pageObjectId).append(" 0 R ");

            String content = contenuPagePdf(ressources, page, lignesParPage, pageCount);
            byte[] contentBytes = content.getBytes(StandardCharsets.ISO_8859_1);

            objects.add((pageObjectId + " 0 obj\n"
                    + "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] "
                    + "/Resources << /Font << /F1 3 0 R >> >> /Contents " + contentObjectId + " 0 R >>\n"
                    + "endobj\n").getBytes(StandardCharsets.ISO_8859_1));

            ByteArrayOutputStream contentObject = new ByteArrayOutputStream();
            contentObject.write((contentObjectId + " 0 obj\n<< /Length " + contentBytes.length + " >>\nstream\n")
                    .getBytes(StandardCharsets.ISO_8859_1));
            contentObject.write(contentBytes);
            contentObject.write("\nendstream\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));

            objects.add(contentObject.toByteArray());
        }

        List<byte[]> pdfObjects = new ArrayList<>();
        pdfObjects.add("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));
        pdfObjects.add(("2 0 obj\n<< /Type /Pages /Kids [" + kids + "] /Count " + pageCount + " >>\nendobj\n").getBytes(StandardCharsets.ISO_8859_1));
        pdfObjects.add("3 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));
        pdfObjects.addAll(objects);

        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write("%PDF-1.4\n".getBytes(StandardCharsets.ISO_8859_1));

            List<Integer> offsets = new ArrayList<>();

            for (byte[] object : pdfObjects) {
                offsets.add((int) out.getChannel().position());
                out.write(object);
            }

            int xref = (int) out.getChannel().position();
            int objectCount = 3 + pageCount * 2;

            out.write(("xref\n0 " + (objectCount + 1) + "\n").getBytes(StandardCharsets.ISO_8859_1));
            out.write("0000000000 65535 f \n".getBytes(StandardCharsets.ISO_8859_1));

            for (int i = 0; i < objectCount; i++) {
                out.write(String.format("%010d 00000 n \n", offsets.get(i)).getBytes(StandardCharsets.ISO_8859_1));
            }

            out.write(("trailer\n<< /Size " + (objectCount + 1) + " /Root 1 0 R >>\n"
                    + "startxref\n" + xref + "\n%%EOF").getBytes(StandardCharsets.ISO_8859_1));
        }
    }

    private String contenuPagePdf(List<Ressource> ressources, int page, int lignesParPage, int pageCount) {
        StringBuilder sb = new StringBuilder();

        sb.append("BT\n/F1 16 Tf\n40 805 Td\n(")
                .append(pdf("Ressources VITA - Export PDF"))
                .append(") Tj\n/F1 9 Tf\n0 -18 Td\n(")
                .append(pdf("Page " + (page + 1) + "/" + pageCount + " - " + LocalDate.now()))
                .append(") Tj\n0 -22 Td\n");

        int start = page * lignesParPage;
        int end = Math.min(ressources.size(), start + lignesParPage);

        if (ressources.isEmpty()) {
            sb.append("(Aucune ressource a exporter) Tj\n");
        }

        for (int i = start; i < end; i++) {
            Ressource r = ressources.get(i);

            String line = String.format(
                    "%d. [%s] %s | evt: %s | cap: %d | aff: %d | libre: %d | occ: %.0f%%",
                    i + 1,
                    getTypeRessource(r).label(),
                    safe(r.getNomRessource(), "Sans nom"),
                    safe(r.getEvenementTitre(), "Non affectee"),
                    quantiteCapacite(r),
                    quantiteAffectee(r),
                    Math.max(0, quantiteRestante(r)),
                    tauxOccupation(r) * 100
            );

            sb.append("(").append(pdf(line, 118)).append(") Tj\n0 -15 Td\n");
        }

        sb.append("ET\n");

        return sb.toString();
    }

    private String pdf(String s) {
        return pdf(s, 500);
    }

    private String pdf(String s, int maxLen) {
        String normalized = safe(s);

        if (normalized.length() > maxLen) {
            normalized = normalized.substring(0, maxLen - 3) + "...";
        }

        StringBuilder clean = new StringBuilder();

        for (char c : normalized.toCharArray()) {
            clean.append(c > 255 ? '?' : c);
        }

        return clean.toString()
                .replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)");
    }

    /* ========================================================= */
    /* DECONNEXION                                               */
    /* ========================================================= */

    @FXML
    public void deconnecter() {
        Stage stage = getStage();
        if (stage == null) return;

        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.initOwner(stage);
        modal.setTitle("Déconnexion");

        VBox root = createModalRoot();

        HBox header = createModalHeader(
                "Déconnexion",
                "Voulez-vous vraiment fermer la session ?",
                modal
        );

        Button cancel = boutonSecondaire("Annuler");
        cancel.setOnAction(e -> modal.close());

        Button confirm = boutonDanger("Déconnecter");
        confirm.setOnAction(e -> {
            modal.close();
            stage.close();
        });

        HBox footer = new HBox(10, cancel, confirm);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(20));

        root.getChildren().addAll(header, footer);

        Scene scene = new Scene(root, 460, 190);
        copierStyles(scene);

        modal.setScene(scene);
        modal.showAndWait();
    }

    /* ========================================================= */
    /* LOGIQUE METIER                                            */
    /* ========================================================= */

    private boolean estAffectee(Ressource r) {
        return r != null
                && r.getEvenementTitre() != null
                && !r.getEvenementTitre().isBlank()
                && !"-".equals(r.getEvenementTitre());
    }

    /*
     * Ici on utilise :
     * quantiteDisponible = capacité totale de la ressource
     * quantiteRequise = quantité affectée
     */
    private int quantiteCapacite(Ressource r) {
        return r == null ? 0 : Math.max(0, r.getQuantiteDisponible());
    }

    private int quantiteAffectee(Ressource r) {
        return r == null ? 0 : Math.max(0, r.getQuantiteRequise());
    }

    private int quantiteRestante(Ressource r) {
        return quantiteCapacite(r) - quantiteAffectee(r);
    }

    private boolean estEnTension(Ressource r) {
        return r != null && quantiteAffectee(r) > quantiteCapacite(r);
    }

    private double tauxOccupation(Ressource r) {
        int cap = quantiteCapacite(r);
        return cap > 0 ? Math.min(1.0, (double) quantiteAffectee(r) / cap) : 0.0;
    }

    private double scoreOccupationRisque(Ressource r) {
        if (r == null) return 0;

        double occ = tauxOccupation(r);
        double tension = estEnTension(r) ? 1.0 : 0.0;
        double event = estAffectee(r) ? 0.25 : 0.0;
        double delai = Math.min(1.0, getDelaiSafe(r) / 14.0);

        return 100.0 * (0.50 * occ + 0.25 * tension + 0.15 * delai + 0.10 * event);
    }

    private double coutTotal(Ressource r) {
        return r == null ? 0 : quantiteCapacite(r) * getCoutUnitaireSafe(r);
    }

    /* ========================================================= */
    /* TYPE RESSOURCE                                            */
    /* ========================================================= */

    private ResourceType getTypeRessource(Ressource r) {
        if (r == null) return ResourceType.NON_CLASSEE;

        String value = invokeGetterAsString(
                r,
                "getTypeRessource",
                "getType",
                "getCategorie",
                "getCategorieRessource"
        );

        if (value != null && !value.isBlank()) {
            return ResourceType.fromLabel(value);
        }

        return devinerTypeDepuisNom(r);
    }

    private boolean setTypeRessource(Ressource r, String type) {
        if (r == null || type == null) return false;

        return invokeSetter(
                r,
                type,
                "setTypeRessource",
                "setType",
                "setCategorie",
                "setCategorieRessource"
        );
    }

    private ResourceType devinerTypeDepuisNom(Ressource r) {
        String nom = safe(r.getNomRessource()).toLowerCase(Locale.ROOT);

        if (nom.contains("logiciel")
                || nom.contains("licence")
                || nom.contains("application")
                || nom.contains("plateforme")
                || nom.contains("système")
                || nom.contains("systeme")
                || nom.contains("serveur")
                || nom.contains("vpn")
                || nom.contains("zoom")
                || nom.contains("teams")) {
            return ResourceType.LOGICIELLE;
        }

        if (nom.contains("médecin")
                || nom.contains("medecin")
                || nom.contains("infirmier")
                || nom.contains("technicien")
                || nom.contains("agent")
                || nom.contains("personnel")
                || nom.contains("bénévole")
                || nom.contains("benevole")
                || nom.contains("chauffeur")
                || nom.contains("coordinateur")
                || nom.contains("formateur")) {
            return ResourceType.HUMAINE;
        }

        return ResourceType.MATERIELLE;
    }

    private String invokeGetterAsString(Object target, String... methodNames) {
        for (String methodName : methodNames) {
            try {
                Method method = target.getClass().getMethod(methodName);
                Object value = method.invoke(target);

                if (value != null) {
                    return String.valueOf(value);
                }
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    private boolean invokeSetter(Object target, String value, String... methodNames) {
        for (String methodName : methodNames) {
            try {
                Method method = target.getClass().getMethod(methodName, String.class);
                method.invoke(target, value);
                return true;
            } catch (Exception ignored) {
            }
        }

        return false;
    }

    /* ========================================================= */
    /* HELPERS MODAL                                             */
    /* ========================================================= */

    private VBox createModalRoot() {
        VBox root = new VBox();
        root.setStyle(
                "-fx-background-color:white;" +
                        "-fx-background-radius:24;" +
                        "-fx-border-color:#F0D7DF;" +
                        "-fx-border-radius:24;" +
                        "-fx-effect:dropshadow(gaussian, rgba(87,20,43,0.22), 30, 0.18, 0, 8);"
        );
        return root;
    }

    private HBox createModalHeader(String title, String subtitle, Stage stage) {
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 22, 18, 22));
        header.setStyle(
                "-fx-background-color:linear-gradient(to right, #FFF8FA, white);" +
                        "-fx-background-radius:24 24 0 0;" +
                        "-fx-border-color:transparent transparent #F0D7DF transparent;" +
                        "-fx-border-width:0 0 1 0;"
        );

        VBox text = new VBox(4);
        HBox.setHgrow(text, Priority.ALWAYS);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill:#5A1730; -fx-font-size:19px; -fx-font-weight:900;");

        Label subLabel = new Label(subtitle);
        subLabel.setWrapText(true);
        subLabel.setStyle("-fx-text-fill:#8D5267; -fx-font-size:12px;");

        text.getChildren().addAll(titleLabel, subLabel);

        Button close = new Button("✕");
        close.setStyle(
                "-fx-background-color:white;" +
                        "-fx-border-color:#EDD4DC;" +
                        "-fx-border-radius:12;" +
                        "-fx-background-radius:12;" +
                        "-fx-text-fill:#9B0F3E;" +
                        "-fx-font-weight:900;" +
                        "-fx-cursor:hand;"
        );
        close.setOnAction(e -> stage.close());

        header.getChildren().addAll(text, close);

        return header;
    }

    private Button boutonPrimaire(String text) {
        Button btn = new Button(text);
        btn.setStyle(
                "-fx-background-color:#9B0F3E;" +
                        "-fx-text-fill:white;" +
                        "-fx-background-radius:14;" +
                        "-fx-border-color:transparent;" +
                        "-fx-padding:9 16;" +
                        "-fx-font-size:12px;" +
                        "-fx-font-weight:900;" +
                        "-fx-cursor:hand;"
        );
        return btn;
    }

    private Button boutonSecondaire(String text) {
        Button btn = new Button(text);
        btn.setStyle(
                "-fx-background-color:white;" +
                        "-fx-text-fill:#9B0F3E;" +
                        "-fx-border-color:#DBB5C1;" +
                        "-fx-border-radius:14;" +
                        "-fx-background-radius:14;" +
                        "-fx-padding:9 14;" +
                        "-fx-font-size:12px;" +
                        "-fx-font-weight:900;" +
                        "-fx-cursor:hand;"
        );
        return btn;
    }

    private Button boutonDanger(String text) {
        Button btn = new Button(text);
        btn.setStyle(
                "-fx-background-color:#C8204F;" +
                        "-fx-text-fill:white;" +
                        "-fx-background-radius:14;" +
                        "-fx-border-color:transparent;" +
                        "-fx-padding:9 14;" +
                        "-fx-font-size:12px;" +
                        "-fx-font-weight:900;" +
                        "-fx-cursor:hand;"
        );
        return btn;
    }

    private void copierStyles(Scene scene) {
        try {
            Stage owner = getStage();

            if (owner != null && owner.getScene() != null) {
                scene.getStylesheets().addAll(owner.getScene().getStylesheets());
            }
        } catch (Exception ignored) {
        }
    }

    /* ========================================================= */
    /* TOAST                                                     */
    /* ========================================================= */

    private void montrerToast(String type, String titre, String message) {
        if (toastContainer == null) {
            Alert alert = new Alert(switch (type) {
                case "error" -> Alert.AlertType.ERROR;
                case "warning" -> Alert.AlertType.WARNING;
                default -> Alert.AlertType.INFORMATION;
            });

            Stage ownerStage = getStage();
            if (ownerStage != null) alert.initOwner(ownerStage);

            alert.setTitle(titre);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
            return;
        }

        VBox toast = new VBox(3);
        toast.setPadding(new Insets(12, 16, 12, 16));
        toast.setMaxWidth(340);

        String color = switch (type) {
            case "error" -> RED;
            case "warning" -> ORANGE;
            default -> GREEN;
        };

        toast.setStyle(
                "-fx-background-color:white;" +
                        "-fx-background-radius:14;" +
                        "-fx-border-color:" + color + ";" +
                        "-fx-border-radius:14;" +
                        "-fx-effect:dropshadow(gaussian, rgba(0,0,0,0.18), 16, 0.12, 0, 4);"
        );

        Label title = new Label(titre);
        title.setStyle("-fx-text-fill:" + color + "; -fx-font-weight:900; -fx-font-size:13px;");

        Label msg = new Label(message);
        msg.setWrapText(true);
        msg.setStyle("-fx-text-fill:#4B1026; -fx-font-size:12px;");

        toast.getChildren().addAll(title, msg);

        toast.setOpacity(0);
        toast.setTranslateX(40);

        toastContainer.getChildren().add(toast);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(180), toast);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        TranslateTransition slide = new TranslateTransition(Duration.millis(180), toast);
        slide.setFromX(40);
        slide.setToX(0);

        fadeIn.play();
        slide.play();

        PauseTransition wait = new PauseTransition(Duration.seconds(3));
        wait.setOnFinished(e -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(200), toast);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(ev -> toastContainer.getChildren().remove(toast));
            fadeOut.play();
        });
        wait.play();
    }

    /* ========================================================= */
    /* UTILITAIRES                                               */
    /* ========================================================= */

    private Stage getStage() {
        if (ressourceListView != null && ressourceListView.getScene() != null) {
            return (Stage) ressourceListView.getScene().getWindow();
        }

        if (exportBtn != null && exportBtn.getScene() != null) {
            return (Stage) exportBtn.getScene().getWindow();
        }

        if (searchField != null && searchField.getScene() != null) {
            return (Stage) searchField.getScene().getWindow();
        }

        return null;
    }

    private void setVisibleManaged(Node node, boolean value) {
        if (node != null) {
            node.setVisible(value);
            node.setManaged(value);
        }
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private String safe(String s, String fallback) {
        return s != null && !s.isBlank() ? s : fallback;
    }

    private int getDelaiSafe(Ressource r) {
        if (r == null || r.getDelaiReapprovisionnementJours() == null) {
            return 3;
        }

        return r.getDelaiReapprovisionnementJours();
    }

    private void setDelaiSafe(Ressource r, int jours) {
        if (r != null) {
            r.setDelaiReapprovisionnementJours(jours);
        }
    }

    private double getCoutUnitaireSafe(Ressource r) {
        return r == null ? 0 : r.getCoutUnitaire();
    }

    private void setCoutUnitaireSafe(Ressource r, double cout) {
        if (r != null) {
            r.setCoutUnitaire(cout);
        }
    }
}