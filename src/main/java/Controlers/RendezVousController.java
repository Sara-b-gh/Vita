package Controlers;

import Entites.CompteRendu;
import Entites.Disponibilite;
import Entites.RendezVous;
import Entites.User;
import Services.CompteRenduCRUD;
import Services.DisponibiliteCRUD;
import Services.RendezVousCRUD;
import Services.UserService;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

public class RendezVousController implements Initializable {

    // ── FXML ──────────────────────────────────────────────────────────
    @FXML private StackPane rootStack;
    @FXML private BorderPane mainPane;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterStatut;
    @FXML private Label lblTotal, lblStatus;
    @FXML private ToggleButton btnVuePatient, btnVueDoc;
    @FXML private ToggleGroup vueGroup;

    // Zone centrale — deux vues
    @FXML private StackPane  centerStack;
    @FXML private ScrollPane scrollCenter;   // vue liste
    @FXML private ScrollPane scrollDetail;   // vue formulaire / détail
    @FXML private VBox       detailContent;  // contenu injecté dans scrollDetail

    @FXML private GridPane gridCards;

    private static final DateTimeFormatter FMT      = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_FMT  = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT  = DateTimeFormatter.ofPattern("HH:mm");
    private final CompteRenduCRUD compteRenduCRUD = new CompteRenduCRUD();
    private final RendezVousCRUD    rdvCRUD     = new RendezVousCRUD();
    private final DisponibiliteCRUD dispoCRUD   = new DisponibiliteCRUD();
    private final UserService       userService  = new UserService();

    private String nomPatient(int id) {
        User u = userService.findById(id);
        return u != null ? u.getPrenom() + " " + u.getNom() : "Patient #" + id;
    }

    private List<RendezVous>    allRdvs;
    private List<Disponibilite> allDispos;
    private List<User>          allDoctors = new ArrayList<>();

    private enum Vue { PATIENT, DOCTEUR }
    private Vue vueActive = Vue.PATIENT;

    private int currentPatientId = 1;
    private int currentMedecinId = 66;

    // State for the booking view
    private User          selectedDoctor  = null;
    private LocalDate     selectedDate    = null;
    private Disponibilite selectedCreneau = null;

    // ── Initialize ────────────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        filterStatut.getItems().addAll("Tous", "en_cours", "confirme", "annule", "termine");
        filterStatut.setValue("Tous");

        searchField.textProperty().addListener((o, old, n) -> applyFilters());
        filterStatut.valueProperty().addListener((o, old, n) -> applyFilters());

        try {
            allDoctors = userService.getDoctors();
        } catch (SQLException e) {
            System.out.println("Impossible de charger les médecins : " + e.getMessage());
        }

        loadData();
    }
    private void naviguerVersCompteRendu(int idRdv) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ConsulterCompteRendu.fxml"));
            BorderPane fullPane = loader.load();
            AfficherCompteRenduController ctrl = loader.getController();
            ctrl.filtrerParRdv(idRdv);

            javafx.scene.Node contenu = fullPane.getCenter();
            fullPane.setCenter(null);

            mainPane.setCenter(contenu);

        } catch (Exception e) {
            setStatus("Erreur navigation : " + e.getMessage(), true);
        }
    }
    private void ouvrirAjoutCompteRenduView(RendezVous rv) {
        try {
            CompteRendu existing = compteRenduCRUD.trouverParRdv(rv.getId_rdv());
            if (existing != null) {
                naviguerVersCompteRendu(rv.getId_rdv());
                return;
            }
        } catch (Exception e) {
            // aucun CR → formulaire de création
        }
        VBox body = new VBox(16);

        // ── Médecin (pré-rempli, non éditable) ───────────────────────────
        String doctorLabel = "Dr #" + rv.getMedecin_id();
        for (User d : allDoctors) {
            if (d.getId() == rv.getMedecin_id()) {
                doctorLabel = "Dr " + d.getNom() + " " + d.getPrenom();
                break;
            }
        }
        Label lblMedecin = new Label(doctorLabel);
        lblMedecin.setStyle(
                "-fx-font-size:13px;-fx-text-fill:#333;" +
                        "-fx-background-color:#f0e6ea;-fx-border-color:#e5b7c4;" +
                        "-fx-border-radius:8;-fx-background-radius:8;-fx-padding:8 10;"
        );
        lblMedecin.setMaxWidth(Double.MAX_VALUE);

        // ── Contenu ───────────────────────────────────────────────────────
        TextArea taContenu = new TextArea();
        taContenu.setPromptText("Contenu du compte rendu...");
        taContenu.setPrefRowCount(4);
        taContenu.setStyle(fieldStyle());
        taContenu.setWrapText(true);

        // ── Diagnostic ────────────────────────────────────────────────────
        TextField tfDiagnostic = styledField("Diagnostic médical");

        // ── Traitement ────────────────────────────────────────────────────
        TextArea taTraitement = new TextArea();
        taTraitement.setPromptText("Traitement prescrit...");
        taTraitement.setPrefRowCount(3);
        taTraitement.setStyle(fieldStyle());
        taTraitement.setWrapText(true);

        // ── Prochain RDV ──────────────────────────────────────────────────
        DatePicker dpProchain = new DatePicker();
        dpProchain.setStyle(fieldStyle());
        dpProchain.setMaxWidth(Double.MAX_VALUE);

        // ── Confidentiel ──────────────────────────────────────────────────
        CheckBox cbConf = new CheckBox("Marquer comme confidentiel");
        cbConf.setStyle("-fx-font-size:13px;-fx-text-fill:#555;");

        // ── Erreur ────────────────────────────────────────────────────────
        Label lblErr = new Label("");
        lblErr.setStyle("-fx-text-fill:#ff4b3a;-fx-font-size:12px;");
        lblErr.setWrapText(true);

        // ── Bouton Enregistrer ────────────────────────────────────────────
        Button btnSave = primaryBtn("Enregistrer le compte rendu");
        btnSave.setOnAction(e -> {
            lblErr.setText("");
            String contenu = taContenu.getText().trim();
            if (contenu.isEmpty()) {
                lblErr.setText("Le contenu du compte rendu est obligatoire.");
                return;
            }
            try {
                CompteRendu cr = new CompteRendu();
                cr.setId_rdv(rv.getId_rdv());
                cr.setRedige_par(rv.getMedecin_id());
                cr.setContenu(contenu);
                cr.setDiagnostic(tfDiagnostic.getText().trim());
                cr.setTraitement(taTraitement.getText().trim());
                cr.setProchain_rdv(dpProchain.getValue());
                cr.setConfidentiel(cbConf.isSelected());

                compteRenduCRUD.ajouter(cr);
                setStatus("Compte rendu ajouté pour le RDV #" + rv.getId_rdv(), false);
                fermerPopup();
            } catch (Exception ex) {
                lblErr.setText("Erreur : " + ex.getMessage());
            }
        });

        body.getChildren().addAll(
                sectionLabel("Médecin rédacteur"),
                formRow("Médecin", lblMedecin),
                sectionLabel("Contenu médical"),
                formRow("Contenu *", taContenu),
                formRow("Diagnostic", tfDiagnostic),
                formRow("Traitement", taTraitement),
                sectionLabel("Suivi"),
                formRow("Prochain RDV", dpProchain),
                cbConf,
                lblErr,
                btnSave
        );

        showPopup("Nouveau Compte Rendu — RDV #" + rv.getId_rdv(), body);
    }
    // ── Chargement ────────────────────────────────────────────────────
    public void loadData() {
        try {
            allRdvs   = rdvCRUD.afficher();
            allDispos = dispoCRUD.parMedecin(currentMedecinId);
            // Toujours revenir à la vue liste après rechargement
            afficherListe();
            applyFilters();
        } catch (SQLException e) {
            setStatus("Erreur chargement : " + e.getMessage(), true);
        }
    }

    // ── Navigation centre ─────────────────────────────────────────────

    /** Affiche la vue liste et masque le détail */
    private void afficherListe() {
        scrollDetail.setVisible(false);
        scrollDetail.setManaged(false);
        scrollCenter.setVisible(true);
        scrollCenter.setManaged(true);
    }

    /**
     * Remplace la zone centrale par une vue titre + contenu.
     * Un bouton "← Retour" ramène à la liste.
     */
    private void showPopup(String title, VBox content) {
        // ── Header ────────────────────────────────────────────────────
        HBox header = new HBox(14);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle(
                "-fx-background-color:white;" +
                        "-fx-border-color:transparent transparent #ececec transparent;" +
                        "-fx-padding:14 24;"
        );

        Button btnBack = new Button("← Retour");
        btnBack.setStyle(
                "-fx-background-color:#f0e6ea;-fx-text-fill:#7a002f;" +
                        "-fx-font-size:12px;-fx-font-weight:bold;" +
                        "-fx-border-radius:8;-fx-background-radius:8;" +
                        "-fx-padding:7 16;-fx-cursor:hand;"
        );
        btnBack.setOnAction(e -> fermerPopup());

        Label lblTitle = new Label(title);
        lblTitle.setStyle(
                "-fx-font-size:20px;-fx-font-weight:bold;-fx-text-fill:#1a1a2e;"
        );

        header.getChildren().addAll(btnBack, lblTitle);

        // ── Carte contenu ─────────────────────────────────────────────
        VBox card = new VBox(0);
        card.setStyle(
                "-fx-background-color:white;" +
                        "-fx-border-color:#e0e0e0;-fx-border-width:1;" +
                        "-fx-border-radius:14;-fx-background-radius:14;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),12,0,0,3);"
        );
        card.setPadding(new Insets(24));
        card.getChildren().add(content);

        VBox wrapper = new VBox(16);
        wrapper.setPadding(new Insets(20, 24, 24, 24));
        wrapper.getChildren().add(card);

        // ── Vue complète ──────────────────────────────────────────────
        VBox view = new VBox(0);
        view.getChildren().addAll(header, wrapper);

        detailContent.getChildren().setAll(view);

        // Transition
        scrollCenter.setVisible(false);
        scrollCenter.setManaged(false);
        scrollDetail.setVisible(true);
        scrollDetail.setManaged(true);

        FadeTransition ft = new FadeTransition(Duration.millis(200), scrollDetail);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    @FXML
    private void fermerPopup() {
        FadeTransition ft = new FadeTransition(Duration.millis(150), scrollDetail);
        ft.setFromValue(1);
        ft.setToValue(0);
        ft.setOnFinished(e -> afficherListe());
        ft.play();
    }

    // ── Filtres ───────────────────────────────────────────────────────
    private void applyFilters() {
        String search = searchField.getText().toLowerCase().trim();
        String statut = filterStatut.getValue();

        List<RendezVous> filtered = allRdvs.stream()
                .filter(rv -> {
                    if (vueActive == Vue.PATIENT && rv.getPatient_id() != currentPatientId) return false;
                    if (vueActive == Vue.DOCTEUR && rv.getMedecin_id() != currentMedecinId)  return false;
                    boolean matchSearch = search.isEmpty()
                            || (rv.getMotif() != null && rv.getMotif().toLowerCase().contains(search))
                            || (rv.getLieu()  != null && rv.getLieu().toLowerCase().contains(search))
                            || String.valueOf(rv.getPatient_id()).contains(search);
                    boolean matchStatut = "Tous".equals(statut) || rv.getStatut().equals(statut);
                    return matchSearch && matchStatut;
                })
                .collect(Collectors.toList());

        buildGrid(filtered);
        lblTotal.setText(filtered.size() + " rendez-vous");
    }

    // ── Basculer vues ─────────────────────────────────────────────────
    @FXML private void switchToPatient() {
        vueActive = Vue.PATIENT;
        filterStatut.setValue("Tous");
        applyFilters();
    }

    @FXML private void switchToDocteur() {
        vueActive = Vue.DOCTEUR;
        filterStatut.setValue("Tous");
        applyFilters();
    }

    // ── Grille de cartes ──────────────────────────────────────────────
    private void buildGrid(List<RendezVous> list) {
        gridCards.getChildren().clear();
        gridCards.getColumnConstraints().clear();

        for (int i = 0; i < 2; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(50);
            cc.setHgrow(Priority.ALWAYS);
            gridCards.getColumnConstraints().add(cc);
        }

        int col = 0, row = 0;
        for (RendezVous rv : list) {
            VBox card = buildCard(rv);
            gridCards.add(card, col, row);
            col++;
            if (col >= 2) { col = 0; row++; }
        }

        if (vueActive == Vue.DOCTEUR) {
            Label secTitle = new Label("Mes créneaux de disponibilité");
            secTitle.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:#7a002f;-fx-padding:20 0 8 0;");
            gridCards.add(secTitle, 0, row + 1, 2, 1);
            buildDispoCalendarSection(row + 2);
        }
    }

    // ── Carte RDV ─────────────────────────────────────────────────────
    private VBox buildCard(RendezVous rv) {
        String[] colors = statutColors(rv.getStatut());
        String borderColor = colors[0], bgColor = colors[1], hoverColor = colors[2];

        VBox card = new VBox(8);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setStyle(
                "-fx-border-color:" + borderColor + ";-fx-border-width:2;" +
                        "-fx-border-radius:12;-fx-background-radius:12;" +
                        "-fx-background-color:" + bgColor + ";-fx-padding:14 16;" +
                        "-fx-cursor:hand;"
        );

        Label lblDate = new Label(rv.getDate_rdv() != null ? rv.getDate_rdv().format(FMT) : "—");
        lblDate.setStyle("-fx-font-size:11px;-fx-text-fill:#888;");

        Label lblStatut = new Label(statutLabel(rv.getStatut()));
        lblStatut.setStyle(
                "-fx-font-size:10px;-fx-font-weight:bold;-fx-text-fill:" + borderColor + ";" +
                        "-fx-border-color:" + borderColor + ";-fx-border-radius:4;-fx-border-width:1;" +
                        "-fx-padding:2 6;-fx-background-radius:4;"
        );

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox top = new HBox(8, lblDate, sp, lblStatut);
        top.setAlignment(Pos.CENTER_LEFT);

        Label lblMotif = new Label(rv.getMotif() != null ? rv.getMotif() : "—");
        lblMotif.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#1a1a2e;");
        lblMotif.setWrapText(true);

        String doctorLabel = "Dr #" + rv.getMedecin_id();
        for (User d : allDoctors) {
            if (d.getId() == rv.getMedecin_id()) {
                doctorLabel = "Dr " + d.getNom() + " " + d.getPrenom();
                break;
            }
        }

        Label lblPerson = new Label(vueActive == Vue.PATIENT
                ? doctorLabel
                : "Patient " + nomPatient(rv.getPatient_id()));
        lblPerson.setStyle("-fx-font-size:11px;-fx-text-fill:#aaa;");

        Label lblLieu = new Label(rv.getLieu() != null && !rv.getLieu().isEmpty() ? "📍 " + rv.getLieu() : "");
        lblLieu.setStyle("-fx-font-size:11px;-fx-text-fill:#aaa;");

        HBox actions = buildCardActions(rv);

        card.getChildren().addAll(top, lblMotif, lblPerson);
        if (!lblLieu.getText().isEmpty()) card.getChildren().add(lblLieu);
        card.getChildren().add(actions);

        card.setOnMouseEntered(e -> card.setStyle(card.getStyle().replace(bgColor, hoverColor)));
        card.setOnMouseExited (e -> card.setStyle(card.getStyle().replace(hoverColor, bgColor)));

        return card;
    }

    // ── Boutons d'action carte ────────────────────────────────────────
    private HBox buildCardActions(RendezVous rv) {
        HBox box = new HBox(6);
        box.setAlignment(Pos.CENTER_RIGHT);
        box.setPadding(new Insets(6, 0, 0, 0));

        if (vueActive == Vue.PATIENT) {
            // Détails
            Button btnDetail = smallBtn("Détails", "#7a002f");
            btnDetail.setOnAction(e -> ouvrirDetailPatient(rv));
            box.getChildren().add(btnDetail);

            if ("en_cours".equals(rv.getStatut()) || "confirme".equals(rv.getStatut())) {
                // Modifier
                Button btnEdit = smallBtn("Modifier", "#3b82f6");
                btnEdit.setOnAction(e -> ouvrirModificationPatientView(rv));
                box.getChildren().add(btnEdit);

                // Supprimer
                Button btnDel = smallBtn("🗑", "#aaa");
                btnDel.setOnAction(e -> ouvrirConfirmationSuppression(rv));
                box.getChildren().add(btnDel);

                // Annuler
                Button btnAnnuler = smallBtn("Annuler", "#ff4b3a");
                btnAnnuler.setOnAction(e -> ouvrirConfirmationAnnulation(rv));
                box.getChildren().add(btnAnnuler);
            }

        } else {
            // Détails (vue docteur)
            Button btnDetail = smallBtn("Détails", "#7a002f");
            btnDetail.setOnAction(e -> ouvrirDetailDocteur(rv));
            box.getChildren().add(btnDetail);

            if ("en_cours".equals(rv.getStatut())) {
                Button btnConf = smallBtn("Confirmer ✓", "#38b24a");
                btnConf.setOnAction(e -> changerStatut(rv, "confirme"));
                Button btnRef  = smallBtn("Refuser ✗", "#ff4b3a");
                btnRef.setOnAction(e -> changerStatut(rv, "annule"));
                box.getChildren().addAll(btnConf, btnRef);
            }
            if ("confirme".equals(rv.getStatut())) {
                Button btnFin = smallBtn("Terminer", "#888");
                btnFin.setOnAction(e -> changerStatut(rv, "termine"));
                Button btnAnn = smallBtn("Annuler", "#ff4b3a");
                btnAnn.setOnAction(e -> changerStatut(rv, "annule"));
                box.getChildren().addAll(btnFin, btnAnn);
            }

            Button btnEdit = smallBtn("Modifier", "#3b82f6");
            btnEdit.setOnAction(e -> ouvrirModificationDocView(rv));
            box.getChildren().add(btnEdit);

            Button btnDel = smallBtn("🗑", "#aaa");
            btnDel.setOnAction(e -> ouvrirConfirmationSuppressionDoc(rv));
            box.getChildren().add(btnDel);
            if ("confirme".equals(rv.getStatut()) || "termine".equals(rv.getStatut())) {
                Button btnCR = smallBtn("📋 Compte Rendu", "#7a002f");
                btnCR.setOnAction(e -> ouvrirAjoutCompteRenduView(rv));
                box.getChildren().add(btnCR);
            }
        }

        return box;
    }


    // ══════════════════════════════════════════════════════════════════
    //  VUES DÉTAIL
    // ══════════════════════════════════════════════════════════════════

    /** Détail d'un RDV — vue Patient */
    private void ouvrirDetailPatient(RendezVous rv) {
        VBox body = new VBox(12);

        String doctorLabel = "Dr #" + rv.getMedecin_id();
        for (User d : allDoctors) {
            if (d.getId() == rv.getMedecin_id()) {
                doctorLabel = "Dr " + d.getNom() + " " + d.getPrenom();
                break;
            }
        }

        body.getChildren().addAll(
                detailRow("Médecin", doctorLabel),
                detailRow("Date",    rv.getDate_rdv() != null ? rv.getDate_rdv().format(FMT) : "—"),
                detailRow("Motif",   rv.getMotif()),
                detailRow("Lieu",    rv.getLieu()),
                detailRow("Statut",  statutLabel(rv.getStatut())),
                detailRow("Notes",   rv.getNotes() != null ? rv.getNotes() : "—")
        );

        showPopup("Détails du Rendez-Vous #" + rv.getId_rdv(), body);
    }

    /** Détail d'un RDV — vue Docteur */
    private void ouvrirDetailDocteur(RendezVous rv) {
        VBox body = new VBox(12);

        body.getChildren().addAll(
                detailRow("Patient", nomPatient(rv.getPatient_id())),
                detailRow("Date",    rv.getDate_rdv() != null ? rv.getDate_rdv().format(FMT) : "—"),
                detailRow("Motif",   rv.getMotif()),
                detailRow("Lieu",    rv.getLieu()),
                detailRow("Statut",  statutLabel(rv.getStatut())),
                detailRow("Notes",   rv.getNotes() != null ? rv.getNotes() : "—")
        );

        showPopup("Détails RDV #" + rv.getId_rdv(), body);
    }

    // ══════════════════════════════════════════════════════════════════
    //  VUES MODIFICATION
    // ══════════════════════════════════════════════════════════════════

    /** Modification limitée — vue Patient (date, heure, motif, lieu) */
    private void ouvrirModificationPatientView(RendezVous rv) {
        VBox body = new VBox(16);

        DatePicker dp = new DatePicker(rv.getDate_rdv() != null ? rv.getDate_rdv().toLocalDate() : LocalDate.now());
        dp.setStyle(fieldStyle());
        dp.setMaxWidth(Double.MAX_VALUE);

        TextField tfHeure = styledField("HH:mm");
        tfHeure.setText(rv.getDate_rdv() != null ? rv.getDate_rdv().toLocalTime().format(TIME_FMT) : "09:00");

        TextField tfMotif = styledField("Motif de consultation");
        tfMotif.setText(rv.getMotif() != null ? rv.getMotif() : "");

        TextField tfLieu = styledField("Lieu (optionnel)");
        tfLieu.setText(rv.getLieu() != null ? rv.getLieu() : "");

        Label lblErr = new Label("");
        lblErr.setStyle("-fx-text-fill:#ff4b3a;-fx-font-size:12px;");

        Button btnSave = primaryBtn("Enregistrer les modifications");
        btnSave.setOnAction(e -> {
            try {
                LocalTime heure = LocalTime.parse(tfHeure.getText().trim(), DateTimeFormatter.ofPattern("HH:mm"));
                rv.setDate_rdv(LocalDateTime.of(dp.getValue(), heure));
                rv.setMotif(tfMotif.getText().trim());
                rv.setLieu(tfLieu.getText().trim());
                rdvCRUD.modifier(rv);
                loadData();
                setStatus("Rendez-vous mis à jour.", false);
            } catch (Exception ex) {
                lblErr.setText("Format heure invalide (HH:mm) : " + ex.getMessage());
            }
        });

        body.getChildren().addAll(
                formRow("Date",   dp),
                formRow("Heure",  tfHeure),
                formRow("Motif",  tfMotif),
                formRow("Lieu",   tfLieu),
                lblErr, btnSave
        );

        showPopup("Modifier mon RDV #" + rv.getId_rdv(), body);
    }

    /** Modification complète — vue Docteur */
    private void ouvrirModificationDocView(RendezVous rv) {
        VBox body = new VBox(16);

        DatePicker dp = new DatePicker(rv.getDate_rdv() != null ? rv.getDate_rdv().toLocalDate() : LocalDate.now());
        dp.setStyle(fieldStyle());
        dp.setMaxWidth(Double.MAX_VALUE);

        TextField tfHeure = styledField("HH:mm");
        tfHeure.setText(rv.getDate_rdv() != null ? rv.getDate_rdv().toLocalTime().format(TIME_FMT) : "09:00");

        TextField tfMotif = styledField("Motif");
        tfMotif.setText(rv.getMotif() != null ? rv.getMotif() : "");

        TextField tfLieu = styledField("Lieu");
        tfLieu.setText(rv.getLieu() != null ? rv.getLieu() : "");

        ComboBox<String> cbStatut = new ComboBox<>();
        cbStatut.getItems().addAll("en_cours", "confirme", "annule", "termine");
        cbStatut.setValue(rv.getStatut());
        cbStatut.setMaxWidth(Double.MAX_VALUE);
        cbStatut.setStyle(fieldStyle());

        TextArea taNotes = new TextArea(rv.getNotes() != null ? rv.getNotes() : "");
        taNotes.setPrefRowCount(3);
        taNotes.setStyle(fieldStyle());

        Label lblErr = new Label("");
        lblErr.setStyle("-fx-text-fill:#ff4b3a;-fx-font-size:12px;");

        Button btnSave = primaryBtn("Enregistrer les modifications");
        btnSave.setOnAction(e -> {
            try {
                LocalTime heure = LocalTime.parse(tfHeure.getText().trim(), DateTimeFormatter.ofPattern("HH:mm"));
                rv.setDate_rdv(LocalDateTime.of(dp.getValue(), heure));
                rv.setMotif(tfMotif.getText().trim());
                rv.setLieu(tfLieu.getText().trim());
                rv.setNotes(taNotes.getText().trim());
                rv.setStatut(cbStatut.getValue());
                rdvCRUD.modifier(rv);
                loadData();
                setStatus("Rendez-vous mis à jour.", false);
            } catch (Exception ex) {
                lblErr.setText("Erreur : " + ex.getMessage());
            }
        });

        body.getChildren().addAll(
                formRow("Date",   dp),
                formRow("Heure",  tfHeure),
                formRow("Motif",  tfMotif),
                formRow("Lieu",   tfLieu),
                formRow("Statut", cbStatut),
                formRow("Notes",  taNotes),
                lblErr, btnSave
        );

        showPopup("Modifier RDV #" + rv.getId_rdv(), body);
    }

    // ══════════════════════════════════════════════════════════════════
    //  VUES CONFIRMATION (annulation / suppression)
    // ══════════════════════════════════════════════════════════════════

    private void ouvrirConfirmationAnnulation(RendezVous rv) {
        VBox body = new VBox(20);
        body.setAlignment(Pos.CENTER);

        // Icône
        Label icone = new Label("⚠️");
        icone.setStyle("-fx-font-size:40px;");

        Label msg = new Label(
                "Voulez-vous vraiment annuler ce rendez-vous ?\n\n" +
                        "Motif : " + (rv.getMotif() != null ? rv.getMotif() : "—") + "\n" +
                        "Date  : " + (rv.getDate_rdv() != null ? rv.getDate_rdv().format(FMT) : "—")
        );
        msg.setStyle("-fx-font-size:14px;-fx-text-fill:#333;-fx-text-alignment:center;-fx-alignment:center;");
        msg.setWrapText(true);

        HBox btns = new HBox(12);
        btns.setAlignment(Pos.CENTER);

        Button btnNon = secondaryBtn("Non, garder");
        btnNon.setOnAction(e -> fermerPopup());

        Button btnOui = new Button("Oui, annuler");
        btnOui.setStyle(
                "-fx-background-color:#ff4b3a;-fx-text-fill:white;" +
                        "-fx-font-size:13px;-fx-border-radius:8;-fx-background-radius:8;-fx-padding:10 20;-fx-cursor:hand;"
        );
        btnOui.setOnAction(e -> changerStatut(rv, "annule"));

        btns.getChildren().addAll(btnNon, btnOui);
        body.getChildren().addAll(icone, msg, btns);

        showPopup("Confirmer l'annulation", body);
    }

    private void ouvrirConfirmationSuppression(RendezVous rv) {
        VBox body = new VBox(20);
        body.setAlignment(Pos.CENTER);

        Label icone = new Label("🗑️");
        icone.setStyle("-fx-font-size:40px;");

        Label msg = new Label(
                "Voulez-vous vraiment supprimer définitivement ce rendez-vous ?\n\n" +
                        "Motif : " + (rv.getMotif() != null ? rv.getMotif() : "—") + "\n" +
                        "Date  : " + (rv.getDate_rdv() != null ? rv.getDate_rdv().format(FMT) : "—") + "\n\n" +
                        "Cette action est irréversible."
        );
        msg.setStyle("-fx-font-size:14px;-fx-text-fill:#333;-fx-text-alignment:center;-fx-alignment:center;");
        msg.setWrapText(true);

        HBox btns = new HBox(12);
        btns.setAlignment(Pos.CENTER);

        Button btnNon = secondaryBtn("Non, garder");
        btnNon.setOnAction(e -> fermerPopup());

        Button btnOui = new Button("Oui, supprimer");
        btnOui.setStyle(
                "-fx-background-color:#ff4b3a;-fx-text-fill:white;" +
                        "-fx-font-size:13px;-fx-border-radius:8;-fx-background-radius:8;-fx-padding:10 20;-fx-cursor:hand;"
        );
        btnOui.setOnAction(e -> {
            supprimerRdv(rv);
            loadData();
        });

        btns.getChildren().addAll(btnNon, btnOui);
        body.getChildren().addAll(icone, msg, btns);

        showPopup("Confirmer la suppression", body);
    }

    private void ouvrirConfirmationSuppressionDoc(RendezVous rv) {
        // Même vue que patient mais accessible depuis la vue docteur
        ouvrirConfirmationSuppression(rv);
    }

    // ══════════════════════════════════════════════════════════════════
    //  NOUVEAU RDV (vue Patient)
    // ══════════════════════════════════════════════════════════════════

    @FXML
    private void ouvrirAjoutPopup() {
        selectedDoctor  = null;
        selectedDate    = null;
        selectedCreneau = null;

        VBox body = new VBox(16);

        // ── Étape 1 : Médecin ─────────────────────────────────────────
        ComboBox<User> cbMedecin = new ComboBox<>();
        cbMedecin.setMaxWidth(Double.MAX_VALUE);
        cbMedecin.setStyle(fieldStyle());
        cbMedecin.setPromptText("Sélectionner un médecin…");
        cbMedecin.getItems().addAll(allDoctors);
        cbMedecin.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(User u, boolean empty) {
                super.updateItem(u, empty);
                if (empty || u == null) { setText(null); return; }
                String dep = "";
                try { dep = u.getDepartement() != null ? " — " + u.getDepartement() : ""; }
                catch (Exception ignored) {}
                setText("Dr " + u.getNom() + " " + u.getPrenom() + dep);
            }
        });
        cbMedecin.setButtonCell(cbMedecin.getCellFactory().call(null));

        // ── Étape 2 : Calendrier ──────────────────────────────────────
        VBox calZone  = new VBox(8);
        calZone.setStyle("-fx-opacity:0.4;");

        // ── Étape 3 : Créneaux ────────────────────────────────────────
        VBox slotZone = new VBox(6);
        slotZone.setStyle("-fx-opacity:0.4;");

        // ── Étape 4 : Infos ───────────────────────────────────────────
        TextField tfMotif = styledField("Motif de consultation");
        TextField tfLieu  = styledField("Lieu (optionnel)");
        TextArea  taNotes = new TextArea();
        taNotes.setPromptText("Notes…");
        taNotes.setPrefRowCount(2);
        taNotes.setStyle(fieldStyle());

        Label lblErr = new Label("");
        lblErr.setStyle("-fx-text-fill:#ff4b3a;-fx-font-size:12px;");

        Button btnSave = primaryBtn("Confirmer le rendez-vous");
        btnSave.setDisable(true);
        btnSave.setStyle(btnSave.getStyle() + "-fx-opacity:0.5;");

        // Helpers lambdas
        final YearMonth[] popupMonth   = { YearMonth.now() };
        final Label[]     lblMois      = { new Label() };
        lblMois[0].setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#7a002f;");

        Runnable[] fillSlots   = { null };
        Runnable[] fillPopupCal = { null };

        fillSlots[0] = () -> {
            slotZone.getChildren().clear();
            if (selectedDoctor == null || selectedDate == null) {
                slotZone.setStyle("-fx-opacity:0.4;"); return;
            }
            try {
                List<Disponibilite> dispos = dispoCRUD.creneauxLibres(selectedDoctor.getId())
                        .stream().filter(d -> d.getDate_dispo().equals(selectedDate))
                        .collect(Collectors.toList());

                if (dispos.isEmpty()) {
                    Label none = new Label("Aucun créneau libre ce jour.");
                    none.setStyle("-fx-font-size:12px;-fx-text-fill:#aaa;");
                    slotZone.getChildren().add(none);
                } else {
                    slotZone.setStyle("");
                    for (Disponibilite d : dispos) {
                        Button btnSlot = new Button(d.getHeure_debut().format(TIME_FMT) + " – " + d.getHeure_fin().format(TIME_FMT));
                        btnSlot.setMaxWidth(Double.MAX_VALUE);
                        styleSlotBtn(btnSlot, false);
                        btnSlot.setOnAction(ev -> {
                            selectedCreneau = d;
                            slotZone.getChildren().forEach(n -> { if (n instanceof Button b) styleSlotBtn(b, false); });
                            styleSlotBtn(btnSlot, true);
                            btnSave.setDisable(false);
                            btnSave.setStyle(btnSave.getStyle().replace("-fx-opacity:0.5;", ""));
                        });
                        slotZone.getChildren().add(btnSlot);
                    }
                }
            } catch (SQLException ex) { lblErr.setText("Erreur créneaux : " + ex.getMessage()); }
        };

        fillPopupCal[0] = () -> {
            calZone.getChildren().clear();
            if (selectedDoctor == null) { calZone.setStyle("-fx-opacity:0.4;"); return; }
            calZone.setStyle("");
            try {
                Set<LocalDate> freeDates = dispoCRUD.creneauxLibres(selectedDoctor.getId())
                        .stream().map(Disponibilite::getDate_dispo).collect(Collectors.toSet());

                Button bPrev = new Button("‹"); styleNavBtn(bPrev);
                Button bNext = new Button("›"); styleNavBtn(bNext);
                lblMois[0].setText(popupMonth[0].getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH) + " " + popupMonth[0].getYear());

                bPrev.setOnAction(e -> { popupMonth[0] = popupMonth[0].minusMonths(1); fillPopupCal[0].run(); });
                bNext.setOnAction(e -> { popupMonth[0] = popupMonth[0].plusMonths(1);  fillPopupCal[0].run(); });

                HBox nav = new HBox(10, bPrev, lblMois[0], bNext);
                nav.setAlignment(Pos.CENTER);

                GridPane mini = new GridPane();
                mini.setHgap(4); mini.setVgap(4);
                for (int i = 0; i < 7; i++) {
                    ColumnConstraints cc = new ColumnConstraints();
                    cc.setPercentWidth(100.0 / 7);
                    cc.setHgrow(Priority.ALWAYS);
                    mini.getColumnConstraints().add(cc);
                }
                String[] dNames = {"L","M","M","J","V","S","D"};
                for (int i = 0; i < 7; i++) {
                    Label h = new Label(dNames[i]);
                    h.setStyle("-fx-font-size:10px;-fx-text-fill:#aaa;-fx-alignment:center;");
                    h.setMaxWidth(Double.MAX_VALUE);
                    mini.add(h, i, 0);
                }

                int startCol = popupMonth[0].atDay(1).getDayOfWeek().getValue() - 1;
                int daysInM  = popupMonth[0].lengthOfMonth();
                int cRow = 1, cCol = startCol;

                for (int day = 1; day <= daysInM; day++) {
                    LocalDate date   = popupMonth[0].atDay(day);
                    boolean   isFree = freeDates.contains(date);
                    boolean   isPast = date.isBefore(LocalDate.now());
                    boolean   isSel  = date.equals(selectedDate);

                    Button dayBtn = new Button(String.valueOf(day));
                    dayBtn.setMaxWidth(Double.MAX_VALUE);
                    dayBtn.setMaxHeight(Double.MAX_VALUE);

                    String st;
                    if (isSel) {
                        st = "-fx-background-color:#7a002f;-fx-text-fill:white;-fx-border-radius:6;-fx-background-radius:6;-fx-font-size:12px;-fx-cursor:hand;";
                    } else if (!isFree || isPast) {
                        st = "-fx-background-color:#f5f5f5;-fx-text-fill:#ccc;-fx-border-radius:6;-fx-background-radius:6;-fx-font-size:12px;";
                    } else {
                        st = "-fx-background-color:#f0fdf4;-fx-text-fill:#38b24a;-fx-border-color:#38b24a;-fx-border-width:1;-fx-border-radius:6;-fx-background-radius:6;-fx-font-size:12px;-fx-cursor:hand;";
                    }
                    dayBtn.setStyle(st);
                    dayBtn.setDisable(!isFree || isPast);

                    final LocalDate fd = date;
                    dayBtn.setOnAction(ev -> {
                        selectedDate    = fd;
                        selectedCreneau = null;
                        btnSave.setDisable(true);
                        btnSave.setStyle(btnSave.getStyle().replace("-fx-opacity:0.5;","") + "-fx-opacity:0.5;");
                        fillPopupCal[0].run();
                        fillSlots[0].run();
                    });
                    mini.add(dayBtn, cCol, cRow);
                    cCol++; if (cCol >= 7) { cCol = 0; cRow++; }
                }
                calZone.getChildren().addAll(nav, mini);
            } catch (SQLException ex) { lblErr.setText("Erreur calendrier : " + ex.getMessage()); }
        };

        cbMedecin.valueProperty().addListener((o, old, doc) -> {
            selectedDoctor  = doc;
            selectedDate    = null;
            selectedCreneau = null;
            popupMonth[0]   = YearMonth.now();
            fillPopupCal[0].run();
            slotZone.getChildren().clear();
            slotZone.setStyle("-fx-opacity:0.4;");
            btnSave.setDisable(true);
        });

        btnSave.setOnAction(ev -> {
            lblErr.setText("");
            if (selectedDoctor  == null) { lblErr.setText("Choisissez un médecin.");  return; }
            if (selectedCreneau == null) { lblErr.setText("Choisissez un créneau."); return; }
            try {
                LocalDateTime dateRdv = LocalDateTime.of(selectedCreneau.getDate_dispo(), selectedCreneau.getHeure_debut());
                RendezVous rv = new RendezVous(currentPatientId, selectedDoctor.getId(), dateRdv,
                        tfMotif.getText().trim(), "en_cours", tfLieu.getText().trim(), taNotes.getText().trim());
                rdvCRUD.ajouter(rv);
                loadData();
                setStatus("Rendez-vous créé — en cours.", false);
            } catch (Exception ex) {
                lblErr.setText("Erreur : " + ex.getMessage());
            }
        });

        body.getChildren().addAll(
                sectionLabel("1. Choisir un médecin"), formRow("Médecin", cbMedecin),
                sectionLabel("2. Choisir une date disponible"), calZone,
                sectionLabel("3. Choisir un créneau horaire"), slotZone,
                sectionLabel("4. Informations complémentaires"),
                formRow("Motif", tfMotif),
                formRow("Lieu",  tfLieu),
                formRow("Notes", taNotes),
                lblErr, btnSave
        );

        showPopup("Nouveau Rendez-Vous", body);
    }

    // ══════════════════════════════════════════════════════════════════
    //  CALENDRIER DISPONIBILITÉS (vue Docteur)
    // ══════════════════════════════════════════════════════════════════

    private YearMonth calendarMonth = YearMonth.now();

    private void buildDispoCalendarSection(int startRow) {
        try {
            List<Disponibilite> dispos = dispoCRUD.parMedecin(currentMedecinId);
            VBox calendarBox = buildDispoCalendar(dispos);
            gridCards.add(calendarBox, 0, startRow, 2, 1);

            Button btnAddDispo = new Button("+ Ajouter un créneau");
            btnAddDispo.setStyle(
                    "-fx-background-color:#7a002f;-fx-text-fill:white;" +
                            "-fx-font-size:13px;-fx-border-radius:8;-fx-background-radius:8;" +
                            "-fx-padding:10 20;-fx-cursor:hand;"
            );
            btnAddDispo.setOnAction(e -> ouvrirAjoutDispoView());
            gridCards.add(btnAddDispo, 0, startRow + 1, 2, 1);
        } catch (SQLException e) {
            setStatus("Erreur disponibilités : " + e.getMessage(), true);
        }
    }

    private VBox buildDispoCalendar(List<Disponibilite> dispos) {
        VBox root = new VBox(10);
        root.setStyle(
                "-fx-background-color:white;-fx-border-color:#e0e0e0;" +
                        "-fx-border-radius:14;-fx-background-radius:14;-fx-padding:18;"
        );

        Button btnPrev = new Button("‹");
        Button btnNext = new Button("›");
        styleNavBtn(btnPrev); styleNavBtn(btnNext);

        Label lblMonth = new Label();
        lblMonth.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:#7a002f;");

        HBox navBar = new HBox(10, btnPrev, lblMonth, btnNext);
        navBar.setAlignment(Pos.CENTER);

        HBox legend = new HBox(16);
        legend.setAlignment(Pos.CENTER);
        legend.getChildren().addAll(
                legendDot("#38b24a", "Libre"),
                legendDot("#ff4b3a", "Occupé"),
                legendDot("#f59e0b", "Bloqué")
        );

        GridPane calGrid = new GridPane();
        calGrid.setHgap(6); calGrid.setVgap(6);
        for (int i = 0; i < 7; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / 7);
            cc.setHgrow(Priority.ALWAYS);
            calGrid.getColumnConstraints().add(cc);
        }

        Map<LocalDate, List<Disponibilite>> byDate = dispos.stream()
                .collect(Collectors.groupingBy(Disponibilite::getDate_dispo));

        Runnable fillCalendar = () -> {
            calGrid.getChildren().clear();
            lblMonth.setText(
                    calendarMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH) + " " + calendarMonth.getYear()
            );
            String[] days = {"Lun","Mar","Mer","Jeu","Ven","Sam","Dim"};
            for (int i = 0; i < 7; i++) {
                Label d = new Label(days[i]);
                d.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:#999;-fx-alignment:center;");
                d.setMaxWidth(Double.MAX_VALUE);
                calGrid.add(d, i, 0);
            }
            int startCol = calendarMonth.atDay(1).getDayOfWeek().getValue() - 1;
            int daysInMonth = calendarMonth.lengthOfMonth();
            int cellRow = 1, cellCol = startCol;
            for (int day = 1; day <= daysInMonth; day++) {
                LocalDate date = calendarMonth.atDay(day);
                List<Disponibilite> dayDispos = byDate.getOrDefault(date, Collections.emptyList());
                calGrid.add(buildDayCell(date, dayDispos), cellCol, cellRow);
                cellCol++;
                if (cellCol >= 7) { cellCol = 0; cellRow++; }
            }
        };

        fillCalendar.run();
        btnPrev.setOnAction(e -> { calendarMonth = calendarMonth.minusMonths(1); fillCalendar.run(); });
        btnNext.setOnAction(e -> { calendarMonth = calendarMonth.plusMonths(1);  fillCalendar.run(); });

        root.getChildren().addAll(navBar, legend, calGrid);
        return root;
    }

    private VBox buildDayCell(LocalDate date, List<Disponibilite> dispos) {
        VBox cell = new VBox(2);
        cell.setAlignment(Pos.TOP_CENTER);
        cell.setMinHeight(54);
        cell.setPadding(new Insets(4));

        boolean isToday  = date.equals(LocalDate.now());
        boolean isPast   = date.isBefore(LocalDate.now());
        boolean hasLibre = dispos.stream().anyMatch(d -> "libre".equals(d.getStatut()));
        boolean hasOcc   = dispos.stream().anyMatch(d -> "occupee".equals(d.getStatut()));

        String bg, border;
        if (dispos.isEmpty()) {
            bg = isPast ? "#f9f9f9" : "white"; border = "#ececec";
        } else if (hasLibre && !hasOcc) {
            bg = "#f0fdf4"; border = "#38b24a";
        } else if (hasOcc && !hasLibre) {
            bg = "#fff5f5"; border = "#ff4b3a";
        } else {
            bg = "#fffbeb"; border = "#f59e0b";
        }

        cell.setStyle(
                "-fx-background-color:" + bg + ";-fx-border-color:" + border + ";" +
                        "-fx-border-radius:8;-fx-background-radius:8;" +
                        (isToday ? "-fx-border-width:2;" : "-fx-border-width:1;") +
                        "-fx-cursor:" + (dispos.isEmpty() ? "default" : "hand") + ";"
        );

        Label lblDay = new Label(String.valueOf(date.getDayOfMonth()));
        lblDay.setStyle("-fx-font-size:13px;" + (isToday ? "-fx-font-weight:bold;-fx-text-fill:#7a002f;" : "-fx-text-fill:#333;"));
        cell.getChildren().add(lblDay);

        if (!dispos.isEmpty()) {
            HBox dots = new HBox(3);
            dots.setAlignment(Pos.CENTER);
            for (Disponibilite d : dispos) {
                String dotColor = switch (d.getStatut()) {
                    case "libre"   -> "#38b24a";
                    case "occupee" -> "#ff4b3a";
                    case "bloquee" -> "#f59e0b";
                    default        -> "#aaa";
                };
                javafx.scene.shape.Circle dot = new javafx.scene.shape.Circle(3);
                dot.setStyle("-fx-fill:" + dotColor + ";");
                dots.getChildren().add(dot);
            }
            cell.getChildren().add(dots);
            cell.setOnMouseClicked(e -> ouvrirDetailJourDispoView(date, dispos));
        }
        return cell;
    }

    /** Détail des créneaux d'un jour — s'affiche dans la zone centrale */
    private void ouvrirDetailJourDispoView(LocalDate date, List<Disponibilite> dispos) {
        VBox body = new VBox(10);

        for (Disponibilite d : dispos) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle(
                    "-fx-background-color:" + dispoColors(d.getStatut())[1] + ";" +
                            "-fx-border-color:" + dispoColors(d.getStatut())[0] + ";" +
                            "-fx-border-radius:8;-fx-background-radius:8;-fx-padding:10 14;"
            );

            Label lblHeure = new Label(d.getHeure_debut().format(TIME_FMT) + " – " + d.getHeure_fin().format(TIME_FMT));
            lblHeure.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#333;");

            Label lblSt = new Label(dispoStatutLabel(d.getStatut()));
            lblSt.setStyle("-fx-font-size:11px;-fx-text-fill:" + dispoColors(d.getStatut())[0] + ";-fx-font-weight:bold;");

            Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);

            HBox actBtns = new HBox(6);
            if ("libre".equals(d.getStatut())) {
                Button b = smallBtn("Bloquer", "#f59e0b");
                b.setOnAction(ev -> { modifierStatutDispo(d, "bloquee"); loadData(); });
                actBtns.getChildren().add(b);
            } else if ("bloquee".equals(d.getStatut())) {
                Button b = smallBtn("Libérer", "#38b24a");
                b.setOnAction(ev -> { modifierStatutDispo(d, "libre"); loadData(); });
                actBtns.getChildren().add(b);
            }
            Button btnDel = smallBtn("🗑", "#ddd");
            btnDel.setOnAction(ev -> { supprimerDispo(d); loadData(); });
            actBtns.getChildren().add(btnDel);

            row.getChildren().addAll(lblHeure, lblSt, sp, actBtns);
            body.getChildren().add(row);
        }

        showPopup("Créneaux du " + date.format(DATE_FMT), body);
    }

    /** Formulaire ajout disponibilité */
    private void ouvrirAjoutDispoView() {
        VBox body = new VBox(16);

        DatePicker dp = new DatePicker(LocalDate.now());
        dp.setStyle(fieldStyle());
        dp.setMaxWidth(Double.MAX_VALUE);

        TextField tfDebut = styledField("Début (HH:mm)");
        tfDebut.setText("08:00");
        TextField tfFin = styledField("Fin (HH:mm)");
        tfFin.setText("12:00");

        Label lblErr = new Label("");
        lblErr.setStyle("-fx-text-fill:#ff4b3a;-fx-font-size:12px;");

        Button btnSave = primaryBtn("Ajouter le créneau");
        btnSave.setOnAction(e -> {
            try {
                LocalTime debut = LocalTime.parse(tfDebut.getText().trim(), DateTimeFormatter.ofPattern("HH:mm"));
                LocalTime fin   = LocalTime.parse(tfFin.getText().trim(),   DateTimeFormatter.ofPattern("HH:mm"));
                if (!fin.isAfter(debut)) { lblErr.setText("L'heure de fin doit être après l'heure de début."); return; }
                dispoCRUD.ajouter(new Disponibilite(currentMedecinId, dp.getValue(), debut, fin, "libre"));
                loadData();
                setStatus("Créneau ajouté avec succès.", false);
            } catch (Exception ex) {
                lblErr.setText("Erreur : " + ex.getMessage());
            }
        });

        body.getChildren().addAll(
                formRow("Date",        dp),
                formRow("Heure début", tfDebut),
                formRow("Heure fin",   tfFin),
                lblErr, btnSave
        );

        showPopup("Nouveau Créneau de Disponibilité", body);
    }

    // ── Actions métier ────────────────────────────────────────────────
    private void changerStatut(RendezVous rv, String statut) {
        try {
            rdvCRUD.changerStatut(rv.getId_rdv(), statut);
            loadData();
            setStatus("Statut mis à jour : " + statutLabel(statut), false);
        } catch (SQLException e) {
            setStatus("Erreur : " + e.getMessage(), true);
        }
    }

    private void modifierStatutDispo(Disponibilite d, String statut) {
        try {
            dispoCRUD.modifierStatut(d.getId_dispo(), statut, d.getId_rdv());
        } catch (SQLException e) {
            setStatus("Erreur dispo : " + e.getMessage(), true);
        }
    }

    private void supprimerDispo(Disponibilite d) {
        try {
            dispoCRUD.supprimer(d.getId_dispo());
        } catch (SQLException e) {
            setStatus("Erreur suppression dispo : " + e.getMessage(), true);
        }
    }

    private void supprimerRdv(RendezVous rv) {
        try {
            rdvCRUD.supprimer(rv.getId_rdv());
            setStatus("Rendez-vous supprimé.", false);
        } catch (SQLException e) {
            setStatus("Erreur suppression : " + e.getMessage(), true);
        }
    }

    // ── Navigation sidebar ────────────────────────────────────────────
    @FXML
    private void ouvrirCompteRendus() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ConsulterCompteRendu.fxml"));
            BorderPane fullPane = loader.load();
            AfficherCompteRenduController ctrl = loader.getController();
            ctrl.setParentController(this);
            javafx.scene.Node contenu = fullPane.getCenter();
            fullPane.setCenter(null); // détacher du parent

            mainPane.setCenter(contenu); // injecter dans le center de RendezVous

        } catch (Exception e) {
            setStatus("Erreur : " + e.getMessage(), true);
        }
    }
    // ── Helpers UI ────────────────────────────────────────────────────
    private String fieldStyle() {
        return "-fx-font-size:13px;-fx-background-color:white;" +
                "-fx-border-color:#e5b7c4;-fx-border-radius:8;-fx-background-radius:8;" +
                "-fx-padding:8 10;";
    }

    private TextField styledField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setStyle(fieldStyle());
        tf.setMaxWidth(Double.MAX_VALUE);
        return tf;
    }

    private VBox formRow(String label, javafx.scene.Node field) {
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#555;");
        VBox row = new VBox(4, lbl, field);
        return row;
    }

    private HBox detailRow(String label, String value) {
        Label lbl = new Label(label + " :");
        lbl.setStyle("-fx-font-weight:bold;-fx-font-size:13px;-fx-text-fill:#7a002f;-fx-min-width:90;");
        Label val = new Label(value != null ? value : "—");
        val.setStyle("-fx-font-size:13px;-fx-text-fill:#333;");
        val.setWrapText(true);
        HBox row = new HBox(10, lbl, val);
        row.setStyle(
                "-fx-background-color:#fafafa;-fx-border-color:#f0e6ea;" +
                        "-fx-border-radius:6;-fx-background-radius:6;-fx-padding:8 12;"
        );
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Button smallBtn(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle(
                "-fx-background-color:" + color + ";-fx-text-fill:white;" +
                        "-fx-font-size:10px;-fx-border-radius:6;-fx-background-radius:6;" +
                        "-fx-padding:4 10;-fx-cursor:hand;"
        );
        return btn;
    }

    private Button primaryBtn(String text) {
        Button btn = new Button(text);
        btn.setStyle(
                "-fx-background-color:#7a002f;-fx-text-fill:white;" +
                        "-fx-font-size:13px;-fx-font-weight:bold;" +
                        "-fx-border-radius:8;-fx-background-radius:8;-fx-padding:11 22;-fx-cursor:hand;"
        );
        btn.setMaxWidth(Double.MAX_VALUE);
        return btn;
    }

    private Button secondaryBtn(String text) {
        Button btn = new Button(text);
        btn.setStyle(
                "-fx-background-color:#eee;-fx-text-fill:#333;" +
                        "-fx-font-size:13px;-fx-border-radius:8;-fx-background-radius:8;" +
                        "-fx-padding:10 20;-fx-cursor:hand;"
        );
        return btn;
    }

    private Label sectionLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#7a002f;-fx-padding:4 0 2 0;");
        return l;
    }

    private void styleNavBtn(Button b) {
        b.setStyle(
                "-fx-background-color:#f0e6ea;-fx-text-fill:#7a002f;" +
                        "-fx-font-size:16px;-fx-border-radius:6;-fx-background-radius:6;" +
                        "-fx-padding:2 10;-fx-cursor:hand;"
        );
    }

    private void styleSlotBtn(Button b, boolean selected) {
        if (selected) {
            b.setStyle(
                    "-fx-background-color:#7a002f;-fx-text-fill:white;" +
                            "-fx-border-radius:8;-fx-background-radius:8;" +
                            "-fx-font-size:13px;-fx-padding:8 14;-fx-cursor:hand;-fx-max-width:infinity;"
            );
        } else {
            b.setStyle(
                    "-fx-background-color:#f0fdf4;-fx-text-fill:#38b24a;" +
                            "-fx-border-color:#38b24a;-fx-border-width:1;" +
                            "-fx-border-radius:8;-fx-background-radius:8;" +
                            "-fx-font-size:13px;-fx-padding:8 14;-fx-cursor:hand;-fx-max-width:infinity;"
            );
        }
    }

    private HBox legendDot(String color, String label) {
        javafx.scene.shape.Circle dot = new javafx.scene.shape.Circle(5);
        dot.setStyle("-fx-fill:" + color + ";");
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size:11px;-fx-text-fill:#555;");
        return new HBox(5, dot, lbl);
    }

    private void setStatus(String msg, boolean error) {
        lblStatus.setText(msg);
        lblStatus.setStyle(error
                ? "-fx-text-fill:#ff4b3a;-fx-font-weight:bold;"
                : "-fx-text-fill:#38b24a;-fx-font-weight:bold;"
        );
    }

    // ── Couleurs ──────────────────────────────────────────────────────
    private String[] statutColors(String statut) {
        return switch (statut == null ? "" : statut) {
            case "en_cours" -> new String[]{"#3b82f6", "#eff6ff", "#dbeafe"};
            case "confirme" -> new String[]{"#38b24a", "#f0fdf4", "#dcfce7"};
            case "annule"   -> new String[]{"#ff4b3a", "#fff5f5", "#ffe4e4"};
            case "termine"  -> new String[]{"#888888", "#f5f5f5", "#ebebeb"};
            default         -> new String[]{"#f59e0b", "#fffbeb", "#fef3c7"};
        };
    }

    private String[] dispoColors(String statut) {
        return switch (statut == null ? "" : statut) {
            case "libre"   -> new String[]{"#38b24a", "#f0fdf4"};
            case "occupee" -> new String[]{"#ff4b3a", "#fff5f5"};
            case "bloquee" -> new String[]{"#f59e0b", "#fffbeb"};
            default        -> new String[]{"#888", "#f5f5f5"};
        };
    }

    private String statutLabel(String s) {
        return switch (s == null ? "" : s) {
            case "en_cours" -> "En cours";
            case "confirme" -> "Confirmé";
            case "annule"   -> "Annulé";
            case "termine"  -> "Terminé";
            default         -> s;
        };
    }

    private String dispoStatutLabel(String s) {
        return switch (s == null ? "" : s) {
            case "libre"   -> "● Libre";
            case "occupee" -> "● Occupé";
            case "bloquee" -> "● Bloqué";
            default        -> s;
        };
    }
}