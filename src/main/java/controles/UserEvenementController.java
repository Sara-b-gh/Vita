package controles;
import javafx.scene.image.WritableImage;
import javafx.embed.swing.SwingFXUtils;
import javafx.stage.FileChooser;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import entities.Evenn;
import entities.Ressource;
import entities.ReservationPersonne;
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

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;


public class UserEvenementController {

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

    private List<Evenn> allEvents = new ArrayList<>();
    private List<Evenn> filteredEvents = new ArrayList<>();
    private List<Ressource> ressources = new ArrayList<>();
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
                    .map(Ressource::getNomRessource)
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
                .sorted(Comparator.comparing(Evenn::getDateEvenement, Comparator.nullsLast(Comparator.naturalOrder())))
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

    private void afficherListe(List<Evenn> events) {
        if (eventsContainer == null) return;
        eventsContainer.getChildren().clear();

        LocalDateTime now = LocalDateTime.now();
        List<Evenn> upcoming = events.stream()
                .filter(event -> event.getDateEvenement() != null && !event.getDateEvenement().isBefore(now))
                .sorted(Comparator.comparing(Evenn::getDateEvenement))
                .collect(Collectors.toList());
        List<Evenn> past = events.stream()
                .filter(event -> event.getDateEvenement() == null || event.getDateEvenement().isBefore(now))
                .sorted(Comparator.comparing(Evenn::getDateEvenement, Comparator.nullsLast(Comparator.reverseOrder())))
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

    private VBox creerSection(String titre, List<Evenn> events, boolean upcoming) {
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

        for (Evenn event : events) {
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

    private VBox creerCarte(Evenn event, boolean upcoming) {
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
                .map(Ressource::getNomRessource)
                .findFirst()
                .orElse("Forum " + idRessource);
    }

    private void ouvrirFormulaireReservation(Evenn event) {
        Dialog<ReservationPersonne> dialog = new Dialog<>();
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
                return new ReservationPersonne(
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

    private void afficherDetails(Evenn event) {
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

    private void partagerEvenement(Evenn event) {
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

    private void ajouterAMonAgenda(Evenn event) {
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
            List<Evenn> dayEvents = filteredEvents.stream()
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

    private VBox creerCelluleCalendrier(LocalDate date, boolean isToday, List<Evenn> events) {
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
            Evenn event = events.get(i);
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

    private void mettreAJourStats(List<Evenn> events) {
        long total = events.size();
        long upcoming = events.stream()
                .filter(event -> event.getDateEvenement() != null && !event.getDateEvenement().isBefore(LocalDateTime.now()))
                .count();
        long locations = events.stream()
                .map(Evenn::getLieu)
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

    private void mettreAJourGraphiques(List<Evenn> events) {
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

    private void genererBarresMensuelles(List<Evenn> events) {
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

            ScannerUserController controller = loader.getController();
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
            List<ReservationPersonne> mesReservations = srp.getByEmail(userEmail);

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

            ListView<ReservationPersonne> listView = new ListView<>();
            listView.setItems(FXCollections.observableArrayList(mesReservations));
            listView.setCellFactory(lv -> new ListCell<ReservationPersonne>() {
                @Override
                protected void updateItem(ReservationPersonne r, boolean empty) {
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
            Evenn ev = serviceEvenn.getById(eventId);
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

    private void afficherQRCodeUser(ReservationPersonne r) {
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
