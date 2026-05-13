package controles;

import javafx.scene.image.WritableImage;
import javafx.embed.swing.SwingFXUtils;
import javafx.stage.FileChooser;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import entities.RendezVous.Evenn;
import entities.RendezVous.Ressource;
import entities.RendezVous.ReservationPersonne;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import services.QRCodeService;
import services.ServiceEvenn;
import services.ServiceRessource;
import services.ServiceReservationPersonne;
import tests.Mainfx;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class UserEvenementController {

    // ==================== FXML BINDINGS ====================

    @FXML private BorderPane rootPane;
    @FXML private HBox userSearchBox;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> forumFilterCombo;
    @FXML private Button listViewBtn;
    @FXML private Button calendarViewBtn;
    @FXML private Button reservationsViewBtn;   // NOUVEAU bouton onglet
    @FXML private VBox listView;
    @FXML private VBox calendarView;
    @FXML private VBox reservationsView;        // NOUVEAU panneau réservations
    @FXML private VBox reservationsContainer;   // NOUVEAU conteneur cartes
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
    // Stat réservations (sidebar)
    @FXML private Label reservationsTotalLabel;
    @FXML private Label reservationsAccepteesLabel;
    @FXML private Label reservationsAttenteLabel;
    @FXML private Label reservationsRefuseesLabel;

    // ==================== SERVICES ====================

    private final ServiceEvenn serviceEvenn = new ServiceEvenn();
    private final ServiceRessource serviceRessource = new ServiceRessource();
    private final ServiceReservationPersonne srp = new ServiceReservationPersonne();

    // ==================== STATE ====================

    private List<Evenn> allEvents = new ArrayList<>();
    private List<Evenn> filteredEvents = new ArrayList<>();
    private List<Ressource> ressources = new ArrayList<>();
    private List<ReservationPersonne> mesReservations = new ArrayList<>();
    private final Map<Integer, String> reservationStatusByEventId = new HashMap<>();
    private YearMonth currentMonth = YearMonth.now();

    private static final Locale FR = Locale.FRENCH;
    private static final DateTimeFormatter FMT_DATE_LONG = DateTimeFormatter.ofPattern("dd MMMM yyyy", FR);
    private static final DateTimeFormatter FMT_TIME = DateTimeFormatter.ofPattern("HH:mm", FR);

    // ==================== INITIALISATION ====================

    @FXML
    public void initialize() {
        chargerRessources();
        chargerEvenements();
        chargerStatutsReservations();
        chargerMesReservations();
        filtrerEvenements();
        initialiserListeners();
        appliquerMenuActif(menuEvenementsBtn);
        switchToListView();
    }

    private void initialiserListeners() {
        if (searchField != null) {
            searchField.textProperty().addListener((obs, o, n) -> {
                validerRecherche();
                filtrerEvenements();
            });
        }
        if (forumFilterCombo != null) {
            forumFilterCombo.valueProperty().addListener((obs, o, n) -> filtrerEvenements());
        }
    }

    // ==================== CHARGEMENT ====================

    private void chargerRessources() {
        try {
            ressources = serviceRessource.getAll();
            List<String> options = ressources.stream()
                    .filter(Objects::nonNull)
                    .map(Ressource::getNomRessource)
                    .filter(Objects::nonNull)
                    .filter(n -> !n.isBlank())
                    .distinct().sorted()
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
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger les événements.");
        }
    }

    private void chargerStatutsReservations() {
        reservationStatusByEventId.clear();
        String userEmail = getUserEmail();
        if (userEmail == null || userEmail.isBlank()) return;
        try {
            List<ReservationPersonne> reservations = srp.getByEmail(userEmail.trim());
            for (ReservationPersonne r : reservations) {
                if (r == null) continue;
                reservationStatusByEventId.putIfAbsent(r.getIdEvenement(), normaliserStatut(r.getStatut()));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** Charge les réservations de l'utilisateur et rafraîchit la liste et les stats. */
    private void chargerMesReservations() {
        try {
            mesReservations = srp.getByEmail(getUserEmail());
        } catch (SQLException e) {
            mesReservations = new ArrayList<>();
        }
        afficherListeReservations();
        mettreAJourStatsReservations();
    }

    // ==================== FILTRAGE / AFFICHAGE ÉVÉNEMENTS ====================

    private void validerRecherche() {
        if (searchField == null) return;
        String t = searchField.getText();
        if (t == null || t.isBlank()) return;
        String clean = t.replaceAll("[<>{}]", "");
        if (!clean.equals(t)) searchField.setText(clean);
        if (clean.length() > 60) searchField.setText(clean.substring(0, 60));
    }

    private void filtrerEvenements() {
        String recherche = searchField != null && searchField.getText() != null
                ? searchField.getText().trim().toLowerCase(FR) : "";
        String forum = forumFilterCombo != null ? forumFilterCombo.getValue() : "Tous les forums";

        filteredEvents = allEvents.stream()
                .filter(Objects::nonNull)
                .filter(ev -> {
                    boolean r = recherche.isBlank()
                            || contient(ev.getTitre(), recherche)
                            || contient(ev.getLieu(), recherche)
                            || contient(ev.getDescription(), recherche)
                            || contient(getRessourceName(ev.getIdRessource()), recherche);
                    boolean f = forum == null || "Tous les forums".equals(forum)
                            || forum.equalsIgnoreCase(getRessourceName(ev.getIdRessource()));
                    return r && f;
                })
                .sorted(Comparator.comparing(Evenn::getDateEvenement,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        afficherListe(filteredEvents);
        mettreAJourStats(filteredEvents);
        mettreAJourGraphiques(filteredEvents);
        mettreAJourCalendrier();
    }

    private boolean contient(String source, String mot) {
        return source != null && source.toLowerCase(FR).contains(mot);
    }

    // ==================== VUES ====================

    @FXML
    private void switchToListView() {
        setVisible(listView, true);
        setVisible(calendarView, false);
        setVisible(reservationsView, false);
        setVisibleManaged(userSearchBox, true);
        setVisibleManaged(forumFilterCombo, true);
        activerOnglet(listViewBtn);
    }

    @FXML
    private void switchToCalendarView() {
        setVisible(listView, false);
        setVisible(calendarView, true);
        setVisible(reservationsView, false);
        setVisibleManaged(userSearchBox, false);
        setVisibleManaged(forumFilterCombo, false);
        activerOnglet(calendarViewBtn);
        mettreAJourCalendrier();
    }

    @FXML
    private void switchToReservationsView() {
        setVisible(listView, false);
        setVisible(calendarView, false);
        setVisible(reservationsView, true);
        setVisibleManaged(userSearchBox, false);
        setVisibleManaged(forumFilterCombo, false);
        activerOnglet(reservationsViewBtn);
        chargerMesReservations();
    }

    private void setVisible(VBox box, boolean v) {
        if (box != null) { box.setVisible(v); box.setManaged(v); }
    }

    private void setVisibleManaged(Node node, boolean v) {
        if (node != null) { node.setVisible(v); node.setManaged(v); }
    }

    private void activerOnglet(Button actif) {
        for (Button b : new Button[]{listViewBtn, calendarViewBtn, reservationsViewBtn}) {
            if (b == null) continue;
            b.getStyleClass().remove("btn-tab-active");
            if (b == actif) b.getStyleClass().add("btn-tab-active");
        }
    }

    // ==================== LISTE ÉVÉNEMENTS ====================

    private void afficherListe(List<Evenn> events) {
        if (eventsContainer == null) return;
        eventsContainer.getChildren().clear();
        LocalDateTime now = LocalDateTime.now();

        List<Evenn> upcoming = events.stream()
                .filter(e -> e.getDateEvenement() != null && !e.getDateEvenement().isBefore(now))
                .sorted(Comparator.comparing(Evenn::getDateEvenement))
                .collect(Collectors.toList());
        List<Evenn> past = events.stream()
                .filter(e -> e.getDateEvenement() == null || e.getDateEvenement().isBefore(now))
                .sorted(Comparator.comparing(Evenn::getDateEvenement,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        if (upcoming.isEmpty() && past.isEmpty()) {
            eventsContainer.getChildren().add(creerCarteVide());
            return;
        }
        if (!upcoming.isEmpty()) eventsContainer.getChildren().add(creerSection("Événements à venir", upcoming, true));
        if (!past.isEmpty())     eventsContainer.getChildren().add(creerSection("Événements passés", past, false));
    }

    private VBox creerSection(String titre, List<Evenn> events, boolean upcoming) {
        VBox section = new VBox(16);
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label dot = new Label("●");
        dot.getStyleClass().add(upcoming ? "dot-upcoming" : "dot-past");
        Label lbl = new Label(titre);
        lbl.getStyleClass().add("section-title");
        header.getChildren().addAll(dot, lbl);

        FlowPane flow = new FlowPane();
        flow.setHgap(20); flow.setVgap(20);
        flow.setPrefWrapLength(1080);
        for (Evenn ev : events) flow.getChildren().add(creerCarte(ev, upcoming));

        section.getChildren().addAll(header, flow);
        return section;
    }

    private VBox creerCarteVide() {
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(30));
        box.getStyleClass().add("empty-state-card");
        Label t = new Label("Aucun événement trouvé");
        t.getStyleClass().add("empty-state-title");
        Label m = new Label("Essayez une autre recherche ou changez le filtre du forum.");
        m.getStyleClass().add("empty-state-text");
        box.getChildren().addAll(t, m);
        return box;
    }

    private VBox creerCarte(Evenn event, boolean upcoming) {
        VBox card = new VBox(14);
        card.setPadding(new Insets(22));
        card.setPrefWidth(340); card.setMinWidth(320);
        card.getStyleClass().add("event-card");

        // --- top row ---
        HBox top = new HBox();
        top.setAlignment(Pos.CENTER_LEFT);
        String statut = getReservationStatutPourEvenement(event);

        Label badge = new Label(upcoming ? "À venir" : "Passé");
        badge.getStyleClass().add(upcoming ? "event-badge-upcoming" : "event-badge-past");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label icon = new Label("EV");
        icon.getStyleClass().add("event-icon-box");

        if (statut != null) {
            Label statusChip = new Label(getStatutEmoji(statut) + " " + getStatutLabel(statut));
            statusChip.getStyleClass().addAll("reservation-status-chip", getStatutCssClass(statut));
            VBox badgeStack = new VBox(6);
            badgeStack.getChildren().addAll(badge, statusChip);
            top.getChildren().addAll(badgeStack, spacer, icon);
        } else {
            top.getChildren().addAll(badge, spacer, icon);
        }

        Label titre = new Label(valeurOuDefaut(event.getTitre(), "Sans titre"));
        titre.setWrapText(true);
        titre.getStyleClass().add("event-title");

        Separator sep = new Separator();
        sep.getStyleClass().add("soft-separator");

        VBox details = new VBox(10);
        details.getChildren().addAll(
                creerLigneDetail("📅", "Date",  event.getDateEvenement() != null ? event.getDateEvenement().format(FMT_DATE_LONG) : "Non précisée"),
                creerLigneDetail("🕒", "Heure", event.getDateEvenement() != null ? event.getDateEvenement().format(FMT_TIME) : "Non précisée"),
                creerLigneDetail("📍", "Lieu",  valeurOuDefaut(event.getLieu(), "Non précisé")),
                creerLigneDetail("🛡", "Forum", getRessourceName(event.getIdRessource()))
        );

        Label desc = new Label(valeurOuDefaut(event.getDescription(), "Aucune description"));
        desc.setWrapText(true);
        desc.getStyleClass().add("event-description");

        // --- actions ---
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_LEFT);

        Button detailsBtn = new Button("Voir détails");
        detailsBtn.getStyleClass().add("btn-primary");
        detailsBtn.setOnAction(e -> afficherDetails(event));

        Button shareBtn = new Button("Partager");
        shareBtn.getStyleClass().add("btn-outline");
        shareBtn.setOnAction(e -> partagerEvenement(event));

        Button agendaBtn = new Button("Agenda");
        agendaBtn.getStyleClass().add("btn-soft");
        agendaBtn.setOnAction(e -> ajouterAMonAgenda(event));

        Button reserverBtn = new Button("Réserver");
        reserverBtn.getStyleClass().add("btn-success");
        reserverBtn.setOnAction(e -> ouvrirFormulaireReservation(event));

        if ("EN_ATTENTE".equals(statut)) {
            reserverBtn.setText("⏳ En attente");
            reserverBtn.setDisable(true);
            reserverBtn.getStyleClass().add("btn-disabled");
        } else if ("ACCEPTE".equals(statut)) {
            reserverBtn.setText("✅ Acceptée");
            reserverBtn.setDisable(true);
            reserverBtn.getStyleClass().add("btn-disabled");
        } else if ("REFUSE".equals(statut)) {
            reserverBtn.setText("🔄 Réessayer");
            reserverBtn.getStyleClass().remove("btn-success");
            reserverBtn.getStyleClass().add("btn-warning");
        }

        actions.getChildren().addAll(detailsBtn, shareBtn, agendaBtn, reserverBtn);
        card.getChildren().addAll(top, titre, sep, details, desc, actions);
        return card;
    }

    // ==================== LISTE MES RÉSERVATIONS (onglet intégré) ====================

    private void afficherListeReservations() {
        if (reservationsContainer == null) return;
        reservationsContainer.getChildren().clear();

        if (mesReservations.isEmpty()) {
            reservationsContainer.getChildren().add(creerVideReservations());
            return;
        }

        // Trier : ACCEPTE d'abord, puis EN_ATTENTE, puis REFUSE
        List<ReservationPersonne> triees = mesReservations.stream()
                .sorted(Comparator.comparing(r -> getOrdreStatut(normaliserStatut(r.getStatut()))))
                .collect(Collectors.toList());

        FlowPane flow = new FlowPane();
        flow.setHgap(20); flow.setVgap(20);
        flow.setPrefWrapLength(1080);

        for (ReservationPersonne r : triees) {
            flow.getChildren().add(creerCarteReservation(r));
        }
        reservationsContainer.getChildren().add(flow);
    }

    private int getOrdreStatut(String statut) {
        switch (statut) {
            case "ACCEPTE":    return 0;
            case "EN_ATTENTE": return 1;
            case "REFUSE":     return 2;
            default:           return 3;
        }
    }

    private VBox creerVideReservations() {
        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(40));
        box.getStyleClass().add("empty-state-card");
        Label icon = new Label("📋");
        icon.setStyle("-fx-font-size: 38px;");
        Label t = new Label("Aucune réservation");
        t.getStyleClass().add("empty-state-title");
        Label m = new Label("Vous n'avez pas encore réservé d'événement.\nCliquez sur « Réserver » depuis la liste des événements.");
        m.getStyleClass().add("empty-state-text");
        m.setWrapText(true);
        m.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        box.getChildren().addAll(icon, t, m);
        return box;
    }

    private VBox creerCarteReservation(ReservationPersonne r) {
        String statut = normaliserStatut(r.getStatut());
        String eventTitre = getEventTitle(r.getIdEvenement());

        VBox card = new VBox(14);
        card.setPadding(new Insets(20));
        card.setPrefWidth(340); card.setMinWidth(300);
        card.getStyleClass().addAll("event-card", "reservation-card-" + statut.toLowerCase());

        // En-tête carte
        HBox top = new HBox(10);
        top.setAlignment(Pos.CENTER_LEFT);

        Label statusBadge = new Label(getStatutEmoji(statut) + "  " + getStatutLabel(statut));
        statusBadge.getStyleClass().addAll("reservation-status-chip-lg", getStatutCssClass(statut));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label iconLbl = new Label(getStatutIconBox(statut));
        iconLbl.getStyleClass().add("event-icon-box");

        top.getChildren().addAll(statusBadge, spacer, iconLbl);

        // Titre événement
        Label titreEvt = new Label(eventTitre);
        titreEvt.setWrapText(true);
        titreEvt.getStyleClass().add("event-title");

        Separator sep = new Separator();
        sep.getStyleClass().add("soft-separator");

        // Infos personne
        VBox infos = new VBox(8);
        infos.getChildren().addAll(
                creerLigneDetail("👤", "Nom",       r.getNomComplet()),
                creerLigneDetail("✉️", "Email",     r.getEmail()),
                creerLigneDetail("📞", "Téléphone", valeurOuDefaut(r.getTelephone(), "Non renseigné"))
        );

        // Message statut
        VBox msgBox = new VBox(4);
        msgBox.getStyleClass().add("statut-message-box-" + statut.toLowerCase());
        msgBox.setPadding(new Insets(10, 14, 10, 14));
        Label msgLbl = new Label(getStatutMessage(statut));
        msgLbl.setWrapText(true);
        msgLbl.getStyleClass().add("statut-message-text");
        msgBox.getChildren().add(msgLbl);

        // Boutons
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_LEFT);

        if ("ACCEPTE".equals(statut)) {
            Button qrBtn = new Button("📱 Voir QR Code");
            qrBtn.getStyleClass().add("btn-primary");
            qrBtn.setOnAction(e -> afficherQRCodeUser(r));
            actions.getChildren().add(qrBtn);
        } else if ("REFUSE".equals(statut)) {
            Button retryBtn = new Button("🔄 Réessayer");
            retryBtn.getStyleClass().add("btn-warning");
            retryBtn.setOnAction(e -> {
                // Trouver l'événement et rouvrir le formulaire
                allEvents.stream()
                        .filter(ev -> ev.getId_Evenn() == r.getIdEvenement())
                        .findFirst()
                        .ifPresent(ev -> {
                            reservationStatusByEventId.remove(ev.getId_Evenn());
                            ouvrirFormulaireReservation(ev);
                        });
            });
            actions.getChildren().add(retryBtn);
        }

        Button detailsBtn = new Button("📅 Voir événement");
        detailsBtn.getStyleClass().add("btn-outline");
        detailsBtn.setOnAction(e -> allEvents.stream()
                .filter(ev -> ev.getId_Evenn() == r.getIdEvenement())
                .findFirst().ifPresent(this::afficherDetails));
        actions.getChildren().add(detailsBtn);

        card.getChildren().addAll(top, titreEvt, sep, infos, msgBox, actions);
        return card;
    }

    private void mettreAJourStatsReservations() {
        long total    = mesReservations.size();
        long accepte  = mesReservations.stream().filter(r -> "ACCEPTE".equals(normaliserStatut(r.getStatut()))).count();
        long attente  = mesReservations.stream().filter(r -> "EN_ATTENTE".equals(normaliserStatut(r.getStatut()))).count();
        long refuse   = mesReservations.stream().filter(r -> "REFUSE".equals(normaliserStatut(r.getStatut()))).count();

        if (reservationsTotalLabel    != null) reservationsTotalLabel.setText(String.valueOf(total));
        if (reservationsAccepteesLabel != null) reservationsAccepteesLabel.setText(String.valueOf(accepte));
        if (reservationsAttenteLabel   != null) reservationsAttenteLabel.setText(String.valueOf(attente));
        if (reservationsRefuseesLabel  != null) reservationsRefuseesLabel.setText(String.valueOf(refuse));
    }

    // ==================== HELPERS STATUT ====================

    private String normaliserStatut(String statut) {
        if (statut == null || statut.isBlank()) return "EN_ATTENTE";
        String v = statut.trim().toUpperCase(Locale.ROOT);
        if ("ACCEPTEE".equals(v)) return "ACCEPTE";
        if ("REFUSEE".equals(v))  return "REFUSE";
        return v;
    }

    private String getReservationStatutPourEvenement(Evenn event) {
        if (event == null) return null;
        return reservationStatusByEventId.get(event.getId_Evenn());
    }

    private String getStatutLabel(String statut) {
        switch (normaliserStatut(statut)) {
            case "ACCEPTE":    return "Acceptée";
            case "REFUSE":     return "Refusée";
            default:           return "En attente";
        }
    }

    private String getStatutEmoji(String statut) {
        switch (normaliserStatut(statut)) {
            case "ACCEPTE":    return "✅";
            case "REFUSE":     return "❌";
            default:           return "⏳";
        }
    }

    private String getStatutIconBox(String statut) {
        switch (normaliserStatut(statut)) {
            case "ACCEPTE":    return "✓";
            case "REFUSE":     return "✕";
            default:           return "…";
        }
    }

    private String getStatutCssClass(String statut) {
        switch (normaliserStatut(statut)) {
            case "ACCEPTE":    return "reservation-status-accepted";
            case "REFUSE":     return "reservation-status-refused";
            default:           return "reservation-status-pending";
        }
    }

    private String getStatutMessage(String statut) {
        switch (normaliserStatut(statut)) {
            case "ACCEPTE":    return "Votre réservation est confirmée. Présentez le QR code à l'entrée de l'événement.";
            case "REFUSE":     return "Votre réservation a été refusée. Vous pouvez soumettre une nouvelle demande.";
            default:           return "Votre réservation est en cours de traitement. Vous serez notifié(e) sous 24h.";
        }
    }

    private String getStatutStyle(String statut) {
        switch (normaliserStatut(statut)) {
            case "ACCEPTE": return "-fx-background-color:#d5f5e3;-fx-text-fill:#27ae60;-fx-padding:4 10;-fx-background-radius:12;-fx-font-size:11px;";
            case "REFUSE":  return "-fx-background-color:#f5d5dc;-fx-text-fill:#c1283e;-fx-padding:4 10;-fx-background-radius:12;-fx-font-size:11px;";
            default:        return "-fx-background-color:#fef5e7;-fx-text-fill:#f39c12;-fx-padding:4 10;-fx-background-radius:12;-fx-font-size:11px;";
        }
    }

    private String getStatutText(String statut) {
        switch (normaliserStatut(statut)) {
            case "ACCEPTE": return "✅ Acceptée";
            case "REFUSE":  return "❌ Refusée";
            default:        return "⏳ En attente";
        }
    }

    // ==================== FORMULAIRE RÉSERVATION ====================

    private void ouvrirFormulaireReservation(Evenn event) {
        Dialog<ReservationPersonne> dialog = new Dialog<>();
        dialog.setTitle("Réserver un événement");
        dialog.setHeaderText("Inscription à : " + event.getTitre());

        ButtonType btnReserver = new ButtonType("Réserver", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnAnnuler  = new ButtonType("Annuler",  ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(btnReserver, btnAnnuler);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(12);
        grid.setPadding(new Insets(20));

        TextField nomField       = new TextField(); nomField.setPromptText("Nom complet *"); nomField.setPrefWidth(300);
        TextField emailField     = new TextField(); emailField.setPromptText("Adresse email *"); emailField.setPrefWidth(300);
        TextField telephoneField = new TextField(); telephoneField.setPromptText("Numéro de téléphone"); telephoneField.setPrefWidth(300);
        TextArea  commentField   = new TextArea();  commentField.setPromptText("Commentaire / demande"); commentField.setPrefRowCount(3); commentField.setWrapText(true); commentField.setPrefWidth(300);

        grid.add(new Label("Nom complet :"),  0, 0); grid.add(nomField,       1, 0);
        grid.add(new Label("Email :"),        0, 1); grid.add(emailField,     1, 1);
        grid.add(new Label("Téléphone :"),    0, 2); grid.add(telephoneField, 1, 2);
        grid.add(new Label("Commentaire :"),  0, 3); grid.add(commentField,   1, 3);
        dialog.getDialogPane().setContent(grid);

        Node okNode = dialog.getDialogPane().lookupButton(btnReserver);
        okNode.setDisable(true);
        nomField.textProperty().addListener((o, old, v) -> okNode.setDisable(v.isBlank() || emailField.getText().isBlank()));
        emailField.textProperty().addListener((o, old, v) -> okNode.setDisable(nomField.getText().isBlank() || v.isBlank()));

        dialog.setResultConverter(bt -> {
            if (bt == btnReserver) {
                if (nomField.getText().trim().isEmpty() || emailField.getText().trim().isEmpty()) {
                    showAlert("Champs requis", "Veuillez remplir votre nom et email.", Alert.AlertType.WARNING);
                    return null;
                }
                if (!emailField.getText().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                    showAlert("Email invalide", "Veuillez saisir un email valide.", Alert.AlertType.WARNING);
                    return null;
                }
                return new ReservationPersonne(event.getId_Evenn(),
                        nomField.getText().trim(), emailField.getText().trim(),
                        telephoneField.getText().trim(), commentField.getText().trim());
            }
            return null;
        });

        dialog.showAndWait().ifPresent(reservation -> {
            try {
                srp.add(reservation);
                reservationStatusByEventId.put(event.getId_Evenn(), normaliserStatut(reservation.getStatut()));
                chargerMesReservations();
                filtrerEvenements();
                showAlert("Réservation confirmée !",
                        "Votre réservation pour « " + event.getTitre() + " » a été enregistrée.\n" +
                                "Vous recevrez une confirmation par email sous 24h.",
                        Alert.AlertType.INFORMATION);
            } catch (SQLException e) {
                showAlert("Erreur", "Impossible d'enregistrer la réservation : " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    // ==================== DÉTAILS ÉVÉNEMENT ====================

    private void afficherDetails(Evenn event) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (rootPane != null && rootPane.getScene() != null)
            dialog.initOwner(rootPane.getScene().getWindow());
        dialog.setTitle("Détails de l'événement");

        VBox root = new VBox();
        root.getStyleClass().add("details-modal-root");

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20));
        header.getStyleClass().add("details-modal-header");

        VBox titleBox = new VBox(4);
        Label eyebrow = new Label("Détails de l'événement"); eyebrow.getStyleClass().add("details-eyebrow");
        Label title   = new Label(valeurOuDefaut(event.getTitre(), "Sans titre")); title.getStyleClass().add("details-title");
        titleBox.getChildren().addAll(eyebrow, title);

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Button closeTop = new Button("Fermer ✕"); closeTop.getStyleClass().add("btn-modal-close");
        closeTop.setOnAction(e -> dialog.close());
        header.getChildren().addAll(titleBox, spacer, closeTop);

        VBox content = new VBox(18); content.setPadding(new Insets(24));
        boolean futurs = event.getDateEvenement() != null && !event.getDateEvenement().isBefore(LocalDateTime.now());
        Label status = new Label(futurs ? "À venir" : "Passé");
        status.getStyleClass().add(futurs ? "event-badge-upcoming" : "event-badge-past");

        VBox infoCard = new VBox(12); infoCard.getStyleClass().add("details-card");
        Label infoTitle = new Label("Informations"); infoTitle.getStyleClass().add("details-section-title");
        infoCard.getChildren().addAll(infoTitle,
                creerLigneDetail("📅", "Date",  event.getDateEvenement() != null ? event.getDateEvenement().format(FMT_DATE_LONG) : "Non précisée"),
                creerLigneDetail("🕒", "Heure", event.getDateEvenement() != null ? event.getDateEvenement().format(FMT_TIME) : "Non précisée"),
                creerLigneDetail("📍", "Lieu",  valeurOuDefaut(event.getLieu(), "Non précisé")),
                creerLigneDetail("🛡", "Forum", getRessourceName(event.getIdRessource())));

        VBox descCard = new VBox(12); descCard.getStyleClass().add("details-card");
        Label descTitle = new Label("Description"); descTitle.getStyleClass().add("details-section-title");
        Label descVal   = new Label(valeurOuDefaut(event.getDescription(), "Aucune description"));
        descVal.setWrapText(true); descVal.getStyleClass().add("details-description");
        descCard.getChildren().addAll(descTitle, descVal);

        HBox actions = new HBox(12);
        Button reserverBtn = new Button("📝 Réserver"); reserverBtn.getStyleClass().add("btn-success");
        reserverBtn.setOnAction(e -> { dialog.close(); ouvrirFormulaireReservation(event); });
        Button addBtn   = new Button("Ajouter à mon agenda"); addBtn.getStyleClass().add("btn-primary");
        addBtn.setOnAction(e -> ajouterAMonAgenda(event));
        Button shareBtn = new Button("Partager"); shareBtn.getStyleClass().add("btn-outline");
        shareBtn.setOnAction(e -> partagerEvenement(event));
        Button closeBtn = new Button("Fermer"); closeBtn.getStyleClass().add("btn-soft");
        closeBtn.setOnAction(e -> dialog.close());
        actions.getChildren().addAll(reserverBtn, addBtn, shareBtn, closeBtn);

        content.getChildren().addAll(status, infoCard, descCard, actions);
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true); scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("transparent-scroll");
        root.getChildren().addAll(header, scroll);

        Scene scene = new Scene(root, 760, 560, Color.TRANSPARENT);
        URL cssUrl = getClass().getResource("/STYLE1.css");
        if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    // ==================== PARTAGE / AGENDA ====================

    private void partagerEvenement(Evenn event) {
        String resume = String.format("%s | %s à %s | %s | %s",
                valeurOuDefaut(event.getTitre(), "Sans titre"),
                event.getDateEvenement() != null ? event.getDateEvenement().format(FMT_DATE_LONG) : "Date non précisée",
                event.getDateEvenement() != null ? event.getDateEvenement().format(FMT_TIME) : "Heure non précisée",
                valeurOuDefaut(event.getLieu(), "Lieu non précisé"),
                getRessourceName(event.getIdRessource()));
        ClipboardContent cc = new ClipboardContent();
        cc.putString(resume);
        Clipboard.getSystemClipboard().setContent(cc);
        showAlert("Partager", "Les détails de l'événement ont été copiés dans le presse-papiers.");
    }

    private void ajouterAMonAgenda(Evenn event) {
        showAlert("Agenda", "L'événement « " + valeurOuDefaut(event.getTitre(), "Sans titre") + " » a été ajouté à votre agenda.");
    }

    // ==================== CALENDRIER ====================

    @FXML private void prevMonth()  { currentMonth = currentMonth.minusMonths(1); mettreAJourCalendrier(); }
    @FXML private void nextMonth()  { currentMonth = currentMonth.plusMonths(1);  mettreAJourCalendrier(); }
    @FXML private void goToToday()  { currentMonth = YearMonth.now();             mettreAJourCalendrier(); }

    private void mettreAJourCalendrier() {
        if (calendarGrid == null) return;
        calendarGrid.getChildren().clear();
        calendarGrid.getColumnConstraints().clear();
        calendarGrid.getRowConstraints().clear();

        for (int i = 0; i < 7; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / 7.0); cc.setHgrow(Priority.ALWAYS);
            calendarGrid.getColumnConstraints().add(cc);
        }
        for (int i = 0; i < 7; i++) {
            RowConstraints rc = new RowConstraints(); rc.setVgrow(Priority.ALWAYS);
            calendarGrid.getRowConstraints().add(rc);
        }

        if (monthYearLabel != null) {
            String mt = currentMonth.getMonth().getDisplayName(TextStyle.FULL, FR);
            monthYearLabel.setText(Character.toUpperCase(mt.charAt(0)) + mt.substring(1) + " " + currentMonth.getYear());
        }

        String[] jours = {"Lun","Mar","Mer","Jeu","Ven","Sam","Dim"};
        for (int i = 0; i < jours.length; i++) {
            Label h = new Label(jours[i]); h.getStyleClass().add("calendar-header");
            h.setMaxWidth(Double.MAX_VALUE); h.setAlignment(Pos.CENTER);
            calendarGrid.add(h, i, 0);
        }

        LocalDate firstDay = currentMonth.atDay(1);
        int startCol = firstDay.getDayOfWeek().getValue() - 1;
        int daysInMonth = currentMonth.lengthOfMonth();
        LocalDate today = LocalDate.now();
        int row = 1, col = startCol;

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate cd = currentMonth.atDay(day);
            List<Evenn> dayEvts = filteredEvents.stream()
                    .filter(ev -> ev.getDateEvenement() != null)
                    .filter(ev -> ev.getDateEvenement().toLocalDate().equals(cd))
                    .collect(Collectors.toList());
            calendarGrid.add(creerCelluleCalendrier(cd, cd.equals(today), dayEvts), col, row);
            if (++col == 7) { col = 0; row++; }
        }
    }

    private VBox creerCelluleCalendrier(LocalDate date, boolean isToday, List<Evenn> events) {
        VBox cell = new VBox(6); cell.setPadding(new Insets(10)); cell.setMinHeight(96);
        cell.getStyleClass().add("calendar-cell");
        if (isToday) cell.getStyleClass().add("calendar-cell-today");

        Label num = new Label(String.valueOf(date.getDayOfMonth()));
        num.getStyleClass().add(isToday ? "calendar-day-number-today" : "calendar-day-number");
        cell.getChildren().add(num);

        for (int i = 0; i < Math.min(events.size(), 2); i++) {
            Label chip = new Label(valeurOuDefaut(events.get(i).getTitre(), "Sans titre"));
            chip.getStyleClass().add("calendar-event-chip"); chip.setMaxWidth(Double.MAX_VALUE);
            cell.getChildren().add(chip);
        }
        if (events.size() > 2) {
            Label more = new Label("+" + (events.size() - 2) + " autres");
            more.getStyleClass().add("calendar-more-label");
            cell.getChildren().add(more);
        }
        cell.setOnMouseClicked(e -> { if (!events.isEmpty()) afficherDetails(events.get(0)); });
        return cell;
    }

    // ==================== STATS ====================

    private void mettreAJourStats(List<Evenn> events) {
        long total    = events.size();
        long upcoming = events.stream().filter(e -> e.getDateEvenement() != null && !e.getDateEvenement().isBefore(LocalDateTime.now())).count();
        long locs     = events.stream().map(Evenn::getLieu).filter(Objects::nonNull).filter(l -> !l.isBlank()).distinct().count();
        int  rate     = total > 0 ? (int) Math.round((double) upcoming / total * 100) : 0;

        if (statsLabel           != null) statsLabel.setText(String.valueOf(upcoming));
        if (totalEventsLabel     != null) totalEventsLabel.setText(String.valueOf(total));
        if (locationsCountLabel  != null) locationsCountLabel.setText(String.valueOf(locs));
        if (rateLabel            != null) rateLabel.setText(rate + "%");
    }

    private void mettreAJourGraphiques(List<Evenn> events) {
        long total    = events.size();
        long upcoming = events.stream().filter(e -> e.getDateEvenement() != null && !e.getDateEvenement().isBefore(LocalDateTime.now())).count();
        long past     = Math.max(0, total - upcoming);

        if (donutTotalLabel    != null) donutTotalLabel.setText(String.valueOf(total));
        if (donutUpcomingLabel != null) donutUpcomingLabel.setText("À venir — " + upcoming);
        if (donutPastLabel     != null) donutPastLabel.setText("Passés — " + past);

        if (donutProgressCircle != null) {
            double r = donutProgressCircle.getRadius();
            double circ = 2 * Math.PI * r;
            double prog = total == 0 ? 0 : circ * ((double) upcoming / total);
            donutProgressCircle.getStrokeDashArray().setAll(prog, Math.max(0, circ - prog));
        }
        genererBarresMensuelles(events);
    }

    private void genererBarresMensuelles(List<Evenn> events) {
        if (monthlyBarsContainer == null) return;
        monthlyBarsContainer.getChildren().clear();
        YearMonth now = YearMonth.now();
        List<YearMonth> months = new ArrayList<>();
        for (int i = 5; i >= 0; i--) months.add(now.minusMonths(i));

        long max = months.stream()
                .mapToLong(m -> events.stream().filter(e -> e.getDateEvenement() != null)
                        .filter(e -> YearMonth.from(e.getDateEvenement()).equals(m)).count())
                .max().orElse(1);

        for (YearMonth month : months) {
            long count = events.stream().filter(e -> e.getDateEvenement() != null)
                    .filter(e -> YearMonth.from(e.getDateEvenement()).equals(month)).count();
            VBox col = new VBox(8); col.setAlignment(Pos.BOTTOM_CENTER); col.setPrefWidth(58);
            Label cl = new Label(String.valueOf(count)); cl.getStyleClass().add("bar-count-label");
            Region bar = new Region(); bar.getStyleClass().add("month-bar");
            double h = 12 + (max == 0 ? 0 : (double) count / max * 110);
            bar.setPrefSize(40, h); bar.setMinHeight(h);
            Label ml = new Label(capitalize(month.getMonth().getDisplayName(TextStyle.SHORT, FR).replace(".", "")));
            ml.getStyleClass().add("bar-month-label");
            col.getChildren().addAll(cl, bar, ml);
            monthlyBarsContainer.getChildren().add(col);
        }
    }

    // ==================== QR CODE ====================

    private void afficherQRCodeUser(ReservationPersonne r) {
        Stage stage = new Stage();
        stage.setTitle("Mon QR Code");
        stage.initModality(Modality.APPLICATION_MODAL);

        VBox root = new VBox(15); root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: white;"); root.setAlignment(Pos.CENTER);

        final ImageView qrImage = QRCodeService.generateReservationQRCode(r.getId());
        if (qrImage != null) {
            qrImage.setFitWidth(220); qrImage.setFitHeight(220);
            root.getChildren().add(qrImage);
        }

        Label titleLbl = new Label(getEventTitle(r.getIdEvenement()));
        titleLbl.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#5A1730;");
        titleLbl.setWrapText(true); titleLbl.setAlignment(Pos.CENTER);

        Label nameLbl = new Label(r.getNomComplet());
        nameLbl.setStyle("-fx-font-size:14px;-fx-text-fill:#6b1a2a;");

        Label instrLbl = new Label("Présentez ce QR code à l'entrée de l'événement");
        instrLbl.setStyle("-fx-font-size:11px;-fx-text-fill:#A36277;");

        HBox btns = new HBox(10); btns.setAlignment(Pos.CENTER);
        Button btnClose = new Button("Fermer");
        btnClose.getStyleClass().add("btn-primary"); btnClose.setOnAction(e -> stage.close());
        btns.getChildren().add(btnClose);

        root.getChildren().addAll(titleLbl, nameLbl, instrLbl, btns);
        stage.setScene(new Scene(root, 360, 480));
        stage.show();
    }

    // ==================== MENU ====================

    @FXML private void menuEvenements()  { appliquerMenuActif(menuEvenementsBtn);  switchToListView(); }
    @FXML private void menuRendezVous()  { appliquerMenuActif(menuRendezVousBtn);  showAlert("Rendez-vous", "Section prête à être reliée."); }
    @FXML private void menuDossiers()    { appliquerMenuActif(menuDossiersBtn);    showAlert("Dossiers médicaux", "Section prête à être reliée."); }
    @FXML private void menuQuiz()        { appliquerMenuActif(menuQuizBtn);        showAlert("Quiz communauté", "Section prête à être reliée."); }
    @FXML private void menuMedicaments() { appliquerMenuActif(menuMedicamentsBtn); showAlert("Médicaments", "Section prête à être reliée."); }
    @FXML private void activerNotifications()  { showAlert("Notifications", "Les notifications des événements ont été activées."); }
    @FXML private void exporterMonCalendrier() { showAlert("Exporter", "L'export du calendrier est prêt."); }

    private void appliquerMenuActif(Button actif) {
        for (Button b : List.of(menuEvenementsBtn, menuRendezVousBtn, menuDossiersBtn, menuQuizBtn, menuMedicamentsBtn)) {
            if (b == null) continue;
            b.getStyleClass().remove("menu-btn-active");
            if (b == actif) b.getStyleClass().add("menu-btn-active");
        }
    }

    @FXML
    private void deconnecter() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Déconnexion"); confirm.setHeaderText(null);
        confirm.setContentText("Voulez-vous vraiment vous déconnecter ?");
        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                Stage stage = (Stage) rootPane.getScene().getWindow();
                stage.close();
                Mainfx.naviguerVers("/Login.fxml", "Connexion - VITA");
            }
        });
    }

    // ==================== SCANNER QR ====================

    @FXML
    private void ouvrirScannerUser() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/scanneruser.fxml"));
            AnchorPane root = loader.load();
            ScannerUserController ctrl = loader.getController();
            ctrl.setOnEventFoundCallback(() -> {});
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Scanner un événement");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir le scanner : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // ==================== UTILITAIRES ====================

    private String getUserEmail() {
        String sp = System.getProperty("vita.user.email");
        if (sp != null && !sp.isBlank()) return sp.trim();
        String ev = System.getenv("VITA_USER_EMAIL");
        if (ev != null && !ev.isBlank()) return ev.trim();
        return "test@test.com";
    }

    private String getEventTitle(int id) {
        try {
            Evenn ev = serviceEvenn.getById(id);
            return ev != null ? ev.getTitre() : "Événement inconnu";
        } catch (SQLException e) { return "Événement inconnu"; }
    }

    private String getRessourceName(int id) {
        return ressources.stream().filter(Objects::nonNull)
                .filter(r -> r.getIdRessource() == id)
                .map(Ressource::getNomRessource).findFirst()
                .orElse("Forum " + id);
    }

    private HBox creerLigneDetail(String iconText, String labelText, String valueText) {
        HBox box = new HBox(8); box.setAlignment(Pos.CENTER_LEFT);
        Label icon  = new Label(iconText);  icon.getStyleClass().add("detail-icon");
        Label label = new Label(labelText + " :"); label.getStyleClass().add("detail-label");
        Label value = new Label(valueText); value.setWrapText(true); value.getStyleClass().add("detail-value");
        box.getChildren().addAll(icon, label, value);
        return box;
    }

    private String valeurOuDefaut(String v, String fallback) {
        return v != null && !v.isBlank() ? v.trim() : fallback;
    }

    private String capitalize(String v) {
        if (v == null || v.isBlank()) return "";
        return Character.toUpperCase(v.charAt(0)) + v.substring(1);
    }

    private void showAlert(String titre, String contenu) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(titre); a.setHeaderText(null); a.setContentText(contenu); a.showAndWait();
    }

    private void showAlert(String titre, String contenu, Alert.AlertType type) {
        Alert a = new Alert(type);
        a.setTitle(titre); a.setHeaderText(null); a.setContentText(contenu); a.showAndWait();
    }
}
