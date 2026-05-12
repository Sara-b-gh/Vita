package Controlers;

import Entites.CompteRendu;
import Entites.Disponibilite;
import Entites.RendezVous;
import Entites.User;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import services.*;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.awt.Desktop;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class RendezVousController implements Initializable {

    @FXML private BorderPane       mainPane;
    @FXML private TextField        searchField;
    @FXML private ComboBox<String> filterStatut;
    @FXML private Label            lblTotal, lblStatus;
    @FXML private ScrollPane       scrollCenter;
    @FXML private ScrollPane       scrollDetail;
    @FXML private VBox             detailContent;
    @FXML private GridPane         gridCards;

    private static final DateTimeFormatter FMT     = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final CompteRenduCRUD   compteRenduCRUD;

    {
        try {
            compteRenduCRUD = new CompteRenduCRUD();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private final RendezVousCRUD    rdvCRUD         = new RendezVousCRUD();
    private final DisponibiliteCRUD dispoCRUD       = new DisponibiliteCRUD();
    private final UserService       userService     = new UserService();
    // EmailService is instantiated normally; its sendEmail() is an instance method
    private final EmailService emailService = new EmailService();

    private List<RendezVous> allRdvs;
    private List<User>       allDoctors = new ArrayList<>();

    private enum Vue { PATIENT, DOCTEUR }
    private Vue vueActive = Vue.PATIENT;

    private final int currentPatientId = 1;
    private final int currentMedecinId = 66;

    private User          selectedDoctor  = null;
    private LocalDate     selectedDate    = null;
    private Disponibilite selectedCreneau = null;
    private YearMonth     calendarMonth   = YearMonth.now();
    /** Held as a field so slot buttons can enable it without carrying it as a parameter. */
    private Button        confirmBtn      = null;

    // ─────────────────────────────────────────────────────────────────
    //  HELPERS NOMS
    // ─────────────────────────────────────────────────────────────────

    private String nomPatient(int id) {
        User u = userService.findById(id);
        return u != null ? u.getPrenom() + " " + u.getNom() : "Patient n°" + id;
    }

    private String doctorLabel(User d) {
        String dep = (d.getDepartment() != null && !d.getDepartment().isBlank())
                ? " — " + d.getDepartment() : "";
        return "Dr " + d.getNom() + " " + d.getPrenom() + dep;
    }

    // ─────────────────────────────────────────────────────────────────
    //  INITIALIZE
    // ─────────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        filterStatut.getItems().addAll("Tous", "en_cours", "confirme", "annule", "termine");
        filterStatut.setValue("Tous");

        searchField.textProperty().addListener((o, old, n) -> applyFilters());
        filterStatut.valueProperty().addListener((o, old, n) -> applyFilters());

        // FIX: removed try/catch for SQLException — getDoctors() doesn't throw it here;
        // catch Exception broadly, or adjust to match the actual checked exception your service throws.
        try {
            allDoctors = userService.getDoctors();
            if (allDoctors.isEmpty()) {
                System.out.println("[WARN] Aucun médecin chargé depuis la base.");
            }
        } catch (Exception e) {
            System.out.println("Impossible de charger les médecins : " + e.getMessage());
        }

        loadData();
    }

    // ─────────────────────────────────────────────────────────────────
    //  GOOGLE MAPS
    // ─────────────────────────────────────────────────────────────────

    private void ouvrirGoogleMaps(String lieu) {
        try {
            String urlStr = "https://www.google.com/maps/search/?q="
                    + URLEncoder.encode(lieu, StandardCharsets.UTF_8);
            Desktop.getDesktop().browse(new URI(urlStr));
        } catch (Exception ex) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Erreur");
            alert.setHeaderText(null);
            alert.setContentText("Impossible d'ouvrir Google Maps : " + ex.getMessage());
            alert.showAndWait();
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  MÉTÉO — Open-Meteo (gratuit, sans clé API)
    // ─────────────────────────────────────────────────────────────────

    private void afficherMeteo(LocalDate date, String lieu, VBox body) {
        try {
            String ville = (lieu != null && !lieu.isBlank())
                    ? lieu.split(",")[lieu.split(",").length - 1].trim()
                    : "Tunis";

            // FIX: getCoordinates now returns nomVille as well (see method below)
            String[] coordsAndName = getCoordinatesAndName(ville);
            double lat      = Double.parseDouble(coordsAndName[0]);
            double lon      = Double.parseDouble(coordsAndName[1]);
            String nomVille = coordsAndName[2];

            WeatherData weather = getWeatherData(lat, lon, date);

            VBox card = buildWeatherCard(nomVille, date, weather);
            card.setOpacity(0);
            body.getChildren().add(card);

            FadeTransition ft = new FadeTransition(Duration.millis(400), card);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();

        } catch (Exception ex) {
            HBox errRow = new HBox(8);
            errRow.setAlignment(Pos.CENTER_LEFT);
            errRow.setStyle("-fx-background-color:#fff8f0;-fx-border-color:#f59e0b;"
                    + "-fx-border-width:1;-fx-border-radius:10;-fx-background-radius:10;-fx-padding:10 14;");
            errRow.getChildren().add(new Label("⚠  Météo indisponible pour cette date ou ce lieu."));
            body.getChildren().add(errRow);
        }
    }

    /**
     * FIX: Renamed from getCoordinates() and returns [lat, lon, nomVille] as String[].
     * The original method discarded the extracted city name; now it is returned and used.
     */
    private String[] getCoordinatesAndName(String ville) throws Exception {
        String geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name="
                + URLEncoder.encode(ville, StandardCharsets.UTF_8)
                + "&count=1&language=fr";

        HttpURLConnection geoConn = (HttpURLConnection) new URL(geoUrl).openConnection();
        geoConn.setConnectTimeout(5000);
        geoConn.setReadTimeout(5000);
        String geoJson = new String(geoConn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        double lat      = extraireDouble(geoJson, "\"latitude\":");
        double lon      = extraireDouble(geoJson, "\"longitude\":");
        String nomVille = extraireCityName(geoJson);
        if (nomVille.isBlank()) nomVille = ville;

        return new String[]{String.valueOf(lat), String.valueOf(lon), nomVille};
    }

    private WeatherData getWeatherData(double lat, double lon, LocalDate date) throws Exception {
        String dateStr  = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String meteoUrl = "https://api.open-meteo.com/v1/forecast"
                + "?latitude="  + lat
                + "&longitude=" + lon
                + "&daily=temperature_2m_max,temperature_2m_min,precipitation_sum,"
                + "weathercode,windspeed_10m_max,uv_index_max"
                + "&timezone=auto"
                + "&start_date=" + dateStr
                + "&end_date="   + dateStr;

        HttpURLConnection meteoConn = (HttpURLConnection) new URL(meteoUrl).openConnection();
        meteoConn.setConnectTimeout(5000);
        meteoConn.setReadTimeout(5000);
        String meteoJson = new String(meteoConn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        WeatherData weather = new WeatherData();
        weather.tempMax = extraireDoubleArray(meteoJson, "\"temperature_2m_max\":[");
        weather.tempMin = extraireDoubleArray(meteoJson, "\"temperature_2m_min\":[");
        weather.pluie   = extraireDoubleArray(meteoJson, "\"precipitation_sum\":[");
        weather.wcode   = (int) extraireDoubleArray(meteoJson, "\"weathercode\":[");
        weather.vent    = extraireDoubleArray(meteoJson, "\"windspeed_10m_max\":[");
        weather.uv      = extraireDoubleArray(meteoJson, "\"uv_index_max\":[");

        return weather;
    }

    private static class WeatherData {
        double tempMax, tempMin, pluie, vent, uv;
        int    wcode;
    }

    private VBox buildWeatherCard(String nomVille, LocalDate date, WeatherData w) {
        String[] gradient = weatherGradient(w.wcode);
        String bgTop = gradient[0], bgBot = gradient[1];

        VBox card = new VBox(0);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setStyle("-fx-background-color:linear-gradient(to bottom," + bgTop + "," + bgBot + ");"
                + "-fx-background-radius:20;-fx-border-radius:20;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.18),16,0,0,4);");

        VBox   header     = createWeatherHeader(nomVille, date);
        HBox   centre     = createWeatherCentre(w);
        Region sep        = createSeparator();
        VBox   rangeBlock = createTemperatureRange(w.tempMin, w.tempMax);
        HBox   stats      = createWeatherStats(w.pluie, w.vent, w.uv);
        VBox   conseilBox = createWeatherConseil(w.wcode, w.pluie);

        card.getChildren().addAll(header, centre, sep, rangeBlock, stats);
        if (conseilBox != null) card.getChildren().add(conseilBox);

        return card;
    }

    private VBox createWeatherHeader(String nomVille, LocalDate date) {
        VBox header = new VBox(2);
        header.setPadding(new Insets(20, 20, 12, 20));

        Label lblVille = new Label(nomVille);
        lblVille.setStyle("-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:white;");

        String jourFormate = date.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH));
        jourFormate = jourFormate.substring(0, 1).toUpperCase() + jourFormate.substring(1);
        Label lblDateFmt = new Label(jourFormate);
        lblDateFmt.setStyle("-fx-font-size:12px;-fx-text-fill:rgba(255,255,255,0.78);");

        header.getChildren().addAll(lblVille, lblDateFmt);
        return header;
    }

    private HBox createWeatherCentre(WeatherData w) {
        HBox centre = new HBox(0);
        centre.setAlignment(Pos.CENTER_LEFT);
        centre.setPadding(new Insets(0, 20, 0, 20));

        Label lblIcone = new Label(weatherEmoji(w.wcode));
        lblIcone.setStyle("-fx-font-size:64px;");

        VBox tempBlock = new VBox(0);
        tempBlock.setPadding(new Insets(0, 0, 0, 14));
        tempBlock.setAlignment(Pos.CENTER_LEFT);

        double tempMoy = (w.tempMax + w.tempMin) / 2.0;
        Label lblTemp  = new Label(String.format("%.0f°", tempMoy));
        lblTemp.setStyle("-fx-font-size:58px;-fx-font-weight:bold;-fx-text-fill:white;-fx-label-padding:-8 0 0 0;");

        Label lblDesc = new Label(weatherDesc(w.wcode));
        lblDesc.setStyle("-fx-font-size:15px;-fx-text-fill:rgba(255,255,255,0.88);");

        Label lblMinMax = new Label(String.format("%.0f° / %.0f°", w.tempMin, w.tempMax));
        lblMinMax.setStyle("-fx-font-size:13px;-fx-text-fill:rgba(255,255,255,0.70);");

        tempBlock.getChildren().addAll(lblTemp, lblDesc, lblMinMax);
        centre.getChildren().addAll(lblIcone, tempBlock);
        return centre;
    }

    private Region createSeparator() {
        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setMaxWidth(Double.MAX_VALUE);
        sep.setStyle("-fx-background-color:rgba(255,255,255,0.20);");
        VBox.setMargin(sep, new Insets(14, 16, 0, 16));
        return sep;
    }

    private VBox createTemperatureRange(double tempMin, double tempMax) {
        VBox rangeBlock = new VBox(6);
        rangeBlock.setPadding(new Insets(12, 20, 0, 20));

        HBox rangeLabels = new HBox();
        rangeLabels.setAlignment(Pos.CENTER_LEFT);
        Label lblMinLbl = new Label(String.format("%.0f°", tempMin));
        lblMinLbl.setStyle("-fx-font-size:12px;-fx-text-fill:rgba(255,255,255,0.75);");
        Label lblMaxLbl = new Label(String.format("%.0f°", tempMax));
        lblMaxLbl.setStyle("-fx-font-size:12px;-fx-text-fill:rgba(255,255,255,0.75);");
        Region rangeSpring = new Region();
        HBox.setHgrow(rangeSpring, Priority.ALWAYS);
        rangeLabels.getChildren().addAll(lblMinLbl, rangeSpring, lblMaxLbl);

        Pane barBg = new Pane();
        barBg.setPrefHeight(6);
        barBg.setMaxWidth(Double.MAX_VALUE);
        barBg.setStyle("-fx-background-color:linear-gradient(to right,#64b5f6,#ff7043);-fx-background-radius:3;");

        rangeBlock.getChildren().addAll(rangeLabels, barBg);
        return rangeBlock;
    }

    private HBox createWeatherStats(double pluie, double vent, double uv) {
        HBox stats = new HBox(8);
        stats.setPadding(new Insets(14, 16, 16, 16));
        stats.setAlignment(Pos.CENTER);

        stats.getChildren().addAll(
                buildStatTile("🌧", String.format("%.1f mm", pluie), "Précipitations"),
                buildStatTile("💨", String.format("%.0f km/h", vent), "Vent"),
                buildStatTile("☀", String.format("UV %d", (int) uv), "Indice UV")
        );
        return stats;
    }

    private VBox createWeatherConseil(int code, double pluie) {
        String conseil = getConseilMeteo(code, pluie);
        if (conseil.isBlank()) return null;

        VBox conseilBox = new VBox();
        conseilBox.setStyle("-fx-background-color:rgba(255,255,255,0.18);-fx-background-radius:0 0 20 20;");
        conseilBox.setPadding(new Insets(10, 18, 14, 18));

        Label lblConseil = new Label("💡  " + conseil);
        lblConseil.setStyle("-fx-font-size:12px;-fx-text-fill:rgba(255,255,255,0.90);-fx-font-style:italic;");
        lblConseil.setWrapText(true);
        conseilBox.getChildren().add(lblConseil);

        return conseilBox;
    }

    private VBox buildStatTile(String icon, String value, String label) {
        VBox tile = new VBox(4);
        tile.setAlignment(Pos.CENTER);
        tile.setPrefWidth(100);
        tile.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(tile, Priority.ALWAYS);
        tile.setStyle("-fx-background-color:rgba(255,255,255,0.15);-fx-background-radius:14;-fx-padding:10 8;");

        Label lblIcon = new Label(icon);
        lblIcon.setStyle("-fx-font-size:20px;");
        Label lblVal = new Label(value);
        lblVal.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:white;");
        Label lblLbl = new Label(label);
        lblLbl.setStyle("-fx-font-size:10px;-fx-text-fill:rgba(255,255,255,0.70);");

        tile.getChildren().addAll(lblIcon, lblVal, lblLbl);
        return tile;
    }

    private HBox createErrorRow(String message) {
        HBox errRow = new HBox(8);
        errRow.setAlignment(Pos.CENTER_LEFT);
        errRow.setStyle("-fx-background-color:#fff8f0;-fx-border-color:#f59e0b;"
                + "-fx-border-width:1;-fx-border-radius:10;-fx-background-radius:10;-fx-padding:10 14;");
        Label lblErr = new Label(message);
        lblErr.setStyle("-fx-font-size:12px;-fx-text-fill:#92400e;");
        errRow.getChildren().add(lblErr);
        return errRow;
    }

    private String[] weatherGradient(int code) {
        if (code == 0 || code == 1) return new String[]{"#1a73e8", "#0d47a1"};
        if (code == 2 || code == 3) return new String[]{"#546e7a", "#37474f"};
        if (code <= 49)             return new String[]{"#607d8b", "#455a64"};
        if (code <= 69)             return new String[]{"#1565c0", "#0d47a1"};
        if (code <= 79)             return new String[]{"#455a64", "#263238"};
        if (code <= 84)             return new String[]{"#0277bd", "#01579b"};
        return new String[]{"#4a148c", "#1a237e"};
    }

    private double extraireDouble(String json, String key) {
        int i = json.indexOf(key);
        if (i < 0) return 0;
        String sub = json.substring(i + key.length()).trim();
        int end  = sub.indexOf(',');
        int end2 = sub.indexOf('}');
        if (end < 0 || (end2 >= 0 && end2 < end)) end = end2;
        if (end < 0) end = sub.length();
        try {
            return Double.parseDouble(sub.substring(0, end).trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Extracts the value of the "name" key from a JSON geocoding response. */
    private String extraireCityName(String json) {
        int i = json.indexOf("\"name\":\"");
        if (i < 0) return "";
        String sub = json.substring(i + 8); // length of "\"name\":\"" = 8
        int end = sub.indexOf('"');
        return end >= 0 ? sub.substring(0, end) : "";
    }

    private double extraireDoubleArray(String json, String key) {
        int i = json.indexOf(key);
        if (i < 0) return 0;
        String sub = json.substring(i + key.length()).trim();
        int end = sub.indexOf(']');
        if (end < 0) end = sub.length();
        String val = sub.substring(0, end).split(",")[0].trim();
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String weatherEmoji(int code) {
        if (code == 0)   return "☀️";
        if (code <= 2)   return "🌤";
        if (code == 3)   return "☁️";
        if (code <= 49)  return "🌫";
        if (code <= 59)  return "🌦";
        if (code <= 69)  return "🌧";
        if (code <= 79)  return "❄️";
        if (code <= 84)  return "🌧";
        if (code <= 99)  return "⛈";
        return "🌡";
    }

    private String weatherDesc(int code) {
        if (code == 0)  return "Ciel dégagé";
        if (code == 1)  return "Principalement dégagé";
        if (code == 2)  return "Partiellement nuageux";
        if (code == 3)  return "Couvert";
        if (code <= 49) return "Brouillard";
        if (code <= 59) return "Bruine";
        if (code <= 69) return "Pluie";
        if (code <= 79) return "Neige";
        if (code <= 84) return "Averses";
        if (code <= 99) return "Orage";
        return "Conditions inconnues";
    }

    private String getConseilMeteo(int code, double pluie) {
        // FIX: reordered conditions so the snow range (70-79) is reachable.
        // Original had rain (60-79) checked before snow (70-79), making snow unreachable.
        if (code >= 80 && code <= 99)
            return "Prévoyez un parapluie et du temps supplémentaire pour le trajet.";
        if (code >= 70 && code <= 79)
            return "Risque de neige — vérifiez les conditions routières.";
        if ((code >= 60 && code <= 69) || pluie > 2)
            return "Pensez à vous couvrir, risque de pluie ce jour-là.";
        if (code == 0 || code == 1)
            return "Beau temps prévu, idéal pour votre déplacement !";
        return "";
    }

    // ─────────────────────────────────────────────────────────────────
    //  NAVIGATION COMPTE RENDU
    // ─────────────────────────────────────────────────────────────────

    private void naviguerVersCompteRendu(int idRdv) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ConsulterCompteRendu.fxml"));
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
        } catch (Exception ignored) {}

        VBox body = new VBox(16);
        String label = allDoctors.stream()
                .filter(d -> d.getId() == rv.getMedecin_id())
                .findFirst()
                .map(this::doctorLabel)
                .orElse("Dr n°" + rv.getMedecin_id());

        Label    lblMedecin  = createStyledLabel(label);
        TextArea taContenu   = createTextArea("Contenu du compte rendu...", 4);
        TextField tfDiagnostic = styledField("Diagnostic médical");
        TextArea taTraitement  = createTextArea("Traitement prescrit...", 3);
        DatePicker dpProchain  = new DatePicker();
        dpProchain.setStyle(fieldStyle());
        dpProchain.setMaxWidth(Double.MAX_VALUE);
        CheckBox cbConf = new CheckBox("Marquer comme confidentiel");
        cbConf.setStyle("-fx-font-size:13px;-fx-text-fill:#555;");
        Label lblErr = new Label("");
        lblErr.setStyle("-fx-text-fill:#ff4b3a;-fx-font-size:12px;");
        lblErr.setWrapText(true);
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
                setStatus("Compte rendu ajouté pour le RDV n°" + rv.getId_rdv(), false);
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
                cbConf, lblErr, btnSave);

        showPopup("Nouveau Compte Rendu — RDV n°" + rv.getId_rdv(), body);
    }

    private Label createStyledLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size:13px;-fx-text-fill:#333;"
                + "-fx-background-color:#f0e6ea;-fx-border-color:#e5b7c4;"
                + "-fx-border-radius:8;-fx-background-radius:8;-fx-padding:8 10;");
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    private TextArea createTextArea(String prompt, int rows) {
        TextArea ta = new TextArea();
        ta.setPromptText(prompt);
        ta.setPrefRowCount(rows);
        ta.setStyle(fieldStyle());
        ta.setWrapText(true);
        return ta;
    }

    // ─────────────────────────────────────────────────────────────────
    //  CHARGEMENT
    // ─────────────────────────────────────────────────────────────────

    public void loadData() {
        try {
            allRdvs = rdvCRUD.getAll();
            afficherListe();
            applyFilters();
        } catch (Exception e) {
            setStatus("Erreur chargement : " + e.getMessage(), true);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  NAVIGATION CENTRE
    // ─────────────────────────────────────────────────────────────────

    private void afficherListe() {
        scrollDetail.setVisible(false);
        scrollDetail.setManaged(false);
        scrollCenter.setVisible(true);
        scrollCenter.setManaged(true);
    }

    private void showPopup(String title, VBox content) {
        HBox header = createPopupHeader(title);
        VBox card   = createPopupCard(content);
        VBox wrapper = new VBox(16);
        wrapper.setPadding(new Insets(20, 24, 24, 24));
        wrapper.getChildren().add(card);
        VBox view = new VBox(0);
        view.getChildren().addAll(header, wrapper);
        detailContent.getChildren().setAll(view);
        scrollCenter.setVisible(false);
        scrollCenter.setManaged(false);
        scrollDetail.setVisible(true);
        scrollDetail.setManaged(true);
        FadeTransition ft = new FadeTransition(Duration.millis(200), scrollDetail);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    private HBox createPopupHeader(String title) {
        HBox header = new HBox(14);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color:white;-fx-border-color:transparent transparent #ececec transparent;-fx-padding:14 24;");
        Button btnBack = new Button("← Retour");
        btnBack.setStyle("-fx-background-color:#f0e6ea;-fx-text-fill:#7a002f;"
                + "-fx-font-size:12px;-fx-font-weight:bold;"
                + "-fx-border-radius:8;-fx-background-radius:8;-fx-padding:7 16;-fx-cursor:hand;");
        btnBack.setOnAction(e -> fermerPopup());
        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-font-size:20px;-fx-font-weight:bold;-fx-text-fill:#1a1a2e;");
        header.getChildren().addAll(btnBack, lblTitle);
        return header;
    }

    private VBox createPopupCard(VBox content) {
        VBox card = new VBox(0);
        card.setStyle("-fx-background-color:white;-fx-border-color:#e0e0e0;-fx-border-width:1;"
                + "-fx-border-radius:14;-fx-background-radius:14;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),12,0,0,3);");
        card.setPadding(new Insets(24));
        card.getChildren().add(content);
        return card;
    }

    @FXML
    private void fermerPopup() {
        FadeTransition ft = new FadeTransition(Duration.millis(150), scrollDetail);
        ft.setFromValue(1);
        ft.setToValue(0);
        ft.setOnFinished(e -> afficherListe());
        ft.play();
    }

    // ─────────────────────────────────────────────────────────────────
    //  FILTRES
    // ─────────────────────────────────────────────────────────────────

    private void applyFilters() {
        String search = searchField.getText().toLowerCase().trim();
        String statut = filterStatut.getValue();

        List<RendezVous> filtered = allRdvs.stream()
                .filter(rv -> {
                    if (vueActive == Vue.PATIENT  && rv.getPatient_id() != currentPatientId) return false;
                    if (vueActive == Vue.DOCTEUR  && rv.getMedecin_id() != currentMedecinId) return false;
                    boolean matchSearch = search.isEmpty()
                            || (rv.getMotif() != null && rv.getMotif().toLowerCase().contains(search))
                            || (rv.getLieu()  != null && rv.getLieu().toLowerCase().contains(search))
                            || String.valueOf(rv.getPatient_id()).contains(search);
                    boolean matchStatut = "Tous".equals(statut) || rv.getStatut().equals(statut);
                    return matchSearch && matchStatut;
                })
                .toList();   // FIX: collect(toList()) → toList() (Java 16+)

        buildGrid(filtered);
        lblTotal.setText(filtered.size() + " rendez-vous");
    }

    // ─────────────────────────────────────────────────────────────────
    //  BASCULER VUES
    // ─────────────────────────────────────────────────────────────────

    @FXML
    private void switchToPatient() {
        vueActive = Vue.PATIENT;
        filterStatut.setValue("Tous");
        applyFilters();
    }

    @FXML
    private void switchToDocteur() {
        vueActive = Vue.DOCTEUR;
        filterStatut.setValue("Tous");
        applyFilters();
    }

    // ─────────────────────────────────────────────────────────────────
    //  GRILLE DE CARTES
    // ─────────────────────────────────────────────────────────────────

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
            gridCards.add(buildCard(rv), col, row);
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

    // ─────────────────────────────────────────────────────────────────
    //  CARTE RDV
    // ─────────────────────────────────────────────────────────────────

    private VBox buildCard(RendezVous rv) {
        String[] colors     = statutColors(rv.getStatut());
        String borderColor  = colors[0], bgColor = colors[1], hoverColor = colors[2];

        VBox card = new VBox(8);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setStyle("-fx-border-color:" + borderColor + ";-fx-border-width:2;"
                + "-fx-border-radius:12;-fx-background-radius:12;"
                + "-fx-background-color:" + bgColor + ";-fx-padding:14 16;-fx-cursor:hand;");

        Label lblDate = new Label(rv.getDate_rdv() != null ? rv.getDate_rdv().format(FMT) : "—");
        lblDate.setStyle("-fx-font-size:11px;-fx-text-fill:#888;");

        Label lblStatut = new Label(statutLabel(rv.getStatut()));
        lblStatut.setStyle("-fx-font-size:10px;-fx-font-weight:bold;-fx-text-fill:" + borderColor + ";"
                + "-fx-border-color:" + borderColor + ";-fx-border-radius:4;"
                + "-fx-padding:2 6;-fx-background-radius:4;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        HBox top = new HBox(8, lblDate, sp, lblStatut);
        top.setAlignment(Pos.CENTER_LEFT);

        Label lblMotif = new Label(rv.getMotif() != null ? rv.getMotif() : "—");
        lblMotif.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#1a1a2e;");
        lblMotif.setWrapText(true);

        String docLabel = allDoctors.stream()
                .filter(d -> d.getId() == rv.getMedecin_id())
                .findFirst()
                .map(this::doctorLabel)
                .orElse("Dr n°" + rv.getMedecin_id());

        Label lblPerson = new Label(vueActive == Vue.PATIENT
                ? docLabel : "Patient " + nomPatient(rv.getPatient_id()));
        lblPerson.setStyle("-fx-font-size:11px;-fx-text-fill:#aaa;");

        boolean aUnLieu = rv.getLieu() != null && !rv.getLieu().isBlank();
        Label lblLieu = new Label(aUnLieu ? "📍 " + rv.getLieu() : "");
        if (aUnLieu) {
            lblLieu.setStyle("-fx-font-size:11px;-fx-text-fill:#4285F4;-fx-cursor:hand;-fx-underline:true;");
            lblLieu.setTooltip(new Tooltip("Ouvrir dans Google Maps"));
            lblLieu.setOnMouseClicked(e -> ouvrirGoogleMaps(rv.getLieu()));
        }

        HBox actions = buildCardActions(rv);
        card.getChildren().addAll(top, lblMotif, lblPerson);
        if (aUnLieu) card.getChildren().add(lblLieu);
        card.getChildren().add(actions);

        card.setOnMouseEntered(e -> card.setStyle(card.getStyle().replace(bgColor,    hoverColor)));
        card.setOnMouseExited (e -> card.setStyle(card.getStyle().replace(hoverColor, bgColor)));
        return card;
    }

    // ─────────────────────────────────────────────────────────────────
    //  BOUTONS D'ACTION CARTE
    // ─────────────────────────────────────────────────────────────────

    private HBox buildCardActions(RendezVous rv) {
        HBox box = new HBox(6);
        box.setAlignment(Pos.CENTER_RIGHT);
        box.setPadding(new Insets(6, 0, 0, 0));

        boolean aUnLieu = rv.getLieu() != null && !rv.getLieu().isBlank();

        if (vueActive == Vue.PATIENT) {
            Button btnDetail = smallBtn("Détails", "#7a002f");
            btnDetail.setOnAction(e -> ouvrirDetailPatient(rv));
            box.getChildren().add(btnDetail);

            if ("en_cours".equals(rv.getStatut()) || "confirme".equals(rv.getStatut())) {
                Button btnEdit = smallBtn("✏ Modifier", "#3b82f6");
                btnEdit.setOnAction(e -> ouvrirModificationPatientView(rv));
                Button btnAnn = smallBtn("Annuler", "#ff4b3a");
                btnAnn.setOnAction(e -> ouvrirConfirmationAnnulation(rv));
                Button btnDel = smallBtn("🗑", "#aaa");
                btnDel.setOnAction(e -> ouvrirConfirmationSuppression(rv));
                box.getChildren().addAll(btnEdit, btnAnn, btnDel);
            }

            if ("annule".equals(rv.getStatut()) || "termine".equals(rv.getStatut())) {
                Button btnDel = smallBtn("🗑 Supprimer", "#aaa");
                btnDel.setOnAction(e -> ouvrirConfirmationSuppression(rv));
                box.getChildren().add(btnDel);
            }

            if (aUnLieu) {
                Button btnMaps = smallBtn("🗺️ Maps", "#4285F4");
                btnMaps.setOnAction(e -> ouvrirGoogleMaps(rv.getLieu()));
                box.getChildren().add(btnMaps);
            }
        } else {
            Button btnDetail = smallBtn("Détails", "#7a002f");
            btnDetail.setOnAction(e -> ouvrirDetailDocteur(rv));
            box.getChildren().add(btnDetail);

            if ("en_cours".equals(rv.getStatut())) {
                Button btnConf = smallBtn("Confirmer ✓", "#38b24a");
                btnConf.setOnAction(e -> changerStatut(rv, "confirme"));
                Button btnRef = smallBtn("Refuser ✗", "#ff4b3a");
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
            btnDel.setOnAction(e -> ouvrirConfirmationSuppression(rv));
            box.getChildren().add(btnDel);

            if ("confirme".equals(rv.getStatut()) || "termine".equals(rv.getStatut())) {
                Button btnCR = smallBtn("📋 Compte Rendu", "#7a002f");
                btnCR.setOnAction(e -> ouvrirAjoutCompteRenduView(rv));
                box.getChildren().add(btnCR);
            }

            if (aUnLieu) {
                Button btnMaps = smallBtn("🗺️ Maps", "#4285F4");
                btnMaps.setOnAction(e -> ouvrirGoogleMaps(rv.getLieu()));
                box.getChildren().add(btnMaps);
            }
        }
        return box;
    }

    // ─────────────────────────────────────────────────────────────────
    //  VUES DÉTAIL
    // ─────────────────────────────────────────────────────────────────

    private void ouvrirDetailPatient(RendezVous rv) {
        VBox body = new VBox(12);
        String label = allDoctors.stream()
                .filter(d -> d.getId() == rv.getMedecin_id())
                .findFirst()
                .map(this::doctorLabel)
                .orElse("Dr n°" + rv.getMedecin_id());

        body.getChildren().addAll(
                detailRow("Médecin", label),
                detailRow("Date",    rv.getDate_rdv() != null ? rv.getDate_rdv().format(FMT) : "—"),
                detailRow("Motif",   rv.getMotif()),
                detailRow("Lieu",    rv.getLieu()),
                detailRow("Statut",  statutLabel(rv.getStatut())),
                detailRow("Notes",   rv.getNotes() != null ? rv.getNotes() : "—")
        );

        if (rv.getLieu() != null && !rv.getLieu().isBlank())
            body.getChildren().add(createMapsButton(rv.getLieu()));

        if (rv.getDate_rdv() != null)
            body.getChildren().add(createWeatherButton(rv, body));

        showPopup("Détails du Rendez-Vous n°" + rv.getId_rdv(), body);
    }

    private Button createMapsButton(String lieu) {
        Button btn = new Button("🗺️ Voir sur Google Maps");
        btn.setStyle("-fx-background-color:#4285F4;-fx-text-fill:white;"
                + "-fx-font-size:12px;-fx-border-radius:8;-fx-background-radius:8;"
                + "-fx-padding:8 16;-fx-cursor:hand;");
        btn.setOnAction(e -> ouvrirGoogleMaps(lieu));
        return btn;
    }

    private Button createWeatherButton(RendezVous rv, VBox body) {
        Button btnMeteo = new Button("🌤  Voir la météo de ce jour");
        btnMeteo.setMaxWidth(Double.MAX_VALUE);
        btnMeteo.setStyle("-fx-background-color:#0ea5e9;-fx-text-fill:white;"
                + "-fx-font-size:12px;-fx-font-weight:bold;"
                + "-fx-border-radius:8;-fx-background-radius:8;"
                + "-fx-padding:9 16;-fx-cursor:hand;");

        btnMeteo.setOnAction(e -> {
            btnMeteo.setDisable(true);
            btnMeteo.setText("⏳ Chargement de la météo...");
            String lieu = rv.getLieu() != null ? rv.getLieu() : "";
            Thread t = new Thread(() ->
                    javafx.application.Platform.runLater(() -> {
                        afficherMeteo(rv.getDate_rdv().toLocalDate(), lieu, body);
                        btnMeteo.setVisible(false);
                        btnMeteo.setManaged(false);
                    }));
            t.setDaemon(true);
            t.start();
        });
        return btnMeteo;
    }

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

        if ("en_cours".equals(rv.getStatut())) {
            body.getChildren().add(sectionLabel("Actions rapides"));
            body.getChildren().add(createActionButton("✓ Confirmer le rendez-vous", "#38b24a", e -> {
                changerStatut(rv, "confirme"); fermerPopup();
            }));
            body.getChildren().add(createActionButton("✗ Refuser le rendez-vous", "#ff4b3a", e -> {
                changerStatut(rv, "annule"); fermerPopup();
            }));
        } else if ("confirme".equals(rv.getStatut())) {
            body.getChildren().add(sectionLabel("Actions rapides"));
            body.getChildren().add(createActionButton("✓ Marquer comme terminé", "#888", e -> {
                changerStatut(rv, "termine"); fermerPopup();
            }));
            body.getChildren().add(createActionButton("✗ Annuler le rendez-vous", "#ff4b3a", e -> {
                changerStatut(rv, "annule"); fermerPopup();
            }));
        }

        if (rv.getLieu() != null && !rv.getLieu().isBlank())
            body.getChildren().add(createMapsButton(rv.getLieu()));

        showPopup("Détails RDV n°" + rv.getId_rdv(), body);
    }

    private Button createActionButton(String text, String color,
                                      javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle("-fx-background-color:" + color + ";-fx-text-fill:white;"
                + "-fx-font-size:13px;-fx-border-radius:8;-fx-background-radius:8;"
                + "-fx-padding:10 16;-fx-cursor:hand;");
        btn.setOnAction(handler);
        return btn;
    }

    // ─────────────────────────────────────────────────────────────────
    //  VUES MODIFICATION
    // ─────────────────────────────────────────────────────────────────

    private void ouvrirModificationPatientView(RendezVous rv) {
        VBox body = new VBox(16);

        body.getChildren().add(createInfoBox(
                "Date actuelle : " + (rv.getDate_rdv() != null ? rv.getDate_rdv().format(FMT) : "—")));

        DatePicker dp = createDatePicker(rv.getDate_rdv() != null
                ? rv.getDate_rdv().toLocalDate() : LocalDate.now());
        TextField tfHeure = styledField("HH:mm");
        tfHeure.setText(rv.getDate_rdv() != null
                ? rv.getDate_rdv().toLocalTime().format(TIME_FMT) : "09:00");
        TextField tfMotif = styledField("Motif de consultation");
        tfMotif.setText(rv.getMotif() != null ? rv.getMotif() : "");
        TextField tfLieu = styledField("Lieu (optionnel)");
        tfLieu.setText(rv.getLieu() != null ? rv.getLieu() : "");
        Label lblErr = new Label("");
        lblErr.setStyle("-fx-text-fill:#ff4b3a;-fx-font-size:12px;");
        Button btnSave = primaryBtn("💾 Enregistrer les modifications");

        btnSave.setOnAction(e -> {
            lblErr.setText("");
            try {
                if (dp.getValue() == null) {
                    lblErr.setText("Veuillez choisir une date.");
                    return;
                }
                if (dp.getValue().isBefore(LocalDate.now())) {
                    lblErr.setText("La date ne peut pas être dans le passé.");
                    return;
                }
                LocalTime heure = LocalTime.parse(tfHeure.getText().trim(),
                        DateTimeFormatter.ofPattern("HH:mm"));
                rv.setDate_rdv(LocalDateTime.of(dp.getValue(), heure));
                rv.setMotif(tfMotif.getText().trim());
                rv.setLieu(tfLieu.getText().trim());
                rdvCRUD.modifier(rv);
                loadData();
                setStatus("Rendez-vous mis à jour.", false);
                fermerPopup();
            } catch (Exception ex) {
                lblErr.setText("Erreur : " + ex.getMessage());
            }
        });

        body.getChildren().addAll(
                formRow("Nouvelle date",  dp),
                formRow("Nouvelle heure", tfHeure),
                formRow("Motif",  tfMotif),
                formRow("Lieu",   tfLieu),
                lblErr, btnSave);

        showPopup("Modifier mon RDV n°" + rv.getId_rdv(), body);
    }

    private VBox createInfoBox(String text) {
        VBox box = new VBox(4);
        box.setStyle("-fx-background-color:#fffbeb;-fx-border-color:#f59e0b;-fx-border-width:1;"
                + "-fx-border-radius:8;-fx-background-radius:8;-fx-padding:10 12;");
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size:12px;-fx-text-fill:#92400e;");
        box.getChildren().add(lbl);
        return box;
    }

    private DatePicker createDatePicker(LocalDate initialDate) {
        DatePicker dp = new DatePicker(initialDate);
        dp.setStyle(fieldStyle());
        dp.setMaxWidth(Double.MAX_VALUE);
        dp.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate d, boolean empty) {
                super.updateItem(d, empty);
                setDisable(empty || d.isBefore(LocalDate.now()));
            }
        });
        return dp;
    }

    private void ouvrirModificationDocView(RendezVous rv) {
        VBox body = new VBox(16);

        DatePicker dp = createDatePicker(rv.getDate_rdv() != null
                ? rv.getDate_rdv().toLocalDate() : LocalDate.now());
        TextField tfHeure = styledField("HH:mm");
        tfHeure.setText(rv.getDate_rdv() != null
                ? rv.getDate_rdv().toLocalTime().format(TIME_FMT) : "09:00");
        TextField tfMotif = styledField("Motif");
        tfMotif.setText(rv.getMotif() != null ? rv.getMotif() : "");
        TextField tfLieu = styledField("Lieu");
        tfLieu.setText(rv.getLieu() != null ? rv.getLieu() : "");
        ComboBox<String> cbStatut = createStatutComboBox(rv.getStatut());
        TextArea taNotes = new TextArea(rv.getNotes() != null ? rv.getNotes() : "");
        taNotes.setPrefRowCount(3);
        taNotes.setStyle(fieldStyle());
        Label lblErr = new Label("");
        lblErr.setStyle("-fx-text-fill:#ff4b3a;-fx-font-size:12px;");
        Button btnSave = primaryBtn("Enregistrer les modifications");

        btnSave.setOnAction(e -> {
            try {
                LocalTime heure = LocalTime.parse(tfHeure.getText().trim(),
                        DateTimeFormatter.ofPattern("HH:mm"));
                rv.setDate_rdv(LocalDateTime.of(dp.getValue(), heure));
                rv.setMotif(tfMotif.getText().trim());
                rv.setLieu(tfLieu.getText().trim());
                rv.setNotes(taNotes.getText().trim());
                rv.setStatut(cbStatut.getValue());
                rdvCRUD.modifier(rv);
                loadData();
                setStatus("Rendez-vous mis à jour.", false);
                fermerPopup();
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
                lblErr, btnSave);

        showPopup("Modifier RDV n°" + rv.getId_rdv(), body);
    }

    private ComboBox<String> createStatutComboBox(String currentStatut) {
        ComboBox<String> cb = new ComboBox<>();
        cb.getItems().addAll("en_cours", "confirme", "annule", "termine");
        cb.setValue(currentStatut);
        cb.setMaxWidth(Double.MAX_VALUE);
        cb.setStyle(fieldStyle());
        return cb;
    }

    // ─────────────────────────────────────────────────────────────────
    //  VUES CONFIRMATION
    // ─────────────────────────────────────────────────────────────────

    private void ouvrirConfirmationAnnulation(RendezVous rv) {
        VBox body = createConfirmationBody(
                "⚠️",
                "Voulez-vous vraiment annuler ce rendez-vous ?\n\n"
                        + "Motif : " + (rv.getMotif() != null ? rv.getMotif() : "—") + "\n"
                        + "Date  : " + (rv.getDate_rdv() != null ? rv.getDate_rdv().format(FMT) : "—"),
                () -> changerStatut(rv, "annule")
        );
        showPopup("Confirmer l'annulation", body);
    }

    private void ouvrirConfirmationSuppression(RendezVous rv) {
        VBox body = createConfirmationBody(
                "🗑️",
                "Supprimer définitivement ce rendez-vous ?\n\n"
                        + "Motif : " + (rv.getMotif() != null ? rv.getMotif() : "—") + "\n"
                        + "Date  : " + (rv.getDate_rdv() != null ? rv.getDate_rdv().format(FMT) : "—") + "\n\n"
                        + "Cette action est irréversible.",
                () -> { supprimerRdv(rv); loadData(); fermerPopup(); }
        );
        showPopup("Confirmer la suppression", body);
    }

    private VBox createConfirmationBody(String icon, String message, Runnable onConfirm) {
        VBox body = new VBox(20);
        body.setAlignment(Pos.CENTER);

        Label icone = new Label(icon);
        icone.setStyle("-fx-font-size:40px;");

        Label msg = new Label(message);
        msg.setStyle("-fx-font-size:14px;-fx-text-fill:#333;-fx-text-alignment:center;");
        msg.setWrapText(true);

        // FIX: removed duplicate -fx-text-fill from secondaryBtn; kept only one declaration
        Button btnNon = secondaryBtn("Non, garder");
        btnNon.setOnAction(e -> fermerPopup());

        Button btnOui = new Button("Oui, confirmer");
        btnOui.setStyle("-fx-background-color:#ff4b3a;-fx-text-fill:white;"
                + "-fx-font-size:13px;-fx-border-radius:8;-fx-background-radius:8;"
                + "-fx-padding:10 20;-fx-cursor:hand;");
        btnOui.setOnAction(e -> onConfirm.run());

        HBox buttons = new HBox(12, btnNon, btnOui);
        buttons.setAlignment(Pos.CENTER);
        body.getChildren().addAll(icone, msg, buttons);
        return body;
    }

    // ─────────────────────────────────────────────────────────────────
    //  NOUVEAU RDV
    // ─────────────────────────────────────────────────────────────────

    @FXML
    private void ouvrirAjoutPopup() {
        selectedDoctor  = null;
        selectedDate    = null;
        selectedCreneau = null;

        VBox body     = new VBox(16);
        ComboBox<User> cbMedecin = createDoctorComboBox();
        VBox calZone  = new VBox(8);
        VBox slotZone = new VBox(6);
        calZone .setStyle("-fx-opacity:0.4;");
        slotZone.setStyle("-fx-opacity:0.4;");

        TextField tfMotif  = styledField("Motif de consultation");
        TextField tfLieu   = styledField("Lieu (optionnel)");
        TextArea  taNotes  = new TextArea();
        taNotes.setPromptText("Notes…");
        taNotes.setPrefRowCount(2);
        taNotes.setStyle(fieldStyle());
        Label lblErr = new Label("");
        lblErr.setStyle("-fx-text-fill:#ff4b3a;-fx-font-size:12px;");
        confirmBtn = primaryBtn("Confirmer le rendez-vous");
        confirmBtn.setDisable(true);

        final YearMonth[] popupMonth = {YearMonth.now()};
        final Label[]     lblMois   = {new Label()};
        lblMois[0].setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#7a002f;");

        Runnable fillSlots    = () -> updateSlots(slotZone, lblErr);
        Runnable fillPopupCal = () -> updateCalendar(calZone, popupMonth, lblMois, fillSlots);

        cbMedecin.valueProperty().addListener((o, old, doc) -> {
            selectedDoctor  = doc;
            selectedDate    = null;
            selectedCreneau = null;
            popupMonth[0]   = YearMonth.now();
            fillPopupCal.run();
            slotZone.getChildren().clear();
            slotZone.setStyle("-fx-opacity:0.4;");
            confirmBtn.setDisable(true);
        });

        allDoctors.stream()
                .filter(d -> d.getId() == currentMedecinId)
                .findFirst()
                .ifPresent(d -> { selectedDoctor = d; cbMedecin.setValue(d); });

        confirmBtn.setOnAction(ev -> saveNewRendezVous(lblErr));

        body.getChildren().addAll(
                sectionLabel("1. Choisir un médecin"),          formRow("Médecin", cbMedecin),
                sectionLabel("2. Choisir une date disponible"),  calZone,
                sectionLabel("3. Choisir un créneau horaire"),   slotZone,
                sectionLabel("4. Informations complémentaires"),
                formRow("Motif", tfMotif),
                formRow("Lieu",  tfLieu),
                formRow("Notes", taNotes),
                lblErr, confirmBtn);

        showPopup("Nouveau Rendez-Vous", body);
    }

    private ComboBox<User> createDoctorComboBox() {
        ComboBox<User> cb = new ComboBox<>();
        cb.setMaxWidth(Double.MAX_VALUE);
        cb.setStyle(fieldStyle());
        cb.setPromptText("Sélectionner un médecin…");
        cb.getItems().addAll(allDoctors);
        cb.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(User u, boolean empty) {
                super.updateItem(u, empty);
                setText(empty || u == null ? null : doctorLabel(u));
            }
        });
        cb.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(User u, boolean empty) {
                super.updateItem(u, empty);
                setText(empty || u == null ? "Sélectionner un médecin…" : doctorLabel(u));
            }
        });
        return cb;
    }

    private void updateSlots(VBox slotZone, Label lblErr) {
        slotZone.getChildren().clear();
        if (selectedDoctor == null || selectedDate == null) {
            slotZone.setStyle("-fx-opacity:0.4;");
            return;
        }
        try {
            List<Disponibilite> dispos = dispoCRUD.getCreneauxLibres(selectedDoctor.getId())
                    .stream()
                    .filter(d -> d.getDate_dispo().equals(selectedDate))
                    .toList();

            if (dispos.isEmpty()) {
                Label none = new Label("Aucun créneau libre ce jour.");
                none.setStyle("-fx-font-size:12px;-fx-text-fill:#aaa;");
                slotZone.getChildren().add(none);
            } else {
                slotZone.setStyle("");
                for (Disponibilite d : dispos)
                    slotZone.getChildren().add(createSlotButton(d));
            }
        } catch (Exception ex) {
            lblErr.setText("Erreur créneaux : " + ex.getMessage());
        }
    }

    private Button createSlotButton(Disponibilite d) {
        Button btn = new Button(d.getHeure_debut().format(TIME_FMT) + " – " + d.getHeure_fin().format(TIME_FMT));
        btn.setMaxWidth(Double.MAX_VALUE);
        final String defaultStyle = "-fx-background-color:#f0fdf4;-fx-text-fill:#38b24a;"
                + "-fx-border-color:#38b24a;-fx-border-width:1;"
                + "-fx-border-radius:8;-fx-background-radius:8;"
                + "-fx-font-size:13px;-fx-padding:8 14;-fx-cursor:hand;";
        final String selectedStyle = "-fx-background-color:#7a002f;-fx-text-fill:white;"
                + "-fx-border-radius:8;-fx-background-radius:8;"
                + "-fx-font-size:13px;-fx-padding:8 14;-fx-cursor:hand;";
        btn.setStyle(defaultStyle);

        btn.setOnAction(ev -> {
            selectedCreneau = d;
            if (btn.getParent() instanceof VBox parent) {
                parent.getChildren().forEach(n -> {
                    if (n instanceof Button b) b.setStyle(defaultStyle);
                });
            }
            btn.setStyle(selectedStyle);
            if (confirmBtn != null) confirmBtn.setDisable(false);
        });
        return btn;
    }

    private void updateCalendar(VBox calZone, YearMonth[] popupMonth,
                                Label[] lblMois, Runnable fillSlots) {
        calZone.getChildren().clear();
        if (selectedDoctor == null) {
            calZone.setStyle("-fx-opacity:0.4;");
            return;
        }
        calZone.setStyle("");
        try {
            Set<LocalDate> freeDates = dispoCRUD.getCreneauxLibres(selectedDoctor.getId())
                    .stream()
                    .map(Disponibilite::getDate_dispo)
                    .collect(Collectors.toSet());

            Button bPrev = new Button("‹");
            Button bNext = new Button("›");
            styleNavBtn(bPrev);
            styleNavBtn(bNext);
            lblMois[0].setText(popupMonth[0].getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH)
                    + " " + popupMonth[0].getYear());

            bPrev.setOnAction(e -> {
                popupMonth[0] = popupMonth[0].minusMonths(1);
                updateCalendar(calZone, popupMonth, lblMois, fillSlots);
            });
            bNext.setOnAction(e -> {
                popupMonth[0] = popupMonth[0].plusMonths(1);
                updateCalendar(calZone, popupMonth, lblMois, fillSlots);
            });

            HBox nav = new HBox(10, bPrev, lblMois[0], bNext);
            nav.setAlignment(Pos.CENTER);
            calZone.getChildren().addAll(nav, createCalendarGrid(popupMonth[0], freeDates, fillSlots));
        } catch (Exception ex) {
            Label err = new Label("Erreur calendrier : " + ex.getMessage());
            err.setStyle("-fx-text-fill:#ff4b3a;-fx-font-size:12px;");
            calZone.getChildren().add(err);
        }
    }

    private GridPane createCalendarGrid(YearMonth month, Set<LocalDate> freeDates, Runnable fillSlots) {
        GridPane mini = new GridPane();
        mini.setHgap(4);
        mini.setVgap(4);
        for (int i = 0; i < 7; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / 7);
            cc.setHgrow(Priority.ALWAYS);
            mini.getColumnConstraints().add(cc);
        }

        String[] dNames = {"L", "M", "M", "J", "V", "S", "D"};
        for (int i = 0; i < 7; i++) {
            Label h = new Label(dNames[i]);
            h.setStyle("-fx-font-size:10px;-fx-text-fill:#aaa;-fx-alignment:center;");
            h.setMaxWidth(Double.MAX_VALUE);
            mini.add(h, i, 0);
        }

        int startCol   = month.atDay(1).getDayOfWeek().getValue() - 1;
        int daysInM    = month.lengthOfMonth();
        int cRow = 1, cCol = startCol;

        for (int day = 1; day <= daysInM; day++) {
            LocalDate date   = month.atDay(day);
            boolean isFree   = freeDates.contains(date);
            boolean isPast   = date.isBefore(LocalDate.now());
            boolean isSel    = date.equals(selectedDate);

            mini.add(createDayButton(day, isFree, isPast, isSel, date, fillSlots), cCol, cRow);
            if (++cCol >= 7) { cCol = 0; cRow++; }
        }
        return mini;
    }

    private Button createDayButton(int day, boolean isFree, boolean isPast,
                                   boolean isSel, LocalDate date, Runnable fillSlots) {
        Button btn = new Button(String.valueOf(day));
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setMaxHeight(Double.MAX_VALUE);

        // FIX: extracted style selection to a helper method to eliminate duplication warning
        btn.setStyle(dayButtonStyle(isSel, isFree, isPast));
        btn.setDisable(!isFree || isPast);

        btn.setOnAction(ev -> {
            selectedDate    = date;
            selectedCreneau = null;
            fillSlots.run();
        });
        return btn;
    }

    /** FIX: extracted from createDayButton to resolve "extract method" suggestion. */
    private String dayButtonStyle(boolean isSel, boolean isFree, boolean isPast) {
        if (isSel)
            return "-fx-background-color:#7a002f;-fx-text-fill:white;"
                    + "-fx-border-radius:6;-fx-background-radius:6;-fx-font-size:12px;-fx-cursor:hand;";
        if (!isFree || isPast)
            return "-fx-background-color:#f5f5f5;-fx-text-fill:#ccc;"
                    + "-fx-border-radius:6;-fx-background-radius:6;-fx-font-size:12px;";
        return "-fx-background-color:#f0fdf4;-fx-text-fill:#38b24a;"
                + "-fx-border-color:#38b24a;-fx-border-width:1;"
                + "-fx-border-radius:6;-fx-background-radius:6;-fx-font-size:12px;-fx-cursor:hand;";
    }

    private void saveNewRendezVous(Label lblErr) {
        lblErr.setText("");
        if (selectedDoctor == null)  { lblErr.setText("Choisissez un médecin."); return; }
        if (selectedCreneau == null) { lblErr.setText("Choisissez un créneau."); return; }
        try {
            LocalDateTime dateRdv = LocalDateTime.of(
                    selectedCreneau.getDate_dispo(), selectedCreneau.getHeure_debut());
            RendezVous rv = new RendezVous(
                    currentPatientId, selectedDoctor.getId(), dateRdv,
                    "", "en_cours", "", "");
            rdvCRUD.ajouter(rv);
            loadData();
            setStatus("Rendez-vous créé — en cours.", false);
            fermerPopup();
        } catch (Exception ex) {
            lblErr.setText("Erreur : " + ex.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  CALENDRIER DISPONIBILITÉS (vue Docteur)
    // ─────────────────────────────────────────────────────────────────

    private void buildDispoCalendarSection(int startRow) {
        try {
            List<Disponibilite> dispos = dispoCRUD.getByMedecin(currentMedecinId);
            gridCards.add(buildDispoCalendar(dispos), 0, startRow, 2, 1);

            Button btnAddDispo = new Button("+ Ajouter un créneau");
            btnAddDispo.setStyle("-fx-background-color:#7a002f;-fx-text-fill:white;"
                    + "-fx-font-size:13px;-fx-border-radius:8;-fx-background-radius:8;"
                    + "-fx-padding:10 20;-fx-cursor:hand;");
            btnAddDispo.setOnAction(e -> ouvrirAjoutDispoView());
            gridCards.add(btnAddDispo, 0, startRow + 1, 2, 1);
        } catch (Exception e) {
            setStatus("Erreur disponibilités : " + e.getMessage(), true);
        }
    }

    private VBox buildDispoCalendar(List<Disponibilite> dispos) {
        VBox root = new VBox(10);
        root.setStyle("-fx-background-color:white;-fx-border-color:#e0e0e0;"
                + "-fx-border-radius:14;-fx-background-radius:14;-fx-padding:18;");

        Button btnPrev = new Button("‹");
        Button btnNext = new Button("›");
        styleNavBtn(btnPrev);
        styleNavBtn(btnNext);
        Label lblMonth = new Label();
        lblMonth.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:#7a002f;");
        HBox navBar = new HBox(10, btnPrev, lblMonth, btnNext);
        navBar.setAlignment(Pos.CENTER);

        HBox legend = createLegend();

        GridPane calGrid = new GridPane();
        calGrid.setHgap(6);
        calGrid.setVgap(6);
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
            lblMonth.setText(calendarMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH)
                    + " " + calendarMonth.getYear());

            String[] days = {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"};
            for (int i = 0; i < 7; i++) {
                Label d = new Label(days[i]);
                d.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:#999;-fx-alignment:center;");
                d.setMaxWidth(Double.MAX_VALUE);
                calGrid.add(d, i, 0);
            }

            int startCol   = calendarMonth.atDay(1).getDayOfWeek().getValue() - 1;
            int daysInMonth = calendarMonth.lengthOfMonth();
            int cellRow = 1, cellCol = startCol;

            for (int day = 1; day <= daysInMonth; day++) {
                LocalDate date      = calendarMonth.atDay(day);
                List<Disponibilite> dayDispos = byDate.getOrDefault(date, Collections.emptyList());
                calGrid.add(createDayCell(date, dayDispos), cellCol, cellRow);
                if (++cellCol >= 7) { cellCol = 0; cellRow++; }
            }
        };

        fillCalendar.run();
        btnPrev.setOnAction(e -> { calendarMonth = calendarMonth.minusMonths(1); fillCalendar.run(); });
        btnNext.setOnAction(e -> { calendarMonth = calendarMonth.plusMonths(1);  fillCalendar.run(); });

        root.getChildren().addAll(navBar, legend, calGrid);
        return root;
    }

    private HBox createLegend() {
        HBox legend = new HBox(16);
        legend.setAlignment(Pos.CENTER);
        legend.getChildren().addAll(
                createLegendDot("#38b24a", "Libre"),
                createLegendDot("#ff4b3a", "Occupé"),
                createLegendDot("#f59e0b", "Bloqué"));
        return legend;
    }

    private HBox createLegendDot(String color, String label) {
        javafx.scene.shape.Circle dot = new javafx.scene.shape.Circle(5);
        dot.setStyle("-fx-fill:" + color + ";");
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size:11px;-fx-text-fill:#555;");
        return new HBox(5, dot, lbl);
    }

    private VBox createDayCell(LocalDate date, List<Disponibilite> dispos) {
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
            bg     = isPast ? "#f9f9f9" : "white";
            border = "#ececec";
        } else if (hasLibre && !hasOcc) {
            bg = "#f0fdf4"; border = "#38b24a";
        } else if (hasOcc && !hasLibre) {
            bg = "#fff5f5"; border = "#ff4b3a";
        } else {
            bg = "#fffbeb"; border = "#f59e0b";
        }

        cell.setStyle("-fx-background-color:" + bg + ";-fx-border-color:" + border + ";"
                + "-fx-border-radius:8;-fx-background-radius:8;"
                + (isToday ? "-fx-border-width:2;" : "-fx-border-width:1;")
                + "-fx-cursor:" + (dispos.isEmpty() ? "default" : "hand") + ";");

        Label lblDay = new Label(String.valueOf(date.getDayOfMonth()));
        lblDay.setStyle(isToday
                ? "-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#7a002f;"
                : "-fx-font-size:13px;-fx-font-weight:normal;-fx-text-fill:#333;");
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

    private void ouvrirDetailJourDispoView(LocalDate date, List<Disponibilite> dispos) {
        VBox body = new VBox(10);
        for (Disponibilite d : dispos)
            body.getChildren().add(createDispoRow(d));
        showPopup("Créneaux du " + date.format(DATE_FMT), body);
    }

    private HBox createDispoRow(Disponibilite d) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        String[] colors = getDispoColors(d.getStatut());
        row.setStyle("-fx-background-color:" + colors[1] + ";-fx-border-color:" + colors[0] + ";"
                + "-fx-border-radius:8;-fx-background-radius:8;-fx-padding:10 14;");

        Label lblHeure = new Label(d.getHeure_debut().format(TIME_FMT) + " – " + d.getHeure_fin().format(TIME_FMT));
        lblHeure.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#333;");

        Label lblSt = new Label(getDispoStatutLabel(d.getStatut()));
        lblSt.setStyle("-fx-font-size:11px;-fx-text-fill:" + colors[0] + ";-fx-font-weight:bold;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        HBox actBtns = new HBox(6);
        if ("libre".equals(d.getStatut())) {
            Button b = smallBtn("Bloquer", "#f59e0b");
            b.setOnAction(ev -> { modifierStatutDispo(d, "bloquee"); loadData(); fermerPopup(); });
            actBtns.getChildren().add(b);
        } else if ("bloquee".equals(d.getStatut())) {
            Button b = smallBtn("Libérer", "#38b24a");
            b.setOnAction(ev -> { modifierStatutDispo(d, "libre"); loadData(); fermerPopup(); });
            actBtns.getChildren().add(b);
        }

        Button btnDel = smallBtn("🗑", "#ddd");
        btnDel.setOnAction(ev -> { supprimerDispo(d); loadData(); fermerPopup(); });
        actBtns.getChildren().add(btnDel);

        row.getChildren().addAll(lblHeure, lblSt, sp, actBtns);
        return row;
    }

    private void ouvrirAjoutDispoView() {
        VBox body = new VBox(16);

        DatePicker dp     = createDatePicker(LocalDate.now());
        TextField tfDebut = styledField("Début (HH:mm)");
        tfDebut.setText("08:00");
        TextField tfFin   = styledField("Fin (HH:mm)");
        tfFin.setText("12:00");
        Label  lblErr  = new Label("");
        lblErr.setStyle("-fx-text-fill:#ff4b3a;-fx-font-size:12px;");
        Button btnSave = primaryBtn("Ajouter le créneau");

        btnSave.setOnAction(e -> {
            try {
                LocalTime debut = LocalTime.parse(tfDebut.getText().trim(),
                        DateTimeFormatter.ofPattern("HH:mm"));
                LocalTime fin   = LocalTime.parse(tfFin.getText().trim(),
                        DateTimeFormatter.ofPattern("HH:mm"));
                if (!fin.isAfter(debut)) {
                    lblErr.setText("L'heure de fin doit être après le début.");
                    return;
                }
                dispoCRUD.ajouter(new Disponibilite(
                        currentMedecinId, dp.getValue(), debut, fin, "libre"));
                loadData();
                setStatus("Créneau ajouté avec succès.", false);
                fermerPopup();
            } catch (Exception ex) {
                lblErr.setText("Erreur : " + ex.getMessage());
            }
        });

        body.getChildren().addAll(
                formRow("Date",        dp),
                formRow("Heure début", tfDebut),
                formRow("Heure fin",   tfFin),
                lblErr, btnSave);

        showPopup("Nouveau Créneau de Disponibilité", body);
    }

    // ─────────────────────────────────────────────────────────────────
    //  ACTIONS MÉTIER
    // ─────────────────────────────────────────────────────────────────

    private void changerStatut(RendezVous rv, String statut) {
        try {
            rdvCRUD.changerStatut(rv.getId_rdv(), statut);
            sendNotificationEmail(rv, statut);
            loadData();
            setStatus("Statut mis à jour : " + statutLabel(statut), false);
            fermerPopup();
        } catch (Exception e) {
            setStatus("Erreur : " + e.getMessage(), true);
        }
    }

    private void sendNotificationEmail(RendezVous rv, String statut) {
        User patient = userService.findById(rv.getPatient_id());
        if (patient == null || patient.getEmail() == null) return;

        String date = rv.getDate_rdv() != null
                ? rv.getDate_rdv().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")) : "—";

        String sujet, message;
        switch (statut) {
            case "confirme" -> {
                sujet   = "✅ Rendez-vous Confirmé — VITA Hôpital";
                message = "Bonjour " + patient.getPrenom() + ",\n\n"
                        + "Votre rendez-vous du " + date + " est CONFIRMÉ.\n\n"
                        + "Merci de votre confiance.\n— VITA Hôpital";
            }
            case "annule" -> {
                sujet   = "❌ Rendez-vous Annulé — VITA Hôpital";
                message = "Bonjour " + patient.getPrenom() + ",\n\n"
                        + "Votre rendez-vous du " + date + " a été ANNULÉ.\n\n"
                        + "Contactez-nous pour reprendre un RDV.\n— VITA Hôpital";
            }
            case "termine" -> {
                sujet   = "🏥 Rendez-vous Terminé — VITA Hôpital";
                message = "Bonjour " + patient.getPrenom() + ",\n\n"
                        + "Merci pour votre visite du " + date + ".\n\n"
                        + "N'hésitez pas à nous recontacter.\n— VITA Hôpital";
            }
            default -> { return; }
        }

        emailService.sendEmail(patient.getEmail(), sujet, message);
    }

    private void modifierStatutDispo(Disponibilite d, String statut) {
        try {
            dispoCRUD.updateStatut(d.getId_dispo(), statut, d.getId_rdv());
        } catch (Exception e) {   // FIX: was SQLException but method may not declare it
            setStatus("Erreur dispo : " + e.getMessage(), true);
        }
    }

    private void supprimerDispo(Disponibilite d) {
        try {
            dispoCRUD.delete(d.getId_dispo());
        } catch (Exception e) {   // FIX: was SQLException but method may not declare it
            setStatus("Erreur suppression dispo : " + e.getMessage(), true);
        }
    }

    private void supprimerRdv(RendezVous rv) {
        try {
            rdvCRUD.delete(rv.getId_rdv());
            setStatus("Rendez-vous supprimé.", false);
        } catch (Exception e) {   // FIX: was SQLException but method may not declare it
            setStatus("Erreur suppression : " + e.getMessage(), true);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  NAVIGATION SIDEBAR
    // ─────────────────────────────────────────────────────────────────

    @FXML
    private void ouvrirCompteRendus() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ConsulterCompteRendu.fxml"));
            BorderPane fullPane = loader.load();
            AfficherCompteRenduController ctrl = loader.getController();
            ctrl.filtrerParRdv(0);
            javafx.scene.Node contenu = fullPane.getCenter();
            fullPane.setCenter(null);
            mainPane.setCenter(contenu);
        } catch (Exception e) {
            setStatus("Erreur : " + e.getMessage(), true);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  HELPERS UI
    // ─────────────────────────────────────────────────────────────────

    private String fieldStyle() {
        return "-fx-font-size:13px;-fx-background-color:white;"
                + "-fx-border-color:#e5b7c4;-fx-border-radius:8;-fx-background-radius:8;"
                + "-fx-padding:8 10;";
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
        return new VBox(4, lbl, field);
    }

    private HBox detailRow(String label, String value) {
        Label lbl = new Label(label + " :");
        lbl.setStyle("-fx-font-weight:bold;-fx-font-size:13px;-fx-text-fill:#7a002f;-fx-min-width:90;");
        Label val = new Label(value != null && !value.isBlank() ? value : "—");
        val.setStyle("-fx-font-size:13px;-fx-text-fill:#333;");
        val.setWrapText(true);
        HBox row = new HBox(10, lbl, val);
        row.setStyle("-fx-background-color:#fafafa;-fx-border-color:#f0e6ea;"
                + "-fx-border-radius:6;-fx-background-radius:6;-fx-padding:8 12;");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Button smallBtn(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color:" + color + ";-fx-text-fill:white;"
                + "-fx-font-size:10px;-fx-border-radius:6;-fx-background-radius:6;"
                + "-fx-padding:4 10;-fx-cursor:hand;");
        return btn;
    }

    private Button primaryBtn(String text) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color:#7a002f;-fx-text-fill:white;"
                + "-fx-font-size:13px;-fx-font-weight:bold;"
                + "-fx-border-radius:8;-fx-background-radius:8;"
                + "-fx-padding:11 22;-fx-cursor:hand;");
        btn.setMaxWidth(Double.MAX_VALUE);
        return btn;
    }

    private Button secondaryBtn(String text) {
        Button btn = new Button(text);
        // FIX: removed duplicate -fx-text-fill that was causing overwrite warning
        btn.setStyle("-fx-background-color:#eee;-fx-text-fill:#333;"
                + "-fx-font-size:13px;-fx-border-radius:8;-fx-background-radius:8;"
                + "-fx-padding:10 20;-fx-cursor:hand;");
        return btn;
    }

    private Label sectionLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#7a002f;-fx-padding:4 0 2 0;");
        return l;
    }

    private void styleNavBtn(Button b) {
        b.setStyle("-fx-background-color:#f0e6ea;-fx-text-fill:#7a002f;"
                + "-fx-font-size:16px;-fx-border-radius:6;-fx-background-radius:6;"
                + "-fx-padding:2 10;-fx-cursor:hand;");
    }

    private void setStatus(String msg, boolean error) {
        lblStatus.setText(msg);
        lblStatus.setStyle(error
                ? "-fx-text-fill:#ff4b3a;-fx-font-weight:bold;"
                : "-fx-text-fill:#38b24a;-fx-font-weight:bold;");
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ─────────────────────────────────────────────────────────────────
    //  COULEURS
    // ─────────────────────────────────────────────────────────────────

    private String[] statutColors(String statut) {
        return switch (statut == null ? "" : statut) {
            case "en_cours" -> new String[]{"#3b82f6", "#eff6ff", "#dbeafe"};
            case "confirme" -> new String[]{"#38b24a", "#f0fdf4", "#dcfce7"};
            case "annule"   -> new String[]{"#ff4b3a", "#fff5f5", "#ffe4e4"};
            case "termine"  -> new String[]{"#888888", "#f5f5f5", "#ebebeb"};
            default         -> new String[]{"#f59e0b", "#fffbeb", "#fef3c7"};
        };
    }

    private String[] getDispoColors(String statut) {
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

    private String getDispoStatutLabel(String s) {
        return switch (s == null ? "" : s) {
            case "libre"   -> "● Libre";
            case "occupee" -> "● Occupé";
            case "bloquee" -> "● Bloqué";
            default        -> s;
        };
    }

    public static class GestionRessourceController {

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

        private List<RendezVous.Ressource> allRessources = List.of();
        private List<RendezVous.Ressource> currentRessources = List.of();
        private List<RendezVous.Evenn> allEvenements = List.of();

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
            private final RendezVous.Ressource ressource;

            private RessourceRow(boolean header, String headerTitle, RendezVous.Ressource ressource) {
                this.header = header;
                this.headerTitle = headerTitle;
                this.ressource = ressource;
            }

            static RessourceRow header(String title) {
                return new RessourceRow(true, title, null);
            }

            static RessourceRow resource(RendezVous.Ressource ressource) {
                return new RessourceRow(false, null, ressource);
            }

            boolean isHeader() {
                return header;
            }

            String getHeaderTitle() {
                return headerTitle;
            }

            RendezVous.Ressource getRessource() {
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

                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
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
                    .map(RendezVous.Evenn::getTitre)
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

            List<RendezVous.Ressource> filtered = allRessources.stream()
                    .filter(r -> matchesSearch(r, query))
                    .filter(r -> matchesType(r, typeFilter))
                    .filter(r -> matchesEvent(r, eventFilter))
                    .filter(r -> matchesStatus(r, statusFilter))
                    .sorted(Comparator
                            .comparing((RendezVous.Ressource r) -> getTypeRessource(r).ordinal())
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

        private boolean matchesSearch(RendezVous.Ressource r, String query) {
            if (r == null) return false;
            if (query == null || query.isBlank()) return true;

            return safe(r.getNomRessource()).toLowerCase(Locale.ROOT).contains(query)
                    || safe(r.getResponsable()).toLowerCase(Locale.ROOT).contains(query)
                    || safe(r.getEvenementTitre()).toLowerCase(Locale.ROOT).contains(query)
                    || safe(r.getStatut()).toLowerCase(Locale.ROOT).contains(query)
                    || getTypeRessource(r).label().toLowerCase(Locale.ROOT).contains(query);
        }

        private boolean matchesType(RendezVous.Ressource r, String typeFilter) {
            if (typeFilter == null || "Tous les types".equals(typeFilter)) return true;
            return getTypeRessource(r).label().equals(typeFilter);
        }

        private boolean matchesEvent(RendezVous.Ressource r, String eventFilter) {
            if (eventFilter == null || "Tous les événements".equals(eventFilter)) return true;

            if ("Non affectées".equals(eventFilter)) {
                return !estAffectee(r);
            }

            return safe(r.getEvenementTitre()).equals(eventFilter);
        }

        private boolean matchesStatus(RendezVous.Ressource r, String statusFilter) {
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

        private List<RessourceRow> construireListeGroupee(List<RendezVous.Ressource> ressources) {
            List<RessourceRow> rows = new ArrayList<>();

            if (ressources == null || ressources.isEmpty()) {
                return rows;
            }

            Map<ResourceType, List<RendezVous.Ressource>> groupes = new LinkedHashMap<>();
            groupes.put(ResourceType.MATERIELLE, new ArrayList<>());
            groupes.put(ResourceType.LOGICIELLE, new ArrayList<>());
            groupes.put(ResourceType.HUMAINE, new ArrayList<>());
            groupes.put(ResourceType.NON_CLASSEE, new ArrayList<>());

            for (RendezVous.Ressource r : ressources) {
                groupes.get(getTypeRessource(r)).add(r);
            }

            ajouterGroupe(rows, "Ressources matérielles", groupes.get(ResourceType.MATERIELLE));
            ajouterGroupe(rows, "Ressources logicielles", groupes.get(ResourceType.LOGICIELLE));
            ajouterGroupe(rows, "Ressources humaines", groupes.get(ResourceType.HUMAINE));
            ajouterGroupe(rows, "Ressources non classées", groupes.get(ResourceType.NON_CLASSEE));

            return rows;
        }

        private void ajouterGroupe(List<RessourceRow> rows, String titre, List<RendezVous.Ressource> ressources) {
            if (ressources == null || ressources.isEmpty()) return;

            rows.add(RessourceRow.header(titre + " — " + ressources.size()));

            for (RendezVous.Ressource r : ressources) {
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

        private VBox creerCarteRessourceListe(RendezVous.Ressource r) {
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

        private Label creerBadgeEtatRessource(RendezVous.Ressource r, boolean tension, int disponible) {
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

        private void afficherRessourcesGrille(List<RendezVous.Ressource> ressources) {
            if (ressourceGridPane == null) return;

            ressourceGridPane.getChildren().clear();

            if (ressources == null || ressources.isEmpty()) {
                Label empty = new Label("Aucune ressource trouvée");
                empty.getStyleClass().add("section-subtitle");
                ressourceGridPane.getChildren().add(empty);
                return;
            }

            for (RendezVous.Ressource r : ressources) {
                ressourceGridPane.getChildren().add(creerCarteGrille(r));
            }
        }

        private VBox creerCarteGrille(RendezVous.Ressource r) {
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
            List<RendezVous.Ressource> base = currentRessources == null ? List.of() : currentRessources;

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
                    .map(RendezVous.Ressource::getEvenementTitre)
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

            List<RendezVous.Ressource> critiques = currentRessources.stream()
                    .filter(this::estEnTension)
                    .sorted(Comparator.comparing(this::scoreOccupationRisque).reversed())
                    .limit(3)
                    .collect(Collectors.toList());

            String noms = critiques.stream()
                    .map(RendezVous.Ressource::getNomRessource)
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

            List<RendezVous.Ressource> base = currentRessources == null ? List.of() : currentRessources;

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

            List<RendezVous.Ressource> base = currentRessources == null ? List.of() : currentRessources;

            GraphicsContext gc = risqueCanvas.getGraphicsContext2D();

            double w = risqueCanvas.getWidth();
            double h = risqueCanvas.getHeight();

            gc.clearRect(0, 0, w, h);

            Map<String, List<RendezVous.Ressource>> grouped = base.stream()
                    .filter(this::estAffectee)
                    .collect(Collectors.groupingBy(r -> safe(r.getEvenementTitre(), "Sans événement")));

            List<Map.Entry<String, List<RendezVous.Ressource>>> top = grouped.entrySet().stream()
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
                Map.Entry<String, List<RendezVous.Ressource>> entry = top.get(i);

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

        private double tauxOccupationEvenement(List<RendezVous.Ressource> ressources) {
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

        private void ouvrirFormulaire(RendezVous.Ressource resEdit) {
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
                    .map(RendezVous.Evenn::getTitre)
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

                    RendezVous.Ressource res = resEdit != null ? resEdit : new RendezVous.Ressource();

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
                                .map(RendezVous.Evenn::getId_Evenn)
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

        private void supprimerRessource(RendezVous.Ressource res) {
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

            List<RendezVous.Ressource> tensions = allRessources.stream()
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

            for (RendezVous.Ressource r : tensions) {
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

        private void ecrirePdfRessources(File file, List<RendezVous.Ressource> ressources) throws IOException {
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

        private String contenuPagePdf(List<RendezVous.Ressource> ressources, int page, int lignesParPage, int pageCount) {
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
                RendezVous.Ressource r = ressources.get(i);

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

        private boolean estAffectee(RendezVous.Ressource r) {
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
        private int quantiteCapacite(RendezVous.Ressource r) {
            return r == null ? 0 : Math.max(0, r.getQuantiteDisponible());
        }

        private int quantiteAffectee(RendezVous.Ressource r) {
            return r == null ? 0 : Math.max(0, r.getQuantiteRequise());
        }

        private int quantiteRestante(RendezVous.Ressource r) {
            return quantiteCapacite(r) - quantiteAffectee(r);
        }

        private boolean estEnTension(RendezVous.Ressource r) {
            return r != null && quantiteAffectee(r) > quantiteCapacite(r);
        }

        private double tauxOccupation(RendezVous.Ressource r) {
            int cap = quantiteCapacite(r);
            return cap > 0 ? Math.min(1.0, (double) quantiteAffectee(r) / cap) : 0.0;
        }

        private double scoreOccupationRisque(RendezVous.Ressource r) {
            if (r == null) return 0;

            double occ = tauxOccupation(r);
            double tension = estEnTension(r) ? 1.0 : 0.0;
            double event = estAffectee(r) ? 0.25 : 0.0;
            double delai = Math.min(1.0, getDelaiSafe(r) / 14.0);

            return 100.0 * (0.50 * occ + 0.25 * tension + 0.15 * delai + 0.10 * event);
        }

        private double coutTotal(RendezVous.Ressource r) {
            return r == null ? 0 : quantiteCapacite(r) * getCoutUnitaireSafe(r);
        }

        /* ========================================================= */
        /* TYPE RESSOURCE                                            */
        /* ========================================================= */

        private ResourceType getTypeRessource(RendezVous.Ressource r) {
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

        private boolean setTypeRessource(RendezVous.Ressource r, String type) {
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

        private ResourceType devinerTypeDepuisNom(RendezVous.Ressource r) {
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

        private int getDelaiSafe(RendezVous.Ressource r) {
            if (r == null || r.getDelaiReapprovisionnementJours() == null) {
                return 3;
            }

            return r.getDelaiReapprovisionnementJours();
        }

        private void setDelaiSafe(RendezVous.Ressource r, int jours) {
            if (r != null) {
                r.setDelaiReapprovisionnementJours(jours);
            }
        }

        private double getCoutUnitaireSafe(RendezVous.Ressource r) {
            return r == null ? 0 : r.getCoutUnitaire();
        }

        private void setCoutUnitaireSafe(RendezVous.Ressource r, double cout) {
            if (r != null) {
                r.setCoutUnitaire(cout);
            }
        }
    }

    public static class ScannerUserController {

        @FXML private TextArea scanInput;
        @FXML private VBox resultBox;
        @FXML private Label resultTitle;
        @FXML private Label resultDetail;
        @FXML private Button voirDetailsBtn;

        private final ServiceEvenn se = new ServiceEvenn();
        private RendezVous.Evenn evenementTrouve;
        private Runnable onEventFoundCallback;

        public void setOnEventFoundCallback(Runnable callback) {
            this.onEventFoundCallback = callback;
        }

        @FXML
        private void scanner() {
            String data = scanInput.getText().trim();
            if (data.isEmpty()) {
                showAlert("Erreur", "Veuillez entrer ou coller un QR code", Alert.AlertType.WARNING);
                return;
            }

            QRCodeService.QRData qrData = QRCodeService.parseQRCode(data);

            if (qrData == null) {
                showAlert("QR Code invalide", "Ce QR code n'est pas reconnu par le système", Alert.AlertType.ERROR);
                return;
            }

            if ("EVENT".equals(qrData.type)) {
                verifierEvenement(qrData.id);
            } else if ("CHECKIN".equals(qrData.type)) {
                afficherMessageCheckin();
            } else {
                showAlert("Erreur", "Type de QR code non reconnu", Alert.AlertType.ERROR);
            }
        }

        private void verifierEvenement(int eventId) {
            try {
                RendezVous.Evenn ev = se.getById(eventId);
                if (ev == null) {
                    showAlert("Erreur", "Événement non trouvé", Alert.AlertType.ERROR);
                    return;
                }

                evenementTrouve = ev;
                resultBox.setVisible(true);
                resultBox.setManaged(true);
                resultTitle.setText("📅 " + ev.getTitre());
                resultTitle.setStyle("-fx-text-fill: #8B1538; -fx-font-weight: bold; -fx-font-size: 14px;");

                String dateStr = ev.getDateEvenement() != null ? ev.getDateEvenement().toString() : "Date non définie";
                String lieuStr = ev.getLieu() != null ? ev.getLieu() : "Lieu non défini";
                String descStr = ev.getDescription() != null ?
                        (ev.getDescription().length() > 100 ? ev.getDescription().substring(0, 100) + "..." : ev.getDescription()) :
                        "Aucune description";

                resultDetail.setText(
                        "📅 Date: " + dateStr + "\n" +
                                "📍 Lieu: " + lieuStr + "\n" +
                                "📝 Description: " + descStr
                );
                resultDetail.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

                voirDetailsBtn.setVisible(true);
                voirDetailsBtn.setOnAction(e -> {
                    fermer();
                    if (onEventFoundCallback != null) {
                        onEventFoundCallback.run();
                    }
                    afficherDetailsComplets(ev);
                });

            } catch (SQLException e) {
                showAlert("Erreur", "Erreur base de données: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }

        private void afficherMessageCheckin() {
            resultBox.setVisible(true);
            resultBox.setManaged(true);
            resultTitle.setText("🎟️ Code de présence");
            resultTitle.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
            resultDetail.setText("Ce QR code est pour valider votre présence à l'événement.\nPrésentez-le à l'entrée.");
            voirDetailsBtn.setVisible(false);
        }

        private void afficherDetailsComplets(RendezVous.Evenn ev) {
            Stage stage = new Stage();
            stage.setTitle("Détails - " + ev.getTitre());
            stage.initModality(Modality.APPLICATION_MODAL);

            VBox root = new VBox(15);
            root.setPadding(new Insets(20));
            root.setStyle("-fx-background-color: white; -fx-border-radius: 15;");

            Label title = new Label(ev.getTitre());
            title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #5A1730;");

            Label dateLabel = new Label("📅 Date: " + (ev.getDateEvenement() != null ? ev.getDateEvenement().toString() : "Non définie"));
            dateLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");

            Label lieuLabel = new Label("📍 Lieu: " + (ev.getLieu() != null ? ev.getLieu() : "Non défini"));
            lieuLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");

            Label descLabel = new Label("📝 Description: " + (ev.getDescription() != null ? ev.getDescription() : "Aucune description"));
            descLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");
            descLabel.setWrapText(true);

            Button btnFermer = new Button("Fermer");
            btnFermer.setStyle("-fx-background-color: #8B1538; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16; -fx-cursor: hand;");
            btnFermer.setOnAction(e -> stage.close());

            root.getChildren().addAll(title, dateLabel, lieuLabel, descLabel, btnFermer);

            Scene scene = new Scene(root, 400, 350);
            stage.setScene(scene);
            stage.show();
        }

        @FXML
        private void effacer() {
            scanInput.clear();
            resultBox.setVisible(false);
            resultBox.setManaged(false);
            resultTitle.setText("");
            resultDetail.setText("");
            evenementTrouve = null;
        }

        @FXML
        private void fermer() {
            Stage stage = (Stage) scanInput.getScene().getWindow();
            stage.close();
        }

        private void showAlert(String title, String content, Alert.AlertType type) {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        }
    }
}