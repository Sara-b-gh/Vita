

package Controlers;

import Entites.RendezVous;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import services.*;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
        import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ModifierRdvController implements Initializable {

    @FXML private TextField tfPatientId, tfMedecinId, tfMotif, tfLieu, tfHeure;
    @FXML private DatePicker dpDate;
    @FXML private ComboBox<String> cbStatut;
    @FXML private TextArea taNotes;
    @FXML private Label lblError, lblIdRdv;

    private final RendezVousCRUD crud = new RendezVousCRUD();
    private RendezVous rdvActuel;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cbStatut.getItems().addAll("planifie", "confirme", "annule", "termine");
    }

    /** Called by RendezVousController before showing the stage */
    public void setRendezVous(RendezVous rv) {
        this.rdvActuel = rv;
        lblIdRdv.setText("RDV #" + rv.getId_rdv());
        tfPatientId.setText(String.valueOf(rv.getPatient_id()));
        tfMedecinId.setText(String.valueOf(rv.getMedecin_id()));
        if (rv.getDate_rdv() != null) {
            dpDate.setValue(rv.getDate_rdv().toLocalDate());
            tfHeure.setText(rv.getDate_rdv().toLocalTime().format(TIME_FMT));
        }
        tfMotif.setText(rv.getMotif() != null ? rv.getMotif() : "");
        cbStatut.setValue(rv.getStatut());
        tfLieu.setText(rv.getLieu() != null ? rv.getLieu() : "");
        taNotes.setText(rv.getNotes() != null ? rv.getNotes() : "");
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

            rdvActuel.setPatient_id(patientId);
            rdvActuel.setMedecin_id(medecinId);
            rdvActuel.setDate_rdv(dateRdv);
            rdvActuel.setMotif(motif);
            rdvActuel.setStatut(statut);
            rdvActuel.setLieu(lieu);
            rdvActuel.setNotes(notes);

            crud.modifier(rdvActuel);
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

    public static class AdminEvenementController {

        /* ========================= FXML STRUCTURE ========================= */

        @FXML private BorderPane appShell;
        @FXML private StackPane viewsStack;

        @FXML private HBox topSearchBox;
        @FXML private HBox filterBar;

        @FXML private VBox statsView;
        @FXML private VBox gridView;
        @FXML private VBox listView;
        @FXML private VBox archiveSection;

        /* ========================= FXML TOPBAR ========================= */

        @FXML private Label adminNameLabel;

        @FXML private TextField topSearchField;
        @FXML private ComboBox<String> topFilterCombo;
        @FXML private Button topExportBtn;
        @FXML private Button topNotificationBtn;

        @FXML private ToggleButton btnNavStatistiques;
        @FXML private ToggleButton btnNavListe;
        @FXML private ToggleButton btnNavGrille;
        @FXML private ToggleGroup viewToggleGroup;

        /* ========================= FXML EVENTS ========================= */

        @FXML private FlowPane grillePane;
        @FXML private ListView<RendezVous.Evenn> evenementListView;
        @FXML private ListView<RendezVous.Evenn> pastEventsListView;

        /* ========================= FXML STATS ========================= */

        @FXML private Label statTotalLabel;
        @FXML private Label statAVenirLabel;
        @FXML private Label statLieuxLabel;
        @FXML private Label trendAvenirLabel;
        @FXML private Label countLabel;
        @FXML private Label tauxLabel;

        @FXML private Canvas canvasDistribution;
        @FXML private Canvas canvasMonthly;

        @FXML private Label legendUpcomingLabel;
        @FXML private Label legendPastLabel;

        @FXML private ProgressBar distributionProgress;
        @FXML private ProgressBar activityProgress;

        /* ========================= FXML FILTER ========================= */

        @FXML private Label ressourceFiltreLabel;
        @FXML private Button clearFiltreBtn;

        /* ========================= FXML FOOTER ========================= */

        @FXML private Label footerCountLabel;

        /* ========================= FXML NOTIFICATIONS ========================= */

        @FXML private VBox notificationPanel;
        @FXML private ListView<Entites.LocalDateTime.Notification> notificationListView;
        @FXML private Label notificationBadge;
        @FXML private Label notificationCountLabel;
        @FXML private Button markAllReadBtn;

        /* ========================= SERVICES ========================= */

        private final ServiceEvenn se = new ServiceEvenn();
        private final ServiceRessource sr = new ServiceRessource();
        private final ServiceNotification sn = new ServiceNotification();

        private List<RendezVous.Ressource> ressources;
        private final ObservableList<Entites.LocalDateTime.Notification> notifications = FXCollections.observableArrayList();

        /* ========================= STATE ========================= */

        private enum ViewMode {
            STATS,
            LIST,
            GRID
        }

        private ViewMode currentView = ViewMode.GRID;

        private Integer ressourceFiltreId = null;
        private String ressourceFiltreNom = null;

        /* ========================= CONSTANTES ========================= */

        private static final String C_BORDEAUX = "#8B1538";
        private static final String C_BORDEAUX_DARK = "#74112F";
        private static final String C_TEXT = "#5A1730";
        private static final String C_TEXT_MUTED = "#8A6070";

        private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
        private static final DateTimeFormatter FMT_LONG = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);

        /* ========================= INITIALISATION ========================= */

        @FXML
        public void initialize() {
            initialiserFiltres();
            initialiserNavigationVues();
            initialiserRessources();
            initialiserListes();
            initialiserInterface();

            chargerDonnees();
            chargerNotifications();

            if (adminNameLabel != null) {
                adminNameLabel.setText("Dr. A. Valois");
            }
        }

        private void initialiserFiltres() {
            if (topFilterCombo != null) {
                topFilterCombo.setItems(FXCollections.observableArrayList("Tous", "À venir", "Passés"));
                topFilterCombo.setValue("Tous");
                topFilterCombo.valueProperty().addListener((obs, oldValue, newValue) -> chargerDonnees());
            }

            if (topSearchField != null) {
                topSearchField.textProperty().addListener((obs, oldValue, newValue) -> chargerDonnees());
            }
        }

        private void initialiserNavigationVues() {
            if (viewToggleGroup == null) {
                viewToggleGroup = new ToggleGroup();
            }

            if (btnNavStatistiques != null) btnNavStatistiques.setToggleGroup(viewToggleGroup);
            if (btnNavListe != null) btnNavListe.setToggleGroup(viewToggleGroup);
            if (btnNavGrille != null) btnNavGrille.setToggleGroup(viewToggleGroup);

            applyView(ViewMode.GRID);
        }

        private void initialiserRessources() {
            try {
                ressources = sr.getAll();
            } catch (SQLException e) {
                ressources = List.of();
                System.err.println("Erreur chargement ressources : " + e.getMessage());
            }
        }

        private void initialiserListes() {
            if (evenementListView != null) {
                configurerListe();
            }

            if (pastEventsListView != null) {
                configurerListePasses();
            }

            if (notificationListView != null) {
                configurerListeNotifications();
            }
        }

        private void initialiserInterface() {
            setVisibleManaged(ressourceFiltreLabel, false);
            setVisibleManaged(clearFiltreBtn, false);
            setVisibleManaged(notificationPanel, false);
        }

        /* ========================= VIEWS ========================= */

        @FXML
        public void switchToStatsView() {
            applyView(ViewMode.STATS);
        }

        @FXML
        public void switchToListView() {
            applyView(ViewMode.LIST);
        }

        @FXML
        public void switchToGridView() {
            applyView(ViewMode.GRID);
        }

        private void applyView(ViewMode mode) {
            currentView = mode;

            boolean stats = mode == ViewMode.STATS;
            boolean list = mode == ViewMode.LIST;
            boolean grid = mode == ViewMode.GRID;

            setVisibleManaged(statsView, stats);
            setVisibleManaged(listView, list);
            setVisibleManaged(gridView, grid);

            /*
             * Demandé :
             * En vue statistiques, on supprime visuellement recherche + filtre.
             */
            setVisibleManaged(topSearchBox, !stats);
            setVisibleManaged(topFilterCombo, !stats);

            /*
             * Demandé :
             * En vue statistiques, on masque aussi l’archive.
             */
            setVisibleManaged(archiveSection, !stats);

            if (btnNavStatistiques != null) btnNavStatistiques.setSelected(stats);
            if (btnNavListe != null) btnNavListe.setSelected(list);
            if (btnNavGrille != null) btnNavGrille.setSelected(grid);

            if (stats) {
                rafraichirGraphiquesStats();
            } else {
                chargerDonnees();
            }
        }

        /* ========================= DATA ========================= */

        private void chargerDonnees() {
            try {
                List<RendezVous.Evenn> all = se.getAll();

                if (ressourceFiltreId != null) {
                    final int fid = ressourceFiltreId;
                    all = all.stream()
                            .filter(ev -> ev != null && ev.getIdRessource() == fid)
                            .collect(Collectors.toList());
                }

                LocalDateTime now = LocalDateTime.now();

                List<RendezVous.Evenn> filtered = currentView == ViewMode.STATS
                        ? all
                        : filtrerEvenements(all, now);

                remplirGrille(filtered);
                remplirListe(filtered);
                remplirArchive(all, now);

                mettreAJourStatistiquesEvenements(all, filtered);

                if (footerCountLabel != null) {
                    footerCountLabel.setText("Volume total synchronisé : " + all.size() + " événements");
                }

            } catch (SQLException e) {
                montrerAlerte("Erreur", "Impossible de charger les événements : " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }

        private List<RendezVous.Evenn> filtrerEvenements(List<RendezVous.Evenn> source, LocalDateTime now) {
            String query = getSearchQuery();
            String filter = getStatusFilter();

            return source.stream()
                    .filter(ev -> ev != null)
                    .filter(ev -> {
                        if (query.isEmpty()) return true;

                        return contains(ev.getTitre(), query)
                                || contains(ev.getLieu(), query)
                                || contains(ev.getDescription(), query);
                    })
                    .filter(ev -> matchesStatusFilter(ev, filter, now))
                    .sorted((a, b) -> {
                        if (a.getDateEvenement() == null && b.getDateEvenement() == null) return 0;
                        if (a.getDateEvenement() == null) return 1;
                        if (b.getDateEvenement() == null) return -1;
                        return b.getDateEvenement().compareTo(a.getDateEvenement());
                    })
                    .collect(Collectors.toList());
        }

        private boolean matchesStatusFilter(RendezVous.Evenn ev, String filter, LocalDateTime now) {
            if ("Tous".equalsIgnoreCase(filter)) return true;

            boolean future = ev.getDateEvenement() != null && ev.getDateEvenement().isAfter(now);

            if ("À venir".equalsIgnoreCase(filter) || "A venir".equalsIgnoreCase(filter)) {
                return future;
            }

            if ("Passés".equalsIgnoreCase(filter) || "Passes".equalsIgnoreCase(filter)) {
                return !future;
            }

            return true;
        }

        private String getSearchQuery() {
            if (topSearchField == null || topSearchField.getText() == null) return "";
            return topSearchField.getText().trim().toLowerCase();
        }

        private String getStatusFilter() {
            if (topFilterCombo == null || topFilterCombo.getValue() == null) return "Tous";
            return topFilterCombo.getValue();
        }

        private void remplirGrille(List<RendezVous.Evenn> events) {
            if (grillePane == null) return;

            grillePane.getChildren().clear();

            if (events.isEmpty()) {
                grillePane.getChildren().add(creerEmptyState("Aucun événement trouvé."));
                return;
            }

            for (RendezVous.Evenn ev : events) {
                VBox card = creerCarteAdminCompacte(ev);
                if (card != null) {
                    grillePane.getChildren().add(card);
                }
            }
        }

        private void remplirListe(List<RendezVous.Evenn> events) {
            if (evenementListView != null) {
                evenementListView.setItems(FXCollections.observableArrayList(events));
            }
        }

        private void remplirArchive(List<RendezVous.Evenn> all, LocalDateTime now) {
            if (pastEventsListView == null || currentView == ViewMode.STATS) return;

            List<RendezVous.Evenn> passes = all.stream()
                    .filter(e -> e != null)
                    .filter(e -> e.getDateEvenement() != null)
                    .filter(e -> e.getDateEvenement().isBefore(now))
                    .sorted((a, b) -> b.getDateEvenement().compareTo(a.getDateEvenement()))
                    .collect(Collectors.toList());

            pastEventsListView.setItems(FXCollections.observableArrayList(passes));
        }

        private VBox creerEmptyState(String message) {
            VBox box = new VBox(12);
            box.setAlignment(Pos.CENTER);
            box.setPrefWidth(600);
            box.setPrefHeight(180);
            box.setStyle(
                    "-fx-background-color: #FFF8FA;" +
                            "-fx-border-color: #F0D7DF;" +
                            "-fx-border-radius: 20;" +
                            "-fx-background-radius: 20;" +
                            "-fx-padding: 30;"
            );

            Label icon = new Label("🔎");
            icon.setStyle("-fx-font-size: 34px;");

            Label label = new Label(message);
            label.setStyle("-fx-text-fill: #8A6070; -fx-font-size: 14px; -fx-font-weight: bold;");

            box.getChildren().addAll(icon, label);
            return box;
        }

        /* ========================= STATISTIQUES ========================= */

        private void rafraichirGraphiquesStats() {
            try {
                List<RendezVous.Evenn> all = se.getAll();

                if (ressourceFiltreId != null) {
                    final int fid = ressourceFiltreId;
                    all = all.stream()
                            .filter(ev -> ev != null && ev.getIdRessource() == fid)
                            .collect(Collectors.toList());
                }

                /*
                 * Comme recherche et filtre sont masqués en stats,
                 * les statistiques deviennent globales.
                 */
                mettreAJourStatistiquesEvenements(all, all);

                if (footerCountLabel != null) {
                    footerCountLabel.setText("Volume total synchronisé : " + all.size() + " événements");
                }

            } catch (SQLException e) {
                montrerAlerte("Erreur", "Impossible de rafraîchir les statistiques : " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }

        private void mettreAJourStatistiquesEvenements(List<RendezVous.Evenn> baseList, List<RendezVous.Evenn> filteredList) {
            if (baseList == null || filteredList == null) return;

            LocalDateTime now = LocalDateTime.now();

            long total = filteredList.size();

            long aVenir = filteredList.stream()
                    .filter(ev -> ev != null)
                    .filter(ev -> ev.getDateEvenement() != null)
                    .filter(ev -> ev.getDateEvenement().isAfter(now))
                    .count();

            long passes = total - aVenir;

            long lieuxDistincts = filteredList.stream()
                    .filter(ev -> ev != null)
                    .filter(ev -> ev.getLieu() != null)
                    .filter(ev -> !ev.getLieu().isBlank())
                    .map(ev -> ev.getLieu().trim().toLowerCase())
                    .distinct()
                    .count();

            double tauxFuture = total > 0 ? (double) aVenir / total : 0.0;

            setText(statTotalLabel, String.valueOf(total));
            setText(statAVenirLabel, String.valueOf(aVenir));
            setText(statLieuxLabel, String.valueOf(lieuxDistincts));
            setText(tauxLabel, Math.round(tauxFuture * 100) + "%");

            if (trendAvenirLabel != null) {
                trendAvenirLabel.setText(
                        aVenir > 0
                                ? "↑ " + aVenir + " événement(s) programmé(s)"
                                : "Aucun événement futur"
                );
            }

            if (countLabel != null) {
                countLabel.setText(currentView == ViewMode.STATS
                        ? "Statistiques globales"
                        : "Résultat après recherche et filtres");
            }

            if (legendUpcomingLabel != null) {
                legendUpcomingLabel.setText("À venir — " + aVenir);
            }

            if (legendPastLabel != null) {
                legendPastLabel.setText("Passés — " + passes);
            }

            if (distributionProgress != null) {
                distributionProgress.setProgress(tauxFuture);
            }

            drawDonut(canvasDistribution, tauxFuture);
            drawMonthlyHistogram(canvasMonthly, baseList);
        }

        private void drawDonut(Canvas canvas, double ratioFuture) {
            if (canvas == null) return;

            GraphicsContext gc = canvas.getGraphicsContext2D();

            double w = canvas.getWidth();
            double h = canvas.getHeight();

            double cx = w / 2.0;
            double cy = h / 2.0;

            double stroke = 18.0;
            double r = Math.min(cx, cy) - stroke / 2.0 - 4;

            gc.clearRect(0, 0, w, h);

            gc.setStroke(Color.web("#F5DDE3"));
            gc.setLineWidth(stroke);
            gc.setLineCap(StrokeLineCap.ROUND);
            gc.strokeArc(cx - r, cy - r, r * 2, r * 2, 0, 360, ArcType.OPEN);

            if (ratioFuture > 0.01) {
                gc.setStroke(Color.web(C_BORDEAUX));
                gc.setLineWidth(stroke);
                gc.setLineCap(StrokeLineCap.ROUND);
                gc.strokeArc(cx - r, cy - r, r * 2, r * 2, 90, -(ratioFuture * 360), ArcType.OPEN);
            }

            gc.setFill(Color.web(C_TEXT));
            gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));

            String txt = Math.round(ratioFuture * 100) + "%";
            gc.fillText(txt, cx - 20, cy + 6);
        }

        private void drawMonthlyHistogram(Canvas canvas, List<RendezVous.Evenn> allEvents) {
            if (canvas == null || allEvents == null) return;

            GraphicsContext gc = canvas.getGraphicsContext2D();

            double w = canvas.getWidth();
            double h = canvas.getHeight();

            gc.clearRect(0, 0, w, h);

            int[] counts = new int[6];
            String[] labels = new String[6];

            YearMonth current = YearMonth.now();

            int max = 4;

            for (int i = 0; i < 6; i++) {
                YearMonth target = current.minusMonths(5 - i);

                labels[i] = target.getMonth()
                        .getDisplayName(TextStyle.SHORT, Locale.FRENCH)
                        .replace(".", "");

                int ct = 0;

                for (RendezVous.Evenn ev : allEvents) {
                    if (ev != null && ev.getDateEvenement() != null) {
                        YearMonth ym = YearMonth.from(ev.getDateEvenement());

                        if (ym.equals(target)) {
                            ct++;
                        }
                    }
                }

                counts[i] = ct;
                max = Math.max(max, ct);
            }

            double barW = 34;
            double spacing = (w - (barW * 6)) / 7.0;

            gc.setStroke(Color.web("#F0D7DF"));
            gc.setLineWidth(1);
            gc.strokeLine(0, h - 24, w, h - 24);

            for (int i = 0; i < 6; i++) {
                double x = spacing + i * (barW + spacing);
                double barH = ((double) counts[i] / max) * (h - 52);
                double y = h - 24 - barH;

                gc.setFill(Color.web(i == 5 ? C_BORDEAUX : "#F5DDE3"));
                gc.fillRoundRect(x, y, barW, barH, 10, 10);

                gc.setFill(Color.web(C_TEXT_MUTED));
                gc.setFont(Font.font("Segoe UI", 10));
                gc.fillText(labels[i], x + 4, h - 6);

                if (counts[i] > 0) {
                    gc.setFill(Color.web(C_TEXT));
                    gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
                    gc.fillText(String.valueOf(counts[i]), x + 12, y - 6);
                }
            }

            if (activityProgress != null) {
                activityProgress.setProgress(counts[5] > 0 ? 1.0 : 0.25);
            }
        }

        /* ========================= CARTES ========================= */

        private VBox creerCarteAdminCompacte(RendezVous.Evenn ev) {
            if (ev == null) return null;

            boolean future = ev.getDateEvenement() != null
                    && ev.getDateEvenement().isAfter(LocalDateTime.now());

            VBox card = new VBox(14);
            card.setPadding(new Insets(20));
            card.setPrefWidth(335);
            card.setMinWidth(315);
            card.setMaxWidth(360);
            card.getStyleClass().add("event-card");

            HBox top = new HBox(10);
            top.setAlignment(Pos.CENTER_LEFT);

            Label status = new Label(future ? "À venir" : "Passé");
            status.getStyleClass().add(future ? "event-badge-upcoming" : "event-badge-past");

            Label resource = new Label(shorten(getRessourceNom(ev.getIdRessource()), 20));
            resource.getStyleClass().add("event-resource-pill");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label icon = new Label("📅");
            icon.getStyleClass().add("event-icon-box");

            top.getChildren().addAll(status, resource, spacer, icon);

            Label title = new Label(safe(ev.getTitre(), "Sans titre"));
            title.setWrapText(true);
            title.getStyleClass().add("event-title");

            VBox details = new VBox(8);
            details.getChildren().addAll(
                    creerLigneCarte("📅", "Date", formatDateTime(ev.getDateEvenement())),
                    creerLigneCarte("📍", "Lieu", safe(ev.getLieu(), "Lieu non défini")),
                    creerLigneCarte("📦", "Ressource", getRessourceNom(ev.getIdRessource()))
            );

            Label desc = new Label(shorten(safe(ev.getDescription(), "Aucune description fournie."), 120));
            desc.setWrapText(true);
            desc.getStyleClass().add("event-description");

            HBox actions = new HBox(8);
            actions.setAlignment(Pos.CENTER_LEFT);

            Button detailsBtn = boutonPrimaire("Détails");
            detailsBtn.setOnAction(e -> ouvrirModalDetails(ev));

            Button reservationsBtn = boutonSecondaire("Réservations");
            reservationsBtn.setOnAction(e -> ouvrirGestionReservations(ev));

            Region actionSpacer = new Region();
            HBox.setHgrow(actionSpacer, Priority.ALWAYS);

            MenuButton more = new MenuButton("Plus");
            more.getStyleClass().add("btn-soft-menu");

            MenuItem itemMap = new MenuItem("Carte");
            itemMap.setOnAction(e -> afficherCarte(ev));

            MenuItem itemQr = new MenuItem("QR Event");
            itemQr.setOnAction(e -> genererQREvent(ev));

            MenuItem itemShare = new MenuItem("Partager");
            itemShare.setOnAction(e -> partagerEvenement(ev));

            MenuItem itemEdit = new MenuItem("Modifier");
            itemEdit.setOnAction(e -> ouvrirFormulaire(ev));

            MenuItem itemDelete = new MenuItem("Supprimer");
            itemDelete.setOnAction(e -> supprimerAvecConfirmation(ev));

            more.getItems().addAll(itemMap, itemQr, itemShare, new SeparatorMenuItem(), itemEdit, itemDelete);

            actions.getChildren().addAll(detailsBtn, reservationsBtn, actionSpacer, more);

            card.getChildren().addAll(top, title, details, desc, actions);

            return card;
        }

        private HBox creerLigneCarte(String iconText, String labelText, String valueText) {
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);

            Label icon = new Label(iconText);
            icon.getStyleClass().add("detail-icon");

            Label label = new Label(labelText + " :");
            label.getStyleClass().add("detail-label");

            Label value = new Label(valueText);
            value.setWrapText(true);
            value.getStyleClass().add("detail-value");

            HBox.setHgrow(value, Priority.ALWAYS);
            row.getChildren().addAll(icon, label, value);

            return row;
        }

        /* ========================= LISTES ========================= */

        private void configurerListe() {
            evenementListView.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(RendezVous.Evenn ev, boolean empty) {
                    super.updateItem(ev, empty);

                    if (empty || ev == null) {
                        setText(null);
                        setGraphic(null);
                        return;
                    }

                    HBox row = new HBox(16);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setPadding(new Insets(16));
                    row.setStyle(
                            "-fx-background-color: white;" +
                                    "-fx-border-color: #F0D7DF;" +
                                    "-fx-border-radius: 18;" +
                                    "-fx-background-radius: 18;"
                    );

                    VBox main = new VBox(6);
                    HBox.setHgrow(main, Priority.ALWAYS);

                    Label title = new Label(safe(ev.getTitre(), "Sans titre"));
                    title.setStyle("-fx-text-fill: #5A1730; -fx-font-size: 15px; -fx-font-weight: 900;");

                    Label meta = new Label("📅 " + formatDateTime(ev.getDateEvenement()) + "    📍 " + safe(ev.getLieu(), "Lieu non défini"));
                    meta.setStyle("-fx-text-fill: #8A6070; -fx-font-size: 12px;");

                    main.getChildren().addAll(title, meta);

                    Button details = boutonSecondaire("Détails");
                    details.setOnAction(e -> ouvrirModalDetails(ev));

                    Button edit = boutonPrimaire("Modifier");
                    edit.setOnAction(e -> ouvrirFormulaire(ev));

                    Button delete = boutonDanger("Supprimer");
                    delete.setOnAction(e -> supprimerAvecConfirmation(ev));

                    row.getChildren().addAll(main, details, edit, delete);

                    setText(null);
                    setGraphic(row);
                }
            });
        }

        private void configurerListePasses() {
            pastEventsListView.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(RendezVous.Evenn ev, boolean empty) {
                    super.updateItem(ev, empty);

                    if (empty || ev == null) {
                        setText(null);
                        setGraphic(null);
                        return;
                    }

                    HBox row = new HBox(12);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setPadding(new Insets(12, 14, 12, 14));
                    row.setStyle(
                            "-fx-background-color: #FFF8FA;" +
                                    "-fx-border-color: #F0D7DF;" +
                                    "-fx-border-radius: 16;" +
                                    "-fx-background-radius: 16;"
                    );

                    Label date = new Label(ev.getDateEvenement() != null ? ev.getDateEvenement().format(FMT) : "?");
                    date.setStyle(
                            "-fx-background-color: #F8E9EE;" +
                                    "-fx-text-fill: #8B1538;" +
                                    "-fx-padding: 5 10;" +
                                    "-fx-background-radius: 999;" +
                                    "-fx-font-size: 11px;" +
                                    "-fx-font-weight: bold;"
                    );

                    Label title = new Label(safe(ev.getTitre(), "Sans titre"));
                    title.setStyle("-fx-text-fill: #5A1730; -fx-font-size: 13px; -fx-font-weight: 900;");

                    Label lieu = new Label("📍 " + safe(ev.getLieu(), "Lieu non défini"));
                    lieu.setStyle("-fx-text-fill: #8A6070; -fx-font-size: 11px;");

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    Button consulter = boutonSecondaire("Consulter");
                    consulter.setOnAction(e -> ouvrirModalDetails(ev));

                    row.getChildren().addAll(date, title, lieu, spacer, consulter);

                    setText(null);
                    setGraphic(row);
                }
            });
        }

        /* ========================= FORMULAIRE MODERNE ========================= */

        @FXML
        public void ouvrirFormulaire() {
            ouvrirFormulaire(null);
        }

        private void ouvrirFormulaire(RendezVous.Evenn evEdit) {
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);

            Stage owner = obtenirStage();
            if (owner != null) {
                stage.initOwner(owner);
            }

            stage.setTitle(evEdit == null ? "Nouvel événement" : "Modifier l'événement");

            VBox root = createWindowRoot();

            HBox header = createWindowHeader(
                    evEdit == null ? "＋ Nouvel événement" : "✏ Modifier l’événement",
                    "Renseignez les informations nécessaires à la planification clinique.",
                    stage
            );

            TextField tfTitre = champTexte("Titre de l’événement");
            TextField tfLieu = champTexte("Emplacement ou site");

            TextArea taDesc = new TextArea();
            taDesc.setPromptText("Description, consignes, informations utiles...");
            taDesc.setPrefRowCount(4);
            taDesc.setWrapText(true);
            taDesc.setStyle(inputStyle());

            DatePicker dpDate = new DatePicker(LocalDate.now());
            dpDate.setStyle(inputStyle());

            TextField tfHeure = champTexte("HH:mm");
            tfHeure.setText("09:00");
            tfHeure.setPrefWidth(110);

            ComboBox<RendezVous.Ressource> cbRessource = new ComboBox<>();
            cbRessource.setPromptText("Choisir une ressource");
            cbRessource.setStyle(inputStyle());

            if (ressources != null) {
                cbRessource.setItems(FXCollections.observableArrayList(ressources));
            }

            cbRessource.getItems().add(0, null);

            cbRessource.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(RendezVous.Ressource item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "-- Aucune ressource --" : item.getNomRessource());
                }
            });

            cbRessource.setButtonCell(new ListCell<>() {
                @Override
                protected void updateItem(RendezVous.Ressource item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "-- Aucune ressource --" : item.getNomRessource());
                }
            });

            if (evEdit != null) {
                tfTitre.setText(safe(evEdit.getTitre()));
                tfLieu.setText(safe(evEdit.getLieu()));
                taDesc.setText(safe(evEdit.getDescription()));

                if (evEdit.getDateEvenement() != null) {
                    dpDate.setValue(evEdit.getDateEvenement().toLocalDate());
                    tfHeure.setText(evEdit.getDateEvenement().format(TIME_FMT));
                }

                if (ressources != null) {
                    ressources.stream()
                            .filter(r -> r != null && r.getIdRessource() == evEdit.getIdRessource())
                            .findFirst()
                            .ifPresentOrElse(cbRessource::setValue, () -> cbRessource.setValue(null));
                }
            }

            GridPane form = new GridPane();
            form.setHgap(14);
            form.setVgap(14);
            form.setPadding(new Insets(22));

            ColumnConstraints c1 = new ColumnConstraints();
            c1.setMinWidth(120);

            ColumnConstraints c2 = new ColumnConstraints();
            c2.setHgrow(Priority.ALWAYS);

            ColumnConstraints c3 = new ColumnConstraints();
            c3.setMinWidth(80);

            ColumnConstraints c4 = new ColumnConstraints();
            c4.setMinWidth(120);

            form.getColumnConstraints().addAll(c1, c2, c3, c4);

            form.add(labelForm("Titre *"), 0, 0);
            form.add(tfTitre, 1, 0, 3, 1);

            form.add(labelForm("Ressource"), 0, 1);
            form.add(cbRessource, 1, 1, 3, 1);

            form.add(labelForm("Date *"), 0, 2);
            form.add(dpDate, 1, 2);

            form.add(labelForm("Heure"), 2, 2);
            form.add(tfHeure, 3, 2);

            form.add(labelForm("Lieu *"), 0, 3);
            form.add(tfLieu, 1, 3, 3, 1);

            form.add(labelForm("Description"), 0, 4);
            form.add(taDesc, 1, 4, 3, 1);

            Button btnCancel = boutonSecondaire("Annuler");
            btnCancel.setOnAction(e -> stage.close());

            Button btnSave = boutonPrimaire("Enregistrer");

            btnSave.setOnAction(e -> {
                if (tfTitre.getText().isBlank() || dpDate.getValue() == null || tfLieu.getText().isBlank()) {
                    montrerAlerte("Champs obligatoires", "Veuillez renseigner le titre, la date et le lieu.", Alert.AlertType.WARNING);
                    return;
                }

                LocalTime heure = parseTime(tfHeure.getText());
                LocalDateTime dateTime = LocalDateTime.of(dpDate.getValue(), heure);

                int idRes = cbRessource.getValue() != null
                        ? cbRessource.getValue().getIdRessource()
                        : 0;

                RendezVous.Evenn event;

                if (evEdit != null) {
                    evEdit.setTitre(tfTitre.getText().trim());
                    evEdit.setIdRessource(idRes);
                    evEdit.setDateEvenement(dateTime);
                    evEdit.setDescription(taDesc.getText().trim());
                    evEdit.setLieu(tfLieu.getText().trim());
                    event = evEdit;
                } else {
                    event = new RendezVous.Evenn(
                            idRes,
                            tfTitre.getText().trim(),
                            dateTime,
                            taDesc.getText().trim(),
                            tfLieu.getText().trim()
                    );
                }

                try {
                    if (evEdit == null) {
                        se.add(event);
                        montrerAlerte("Succès", "Événement créé avec succès.", Alert.AlertType.INFORMATION);
                    } else {
                        se.update(event);
                        montrerAlerte("Succès", "Événement mis à jour avec succès.", Alert.AlertType.INFORMATION);
                    }

                    stage.close();
                    chargerDonnees();

                } catch (SQLException ex) {
                    montrerAlerte("Erreur", "Erreur base de données : " + ex.getMessage(), Alert.AlertType.ERROR);
                }
            });

            HBox footer = new HBox(10);
            footer.setAlignment(Pos.CENTER_RIGHT);
            footer.setPadding(new Insets(0, 22, 22, 22));
            footer.getChildren().addAll(btnCancel, btnSave);

            root.getChildren().addAll(header, form, footer);

            Scene scene = new Scene(root, 720, 520);
            copierStyles(scene);

            stage.setScene(scene);
            stage.showAndWait();
        }

        private LocalTime parseTime(String value) {
            try {
                if (value != null && value.contains(":")) {
                    String[] parts = value.trim().split(":");
                    return LocalTime.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
                }
            } catch (Exception ignored) {
            }

            return LocalTime.of(9, 0);
        }

        private TextField champTexte(String prompt) {
            TextField field = new TextField();
            field.setPromptText(prompt);
            field.setStyle(inputStyle());
            return field;
        }

        private Label labelForm(String text) {
            Label label = new Label(text);
            label.setStyle("-fx-text-fill: #5A1730; -fx-font-size: 12px; -fx-font-weight: 900;");
            return label;
        }

        private String inputStyle() {
            return "-fx-background-color: #FFF8FA;" +
                    "-fx-border-color: #EDD4DC;" +
                    "-fx-border-radius: 14;" +
                    "-fx-background-radius: 14;" +
                    "-fx-padding: 10 12;" +
                    "-fx-font-size: 13px;" +
                    "-fx-text-fill: #5A1730;" +
                    "-fx-prompt-text-fill: #B78A98;";
        }

        /* ========================= DÉTAILS ========================= */

        private void ouvrirModalDetails(RendezVous.Evenn ev) {
            if (ev == null) return;

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);

            Stage owner = obtenirStage();
            if (owner != null) stage.initOwner(owner);

            stage.setTitle("Détails - " + safe(ev.getTitre()));

            VBox root = createWindowRoot();

            HBox header = createWindowHeader(
                    "📅 Détails de l’événement",
                    "Informations complètes de l’événement sélectionné.",
                    stage
            );

            VBox content = new VBox(16);
            content.setPadding(new Insets(22));

            boolean future = ev.getDateEvenement() != null && ev.getDateEvenement().isAfter(LocalDateTime.now());

            Label badge = new Label(future ? "Événement à venir" : "Événement passé");
            badge.setStyle(future
                    ? "-fx-background-color: #DCFCE7; -fx-text-fill: #166534; -fx-padding: 6 14; -fx-background-radius: 999; -fx-font-weight: 900;"
                    : "-fx-background-color: #F3EEF0; -fx-text-fill: #856671; -fx-padding: 6 14; -fx-background-radius: 999; -fx-font-weight: 900;"
            );

            Label title = new Label(safe(ev.getTitre(), "Sans titre"));
            title.setWrapText(true);
            title.setStyle("-fx-text-fill: #5A1730; -fx-font-size: 24px; -fx-font-weight: 900;");

            VBox dateBox = infoBox("📅", "Date et heure", formatDateTime(ev.getDateEvenement()));
            VBox lieuBox = infoBox("📍", "Lieu", safe(ev.getLieu(), "Non défini"));
            VBox ressourceBox = infoBox("📦", "Ressource", getRessourceNom(ev.getIdRessource()));

            VBox descriptionBox = new VBox(8);
            Label descTitle = new Label("DESCRIPTION");
            descTitle.setStyle("-fx-text-fill: #A36277; -fx-font-size: 11px; -fx-font-weight: 900;");

            Label desc = new Label(safe(ev.getDescription(), "Aucune description disponible."));
            desc.setWrapText(true);
            desc.setStyle(
                    "-fx-background-color: #FFF8FA;" +
                            "-fx-background-radius: 16;" +
                            "-fx-border-color: #F0D7DF;" +
                            "-fx-border-radius: 16;" +
                            "-fx-padding: 14;" +
                            "-fx-text-fill: #5C4350;" +
                            "-fx-font-size: 13px;"
            );

            descriptionBox.getChildren().addAll(descTitle, desc);
            content.getChildren().addAll(badge, title, dateBox, lieuBox, ressourceBox, descriptionBox);

            Button btnTxt = boutonSecondaire("Exporter fiche .txt");
            btnTxt.setOnAction(e -> exporterFicheTxt(ev));

            Button btnCopy = boutonPrimaire("Copier infos");
            btnCopy.setOnAction(e -> partagerEvenement(ev));

            Button btnClose = boutonSecondaire("Fermer");
            btnClose.setOnAction(e -> stage.close());

            HBox footer = new HBox(10);
            footer.setAlignment(Pos.CENTER_RIGHT);
            footer.setPadding(new Insets(0, 22, 22, 22));
            footer.getChildren().addAll(btnTxt, btnCopy, btnClose);

            root.getChildren().addAll(header, content, footer);

            Scene scene = new Scene(root, 620, 650);
            copierStyles(scene);

            stage.setScene(scene);
            stage.showAndWait();
        }

        private VBox infoBox(String icon, String title, String value) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);

            Label iconLabel = new Label(icon);
            iconLabel.setStyle(
                    "-fx-background-color: #F8E9EE;" +
                            "-fx-background-radius: 14;" +
                            "-fx-padding: 10;" +
                            "-fx-font-size: 18px;"
            );

            VBox textBox = new VBox(3);

            Label titleLabel = new Label(title);
            titleLabel.setStyle("-fx-text-fill: #5A1730; -fx-font-size: 12px; -fx-font-weight: 900;");

            Label valueLabel = new Label(value);
            valueLabel.setWrapText(true);
            valueLabel.setStyle("-fx-text-fill: #8A6070; -fx-font-size: 13px;");

            textBox.getChildren().addAll(titleLabel, valueLabel);
            row.getChildren().addAll(iconLabel, textBox);

            VBox box = new VBox(row);
            box.setStyle(
                    "-fx-background-color: #FFF8FA;" +
                            "-fx-border-color: #F0D7DF;" +
                            "-fx-border-radius: 16;" +
                            "-fx-background-radius: 16;" +
                            "-fx-padding: 14;"
            );

            return box;
        }

        /* ========================= SUPPRESSION MODERNE ========================= */

        private void supprimerAvecConfirmation(RendezVous.Evenn ev) {
            if (ev == null) return;

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);

            Stage owner = obtenirStage();
            if (owner != null) {
                stage.initOwner(owner);
            }

            stage.setTitle("Suppression événement");

            VBox root = createWindowRoot();

            HBox header = createWindowHeader(
                    "🗑 Supprimer l’événement",
                    "Cette action est définitive. Veuillez confirmer votre choix.",
                    stage
            );

            VBox content = new VBox(18);
            content.setPadding(new Insets(24));
            content.setAlignment(Pos.CENTER_LEFT);

            HBox warningBox = new HBox(14);
            warningBox.setAlignment(Pos.CENTER_LEFT);
            warningBox.setPadding(new Insets(16));
            warningBox.setStyle(
                    "-fx-background-color: #FFF1F2;" +
                            "-fx-border-color: #FECDD3;" +
                            "-fx-border-radius: 18;" +
                            "-fx-background-radius: 18;"
            );

            Label warningIcon = new Label("⚠️");
            warningIcon.setStyle("-fx-font-size: 34px;");

            VBox warningText = new VBox(6);

            Label warningTitle = new Label("Confirmation de suppression");
            warningTitle.setStyle(
                    "-fx-text-fill: #991B1B;" +
                            "-fx-font-size: 16px;" +
                            "-fx-font-weight: 900;"
            );

            Label warningMessage = new Label(
                    "Voulez-vous vraiment supprimer l’événement :\n« " +
                            safe(ev.getTitre(), "cet événement") +
                            " » ?"
            );
            warningMessage.setWrapText(true);
            warningMessage.setStyle(
                    "-fx-text-fill: #7F1D1D;" +
                            "-fx-font-size: 13px;" +
                            "-fx-line-spacing: 3;"
            );

            warningText.getChildren().addAll(warningTitle, warningMessage);
            warningBox.getChildren().addAll(warningIcon, warningText);

            Label info = new Label(
                    "Les informations liées à cet événement ne seront plus visibles dans la grille, la liste et les statistiques."
            );
            info.setWrapText(true);
            info.setStyle(
                    "-fx-text-fill: #8A6070;" +
                            "-fx-font-size: 12px;"
            );

            content.getChildren().addAll(warningBox, info);

            Button btnCancel = boutonSecondaire("Annuler");
            btnCancel.setOnAction(e -> stage.close());

            Button btnDelete = boutonDanger("Supprimer définitivement");
            btnDelete.setOnAction(e -> {
                try {
                    se.delete(ev);

                    stage.close();

                    montrerAlerte(
                            "Suppression réussie",
                            "L’événement a été supprimé avec succès.",
                            Alert.AlertType.INFORMATION
                    );

                    chargerDonnees();

                } catch (SQLException ex) {
                    montrerAlerte(
                            "Erreur",
                            "Erreur de suppression : " + ex.getMessage(),
                            Alert.AlertType.ERROR
                    );
                }
            });

            HBox footer = new HBox(10);
            footer.setAlignment(Pos.CENTER_RIGHT);
            footer.setPadding(new Insets(0, 24, 24, 24));
            footer.getChildren().addAll(btnCancel, btnDelete);

            root.getChildren().addAll(header, content, footer);

            Scene scene = new Scene(root, 560, 360);
            copierStyles(scene);

            stage.setScene(scene);
            stage.showAndWait();
        }

        /* ========================= RESERVATIONS ========================= */

        private void ouvrirGestionReservations(RendezVous.Evenn ev) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/ReservationPanel.fxml"));
                AnchorPane root = loader.load();

                NavigationManager.ReservationPanelController controller = loader.getController();
                controller.setEvenement(ev, this::chargerDonnees);

                Stage stage = new Stage();
                stage.initModality(Modality.APPLICATION_MODAL);

                Stage owner = obtenirStage();
                if (owner != null) stage.initOwner(owner);

                stage.setTitle("Réservations - " + safe(ev.getTitre()));
                Scene scene = new Scene(root);
                copierStyles(scene);
                stage.setScene(scene);
                stage.show();

            } catch (IOException e) {
                montrerAlerte("Erreur", "Impossible d'ouvrir le panneau des réservations : " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }

        /* ========================= MAP / QR / SHARE ========================= */

        private void afficherCarte(RendezVous.Evenn ev) {
            if (ev == null || ev.getLieu() == null || ev.getLieu().isBlank()) {
                montrerAlerte("Localisation", "Cet événement n'a pas d'adresse spécifiée.", Alert.AlertType.WARNING);
                return;
            }

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);

            Stage owner = obtenirStage();
            if (owner != null) stage.initOwner(owner);

            stage.setTitle("Localisation - " + safe(ev.getTitre()));

            VBox root = createWindowRoot();

            HBox header = createWindowHeader(
                    "📍 Localisation",
                    "Adresse de l’événement et accès rapide à Google Maps.",
                    stage
            );

            VBox content = new VBox(16);
            content.setPadding(new Insets(22));

            Label title = new Label(safe(ev.getTitre(), "Événement"));
            title.setWrapText(true);
            title.setStyle("-fx-text-fill: #5A1730; -fx-font-size: 20px; -fx-font-weight: 900;");

            Label address = new Label(ev.getLieu());
            address.setWrapText(true);
            address.setStyle(
                    "-fx-background-color: #FFF8FA;" +
                            "-fx-border-color: #F0D7DF;" +
                            "-fx-border-radius: 16;" +
                            "-fx-background-radius: 16;" +
                            "-fx-padding: 16;" +
                            "-fx-text-fill: #5C4350;" +
                            "-fx-font-size: 13px;"
            );

            content.getChildren().addAll(title, address);

            Button copy = boutonSecondaire("Copier adresse");
            copy.setOnAction(e -> {
                ClipboardContent cc = new ClipboardContent();
                cc.putString(ev.getLieu());
                Clipboard.getSystemClipboard().setContent(cc);
                montrerAlerte("Succès", "Adresse copiée.", Alert.AlertType.INFORMATION);
            });

            Button maps = boutonPrimaire("Ouvrir Google Maps");
            maps.setOnAction(e -> {
                try {
                    String encoded = java.net.URLEncoder.encode(ev.getLieu(), StandardCharsets.UTF_8);
                    java.awt.Desktop.getDesktop().browse(
                            new java.net.URI("https://www.google.com/maps/search/?api=1&query=" + encoded)
                    );
                } catch (Exception ex) {
                    montrerAlerte("Erreur", "Impossible d'ouvrir Google Maps : " + ex.getMessage(), Alert.AlertType.ERROR);
                }
            });

            Button close = boutonSecondaire("Fermer");
            close.setOnAction(e -> stage.close());

            HBox footer = new HBox(10);
            footer.setAlignment(Pos.CENTER_RIGHT);
            footer.setPadding(new Insets(0, 22, 22, 22));
            footer.getChildren().addAll(copy, maps, close);

            root.getChildren().addAll(header, content, footer);

            Scene scene = new Scene(root, 560, 360);
            copierStyles(scene);

            stage.setScene(scene);
            stage.showAndWait();
        }

        private void genererQREvent(RendezVous.Evenn ev) {
            if (ev == null) return;

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);

            Stage owner = obtenirStage();
            if (owner != null) stage.initOwner(owner);

            stage.setTitle("QR Code - " + safe(ev.getTitre()));

            VBox root = createWindowRoot();

            HBox header = createWindowHeader(
                    "📷 QR Code événement",
                    "Scannez ce QR Code pour accéder aux informations de l’événement.",
                    stage
            );

            VBox content = new VBox(16);
            content.setPadding(new Insets(22));
            content.setAlignment(Pos.CENTER);

            ImageView qrImage = QRCodeService.generateEventQRCode(ev.getId_Evenn());

            if (qrImage != null) {
                qrImage.setFitWidth(250);
                qrImage.setFitHeight(250);
            }

            Label title = new Label(safe(ev.getTitre(), "Événement"));
            title.setWrapText(true);
            title.setAlignment(Pos.CENTER);
            title.setStyle("-fx-text-fill: #5A1730; -fx-font-size: 16px; -fx-font-weight: 900;");

            if (qrImage != null) {
                content.getChildren().add(qrImage);
            }

            content.getChildren().add(title);

            Button download = boutonPrimaire("Télécharger");
            download.setOnAction(e -> telechargerQREvent(qrImage, safe(ev.getTitre(), "evenement")));

            Button close = boutonSecondaire("Fermer");
            close.setOnAction(e -> stage.close());

            HBox footer = new HBox(10);
            footer.setAlignment(Pos.CENTER_RIGHT);
            footer.setPadding(new Insets(0, 22, 22, 22));
            footer.getChildren().addAll(download, close);

            root.getChildren().addAll(header, content, footer);

            Scene scene = new Scene(root, 460, 560);
            copierStyles(scene);

            stage.setScene(scene);
            stage.showAndWait();
        }

        private void telechargerQREvent(ImageView qrImage, String titre) {
            if (qrImage == null) return;

            FileChooser fc = new FileChooser();
            fc.setTitle("Enregistrer le QR Code");
            fc.setInitialFileName("QR_Event_" + titre.replaceAll("[^a-zA-Z0-9]", "_") + ".png");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Image", "*.png"));

            File file = fc.showSaveDialog(obtenirStage());

            if (file == null) return;

            try {
                WritableImage image = qrImage.snapshot(null, null);
                BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
                ImageIO.write(bufferedImage, "png", file);

                montrerAlerte("Succès", "QR Code sauvegardé.", Alert.AlertType.INFORMATION);

            } catch (IOException e) {
                montrerAlerte("Erreur", "Impossible de sauvegarder : " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }

        private void partagerEvenement(RendezVous.Evenn ev) {
            if (ev == null) return;

            String text = String.format(
                    "Événement VITA Santé%n%nTitre : %s%nDate : %s%nLieu : %s%nRessource : %s%nDescription : %s",
                    safe(ev.getTitre(), "Sans titre"),
                    formatDateTime(ev.getDateEvenement()),
                    safe(ev.getLieu(), "Non défini"),
                    getRessourceNom(ev.getIdRessource()),
                    safe(ev.getDescription(), "")
            );

            ClipboardContent content = new ClipboardContent();
            content.putString(text);
            Clipboard.getSystemClipboard().setContent(content);

            montrerAlerte("Partage", "Les informations ont été copiées.", Alert.AlertType.INFORMATION);
        }

        /* ========================= SCANNER QR MODERNE ========================= */

        @FXML
        public void ouvrirScannerQR() {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/ScannerQRCode.fxml"));
                AnchorPane scannerContent = loader.load();

                NavigationManager.ScannerQRCodeController controller = loader.getController();
                controller.setOnCheckinCallback(this::chargerDonnees);

                Stage stage = new Stage();
                stage.initModality(Modality.APPLICATION_MODAL);

                Stage owner = obtenirStage();
                if (owner != null) {
                    stage.initOwner(owner);
                }

                stage.setTitle("Scanner QR Code");

                VBox root = createWindowRoot();

                HBox header = createWindowHeader(
                        "📷 Scanner QR Code",
                        "Scannez ou saisissez un code QR pour valider la présence à un événement.",
                        stage
                );

                VBox body = new VBox();
                body.setPadding(new Insets(20));
                body.setStyle(
                        "-fx-background-color: white;" +
                                "-fx-background-radius: 0 0 24 24;"
                );

                scannerContent.setStyle("-fx-background-color: transparent;");

                styliserBoutonsScanner(scannerContent);

                body.getChildren().add(scannerContent);
                VBox.setVgrow(scannerContent, Priority.ALWAYS);

                root.getChildren().addAll(header, body);

                Scene scene = new Scene(root, 760, 620);
                copierStyles(scene);

                stage.setScene(scene);
                stage.showAndWait();

            } catch (IOException e) {
                montrerAlerte(
                        "Erreur",
                        "Impossible d'ouvrir le scanner : " + e.getMessage(),
                        Alert.AlertType.ERROR
                );
            }
        }

        private void styliserBoutonsScanner(Node node) {
            if (node == null) return;

            if (node instanceof Button button) {
                String text = button.getText() == null ? "" : button.getText().toLowerCase();

                button.setMinHeight(40);
                button.setPrefHeight(40);
                button.setCursor(Cursor.HAND);

                if (text.contains("scanner")
                        || text.contains("valider")
                        || text.contains("confirmer")
                        || text.contains("check")
                        || text.contains("enregistrer")) {

                    button.setStyle(
                            "-fx-background-color: #8B1538;" +
                                    "-fx-text-fill: white;" +
                                    "-fx-background-radius: 14;" +
                                    "-fx-border-color: transparent;" +
                                    "-fx-padding: 9 18;" +
                                    "-fx-font-size: 12px;" +
                                    "-fx-font-weight: 900;" +
                                    "-fx-cursor: hand;"
                    );

                } else if (text.contains("annuler")
                        || text.contains("fermer")
                        || text.contains("retour")) {

                    button.setStyle(
                            "-fx-background-color: white;" +
                                    "-fx-text-fill: #8B1538;" +
                                    "-fx-border-color: #DBB5C1;" +
                                    "-fx-border-radius: 14;" +
                                    "-fx-background-radius: 14;" +
                                    "-fx-padding: 9 18;" +
                                    "-fx-font-size: 12px;" +
                                    "-fx-font-weight: 900;" +
                                    "-fx-cursor: hand;"
                    );

                } else if (text.contains("supprimer")
                        || text.contains("effacer")
                        || text.contains("reset")
                        || text.contains("réinitialiser")) {

                    button.setStyle(
                            "-fx-background-color: #B41F49;" +
                                    "-fx-text-fill: white;" +
                                    "-fx-background-radius: 14;" +
                                    "-fx-border-color: transparent;" +
                                    "-fx-padding: 9 18;" +
                                    "-fx-font-size: 12px;" +
                                    "-fx-font-weight: 900;" +
                                    "-fx-cursor: hand;"
                    );

                } else {
                    button.setStyle(
                            "-fx-background-color: #FFF8FA;" +
                                    "-fx-text-fill: #8B1538;" +
                                    "-fx-border-color: #EDD4DC;" +
                                    "-fx-border-radius: 14;" +
                                    "-fx-background-radius: 14;" +
                                    "-fx-padding: 9 18;" +
                                    "-fx-font-size: 12px;" +
                                    "-fx-font-weight: 900;" +
                                    "-fx-cursor: hand;"
                    );
                }
            }

            if (node instanceof Parent parent) {
                for (Node child : parent.getChildrenUnmodifiable()) {
                    styliserBoutonsScanner(child);
                }
            }
        }

        /* ========================= NOTIFICATIONS ========================= */

        private void chargerNotifications() {
            try {
                List<Entites.LocalDateTime.Notification> liste = sn.getAll();

                notifications.setAll(liste);

                if (notificationListView != null) {
                    notificationListView.setItems(notifications);
                }

                mettreAJourBadgeNotification();

            } catch (SQLException e) {
                System.err.println("Erreur chargement notifications : " + e.getMessage());
            }
        }

        private void mettreAJourBadgeNotification() {
            long unread = notifications.stream()
                    .filter(n -> n != null && !n.isRead())
                    .count();

            if (notificationBadge != null) {
                notificationBadge.setText(String.valueOf(unread));
                setVisibleManaged(notificationBadge, unread > 0);
            }

            if (notificationCountLabel != null) {
                notificationCountLabel.setText(notifications.size() + " MESSAGE(S) ACTIF(S)");
            }
        }

        @FXML
        public void toggleNotificationPanel() {
            if (notificationPanel == null) return;

            boolean visible = !notificationPanel.isVisible();
            setVisibleManaged(notificationPanel, visible);

            if (visible) {
                chargerNotifications();
            }
        }

        @FXML
        public void closeNotificationPanel() {
            setVisibleManaged(notificationPanel, false);
        }

        @FXML
        public void marquerToutLu() {
            try {
                sn.markAllAsRead();
                chargerNotifications();
            } catch (SQLException e) {
                montrerAlerte("Erreur", "Impossible de marquer les notifications : " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }

        private void configurerListeNotifications() {
            notificationListView.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(Entites.LocalDateTime.Notification notif, boolean empty) {
                    super.updateItem(notif, empty);

                    if (empty || notif == null) {
                        setText(null);
                        setGraphic(null);
                        return;
                    }

                    VBox card = new VBox(8);
                    card.setPadding(new Insets(14, 16, 14, 16));

                    card.setStyle(notif.isRead()
                            ? "-fx-background-color: white; -fx-background-radius: 16; -fx-border-color: #F0D7DF; -fx-border-radius: 16;"
                            : "-fx-background-color: #FFF8FA; -fx-background-radius: 16; -fx-border-color: #8B1538; -fx-border-radius: 16; -fx-border-width: 1.4;"
                    );

                    HBox header = new HBox(10);
                    header.setAlignment(Pos.CENTER_LEFT);

                    Label icon = new Label(getNotificationIcon(notif.getType()));
                    icon.setStyle("-fx-font-size: 16px;");

                    Label title = new Label(safe(notif.getTitre(), "Notification"));
                    title.setStyle("-fx-text-fill: #5A1730; -fx-font-size: 13px; -fx-font-weight: 900;");

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    Label time = new Label(formatTimeAgo(notif.getDateCreation()));
                    time.setStyle("-fx-text-fill: #A36277; -fx-font-size: 10px;");

                    header.getChildren().addAll(icon, title, spacer, time);

                    Label message = new Label(safe(notif.getMessage(), ""));
                    message.setWrapText(true);
                    message.setStyle("-fx-text-fill: #8A6070; -fx-font-size: 12px;");

                    card.getChildren().addAll(header, message);

                    card.setOnMouseClicked(e -> {
                        if (!notif.isRead()) {
                            try {
                                sn.markAsRead(notif.getId());
                                chargerNotifications();
                            } catch (SQLException ex) {
                                ex.printStackTrace();
                            }
                        }
                    });

                    setText(null);
                    setGraphic(card);
                }
            });
        }

        private String getNotificationIcon(String type) {
            if (type == null) return "🔔";

            return switch (type.toLowerCase()) {
                case "urgent", "alert" -> "⚠️";
                case "info" -> "ℹ️";
                case "success" -> "✅";
                case "event", "calendar" -> "📅";
                case "resource" -> "📦";
                default -> "🔔";
            };
        }

        private String formatTimeAgo(LocalDateTime date) {
            if (date == null) return "";

            long minutes = ChronoUnit.MINUTES.between(date, LocalDateTime.now());

            if (minutes < 1) return "À l'instant";
            if (minutes < 60) return "Il y a " + minutes + " min";

            long hours = ChronoUnit.HOURS.between(date, LocalDateTime.now());

            if (hours < 24) return "Il y a " + hours + " h";

            long days = ChronoUnit.DAYS.between(date, LocalDateTime.now());

            return "Il y a " + days + " j";
        }

        /* ========================= RESOURCE FILTER ========================= */

        public void setRessourceFiltre(int ressourceId, String ressourceNom) {
            this.ressourceFiltreId = ressourceId;
            this.ressourceFiltreNom = ressourceNom;

            if (ressourceFiltreLabel != null) {
                ressourceFiltreLabel.setText("Filtre actif : " + ressourceNom);
            }

            setVisibleManaged(ressourceFiltreLabel, true);
            setVisibleManaged(clearFiltreBtn, true);

            chargerDonnees();
        }

        @FXML
        public void clearFiltreRessource() {
            this.ressourceFiltreId = null;
            this.ressourceFiltreNom = null;

            setVisibleManaged(ressourceFiltreLabel, false);
            setVisibleManaged(clearFiltreBtn, false);

            chargerDonnees();
        }

        /* ========================= EXPORT TXT ========================= */

        private void exporterFicheTxt(RendezVous.Evenn ev) {
            if (ev == null) return;

            FileChooser fc = new FileChooser();
            fc.setTitle("Exporter la fiche événement");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier texte", "*.txt"));
            fc.setInitialFileName("evenement_" + safe(ev.getTitre(), "event").replaceAll("[^a-zA-Z0-9]", "_") + ".txt");

            File file = fc.showSaveDialog(obtenirStage());

            if (file == null) return;

            try (FileWriter fw = new FileWriter(file)) {
                fw.write("=====================================\n");
                fw.write("FICHE ÉVÉNEMENT VITA SANTÉ\n");
                fw.write("=====================================\n\n");
                fw.write("Titre : " + safe(ev.getTitre()) + "\n");
                fw.write("Date : " + formatDateTime(ev.getDateEvenement()) + "\n");
                fw.write("Lieu : " + safe(ev.getLieu()) + "\n");
                fw.write("Ressource : " + getRessourceNom(ev.getIdRessource()) + "\n\n");
                fw.write("Description :\n" + safe(ev.getDescription()) + "\n\n");
                fw.write("Généré le " + LocalDateTime.now().format(FMT) + "\n");

                montrerAlerte("Export réussi", "La fiche a été sauvegardée.", Alert.AlertType.INFORMATION);

            } catch (IOException e) {
                montrerAlerte("Erreur", "Impossible de créer le fichier : " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }

        /* ========================= EXPORT PDF ========================= */

        @FXML
        public void exporterCSV() {
            FileChooser fc = new FileChooser();
            fc.setTitle("Exporter les événements en PDF");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier PDF", "*.pdf"));

            String suffix = ressourceFiltreNom != null
                    ? "_" + ressourceFiltreNom.replace(" ", "_")
                    : "";

            fc.setInitialFileName(
                    "evenements_vita" + suffix + "_" +
                            LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) +
                            ".pdf"
            );

            File file = fc.showSaveDialog(obtenirStage());

            if (file == null) return;

            try {
                List<RendezVous.Evenn> all = se.getAll();

                if (ressourceFiltreId != null) {
                    final int fid = ressourceFiltreId;
                    all = all.stream()
                            .filter(ev -> ev != null && ev.getIdRessource() == fid)
                            .collect(Collectors.toList());
                }

                List<RendezVous.Evenn> visibleEvents = currentView == ViewMode.STATS
                        ? all
                        : filtrerEvenements(all, LocalDateTime.now());

                StringBuilder content = new StringBuilder();

                content.append("VITA Sante - Rapport des evenements\n");
                content.append("Date export : ").append(LocalDate.now().format(FMT)).append("\n");
                content.append("Nombre total visible : ").append(visibleEvents.size()).append("\n");

                if (ressourceFiltreNom != null) {
                    content.append("Filtre ressource : ").append(stripPdfText(ressourceFiltreNom)).append("\n");
                }

                content.append("\n");

                for (RendezVous.Evenn ev : visibleEvents) {
                    content.append("ID : ").append(ev.getId_Evenn()).append("\n");
                    content.append("Titre : ").append(stripPdfText(safe(ev.getTitre(), "Sans titre"))).append("\n");
                    content.append("Date : ").append(ev.getDateEvenement() != null ? ev.getDateEvenement().format(FMT) : "").append("\n");
                    content.append("Heure : ").append(ev.getDateEvenement() != null ? ev.getDateEvenement().format(TIME_FMT) : "").append("\n");
                    content.append("Lieu : ").append(stripPdfText(safe(ev.getLieu()))).append("\n");
                    content.append("Ressource : ").append(stripPdfText(getRessourceNom(ev.getIdRessource()))).append("\n");
                    content.append("Description : ").append(stripPdfText(safe(ev.getDescription()).replace("\n", " "))).append("\n");
                    content.append("----------------------------------------\n");
                }

                writeSimplePdf(file, content.toString());

                montrerAlerte("Export PDF réussi", "Le fichier PDF a été sauvegardé avec succès.", Alert.AlertType.INFORMATION);

            } catch (IOException | SQLException e) {
                montrerAlerte("Erreur d'export", "Impossible de créer le fichier : " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }

        private void writeSimplePdf(File file, String text) throws IOException {
            String[] lines = text.split("\\R");

            StringBuilder stream = new StringBuilder("BT\n/F1 11 Tf\n50 790 Td\n14 TL\n");

            int lineCount = 0;

            for (String line : lines) {
                if (lineCount > 0 && lineCount % 52 == 0) {
                    stream.append("(Suite limitee dans ce PDF simplifie) Tj\n");
                    break;
                }

                stream.append("(").append(escapePdf(line)).append(") Tj\nT*\n");
                lineCount++;
            }

            stream.append("ET");

            byte[] streamBytes = stream.toString().getBytes(StandardCharsets.ISO_8859_1);

            byte[] header = "%PDF-1.4\n".getBytes(StandardCharsets.ISO_8859_1);
            byte[] obj1 = "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n".getBytes(StandardCharsets.ISO_8859_1);
            byte[] obj2 = "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n".getBytes(StandardCharsets.ISO_8859_1);
            byte[] obj3 = "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>\nendobj\n".getBytes(StandardCharsets.ISO_8859_1);
            byte[] obj4 = "4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n".getBytes(StandardCharsets.ISO_8859_1);

            byte[] obj5Pre = ("5 0 obj\n<< /Length " + streamBytes.length + " >>\nstream\n").getBytes(StandardCharsets.ISO_8859_1);
            byte[] obj5Suf = "\nendstream\nendobj\n".getBytes(StandardCharsets.ISO_8859_1);

            try (FileOutputStream out = new FileOutputStream(file)) {
                int p1 = header.length;
                int p2 = p1 + obj1.length;
                int p3 = p2 + obj2.length;
                int p4 = p3 + obj3.length;
                int p5 = p4 + obj4.length;

                int xref = p5 + obj5Pre.length + streamBytes.length + obj5Suf.length;

                out.write(header);
                out.write(obj1);
                out.write(obj2);
                out.write(obj3);
                out.write(obj4);
                out.write(obj5Pre);
                out.write(streamBytes);
                out.write(obj5Suf);

                String trailer = "xref\n0 6\n0000000000 65535 f \n"
                        + String.format("%010d 00000 n \n", p1)
                        + String.format("%010d 00000 n \n", p2)
                        + String.format("%010d 00000 n \n", p3)
                        + String.format("%010d 00000 n \n", p4)
                        + String.format("%010d 00000 n \n", p5)
                        + "trailer\n<< /Size 6 /Root 1 0 R >>\n"
                        + "startxref\n" + xref + "\n%%EOF";

                out.write(trailer.getBytes(StandardCharsets.ISO_8859_1));
            }
        }

        private String stripPdfText(String value) {
            if (value == null) return "";
            return value.replaceAll("[^\\x20-\\x7E]", "");
        }

        private String escapePdf(String value) {
            return stripPdfText(value)
                    .replace("\\", "\\\\")
                    .replace("(", "\\(")
                    .replace(")", "\\)");
        }

        /* ========================= NAVIGATION ========================= */

        @FXML
        public void allerTableauBord() {
            montrerAlerte("Navigation", "Tableau de bord principal.", Alert.AlertType.INFORMATION);
        }

        @FXML
        public void allerEvenements() {
            applyView(ViewMode.GRID);
            chargerDonnees();
        }

        @FXML
        public void allerRessources() {
            montrerAlerte("Ressources", "Module ressources.", Alert.AlertType.INFORMATION);
        }

        @FXML
        public void allerRendezVous() {
            montrerAlerte("Rendez-vous", "Module rendez-vous.", Alert.AlertType.INFORMATION);
        }

        @FXML
        public void allerDossiersMedicaux() {
            montrerAlerte("Dossiers médicaux", "Module dossiers médicaux.", Alert.AlertType.INFORMATION);
        }

        @FXML
        public void allerMedicaments() {
            montrerAlerte("Médicaments", "Module médicaments.", Alert.AlertType.INFORMATION);
        }

        @FXML
        public void allerQuiz() {
            montrerAlerte("Quiz santé", "Module quiz santé.", Alert.AlertType.INFORMATION);
        }

        @FXML
        public void voirStatistiques() {
            switchToStatsView();
        }

        @FXML
        public void deconnecter() {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Déconnexion");
            confirm.setHeaderText(null);
            confirm.setContentText("Voulez-vous vraiment fermer la session ?");

            Stage owner = obtenirStage();
            if (owner != null) confirm.initOwner(owner);

            confirm.showAndWait().ifPresent(result -> {
                if (result == ButtonType.OK) {
                    Stage stage = obtenirStage();
                    if (stage != null) stage.close();
                }
            });
        }

        /* ========================= HELPERS FENÊTRES ========================= */

        private VBox createWindowRoot() {
            VBox root = new VBox();
            root.setStyle(
                    "-fx-background-color: white;" +
                            "-fx-background-radius: 24;" +
                            "-fx-border-color: #F0D7DF;" +
                            "-fx-border-radius: 24;" +
                            "-fx-effect: dropshadow(gaussian, rgba(87,20,43,0.22), 30, 0.18, 0, 8);"
            );
            return root;

        }

        private HBox createWindowHeader(String title, String subtitle, Stage stage) {
            HBox header = new HBox(12);
            header.setAlignment(Pos.CENTER_LEFT);
            header.setPadding(new Insets(20, 22, 18, 22));
            header.setStyle(
                    "-fx-background-color: linear-gradient(to right, #FFF8FA, white);" +
                            "-fx-background-radius: 24 24 0 0;" +
                            "-fx-border-color: transparent transparent #F0D7DF transparent;" +
                            "-fx-border-width: 0 0 1 0;"
            );

            VBox texts = new VBox(4);
            HBox.setHgrow(texts, Priority.ALWAYS);

            Label titleLabel = new Label(title);
            titleLabel.setStyle("-fx-text-fill: #5A1730; -fx-font-size: 19px; -fx-font-weight: 900;");

            Label subLabel = new Label(subtitle);
            subLabel.setWrapText(true);
            subLabel.setStyle("-fx-text-fill: #8A6070; -fx-font-size: 12px;");

            texts.getChildren().addAll(titleLabel, subLabel);

            Button close = new Button("✕");
            close.setStyle(
                    "-fx-background-color: white;" +
                            "-fx-border-color: #EDD4DC;" +
                            "-fx-border-radius: 12;" +
                            "-fx-background-radius: 12;" +
                            "-fx-text-fill: #8B1538;" +
                            "-fx-font-size: 14px;" +
                            "-fx-font-weight: 900;" +
                            "-fx-cursor: hand;"
            );
            close.setOnAction(e -> stage.close());

            header.getChildren().addAll(texts, close);

            return header;
        }

        private Button boutonPrimaire(String text) {
            Button btn = new Button(text);
            btn.setStyle(
                    "-fx-background-color: #8B1538;" +
                            "-fx-text-fill: white;" +
                            "-fx-background-radius: 14;" +
                            "-fx-border-color: transparent;" +
                            "-fx-padding: 9 16;" +
                            "-fx-font-size: 12px;" +
                            "-fx-font-weight: 900;" +
                            "-fx-cursor: hand;"
            );
            return btn;
        }

        private Button boutonSecondaire(String text) {
            Button btn = new Button(text);
            btn.setStyle(
                    "-fx-background-color: white;" +
                            "-fx-text-fill: #8B1538;" +
                            "-fx-border-color: #DBB5C1;" +
                            "-fx-border-radius: 14;" +
                            "-fx-background-radius: 14;" +
                            "-fx-padding: 9 14;" +
                            "-fx-font-size: 12px;" +
                            "-fx-font-weight: 900;" +
                            "-fx-cursor: hand;"
            );
            return btn;
        }

        private Button boutonDanger(String text) {
            Button btn = new Button(text);
            btn.setStyle(
                    "-fx-background-color: #B41F49;" +
                            "-fx-text-fill: white;" +
                            "-fx-background-radius: 14;" +
                            "-fx-border-color: transparent;" +
                            "-fx-padding: 9 14;" +
                            "-fx-font-size: 12px;" +
                            "-fx-font-weight: 900;" +
                            "-fx-cursor: hand;"
            );
            return btn;
        }

        private void copierStyles(Scene scene) {
            try {
                Stage owner = obtenirStage();

                if (owner != null && owner.getScene() != null) {
                    scene.getStylesheets().addAll(owner.getScene().getStylesheets());
                }
            } catch (Exception ignored) {
            }
        }

        /* ========================= UTILITAIRES ========================= */

        private Stage obtenirStage() {
            if (appShell != null && appShell.getScene() != null) {
                return (Stage) appShell.getScene().getWindow();
            }

            if (topExportBtn != null && topExportBtn.getScene() != null) {
                return (Stage) topExportBtn.getScene().getWindow();
            }

            if (adminNameLabel != null && adminNameLabel.getScene() != null) {
                return (Stage) adminNameLabel.getScene().getWindow();
            }

            return null;
        }

        private void montrerAlerte(String titre, String contenu, Alert.AlertType type) {
            Alert alert = new Alert(type);
            alert.setTitle(titre);
            alert.setHeaderText(null);
            alert.setContentText(contenu);

            Stage owner = obtenirStage();
            if (owner != null) {
                alert.initOwner(owner);
            }

            alert.showAndWait();
        }

        private void setVisibleManaged(Node node, boolean visible) {
            if (node != null) {
                node.setVisible(visible);
                node.setManaged(visible);
            }
        }

        private void setText(Label label, String text) {
            if (label != null) {
                label.setText(text);
            }
        }

        private String safe(String value) {
            return value != null ? value : "";
        }

        private String safe(String value, String fallback) {
            return value != null && !value.isBlank() ? value : fallback;
        }

        private boolean contains(String source, String query) {
            return source != null && source.toLowerCase().contains(query);
        }

        private String shorten(String value, int max) {
            String safeValue = safe(value);

            if (safeValue.length() <= max) return safeValue;

            return safeValue.substring(0, Math.max(0, max - 3)) + "...";
        }

        private String formatDateTime(LocalDateTime dateTime) {
            if (dateTime == null) return "Date non définie";

            return dateTime.format(FMT) + " à " + dateTime.format(TIME_FMT);
        }

        private String getRessourceNom(int idRessource) {
            if (ressources == null || ressources.isEmpty()) return "Sans ressource";

            return ressources.stream()
                    .filter(r -> r != null && r.getIdRessource() == idRessource)
                    .map(RendezVous.Ressource::getNomRessource)
                    .findFirst()
                    .orElse("Aucune ressource");
        }
    }
}



/*package Controlers;

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


// ══════════════════════════════════════════════════════
//  ModifierRdvController.java
// ══════════════════════════════════════════════════════*/