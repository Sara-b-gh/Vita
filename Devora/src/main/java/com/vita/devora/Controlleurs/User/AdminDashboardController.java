package com.vita.devora.Controlleurs;

import com.vita.devora.Entities.User;
import com.vita.devora.Services.UserService;
import com.vita.devora.utils.SessionManager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.Label;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class AdminDashboardController {

    @FXML private Label totalMedecinsLabel;
    @FXML private Label totalPatientsLabel;
    @FXML private Label totalQuizLabel;
    @FXML private Label totalInteractionsLabel;
    @FXML private Label totalEvenementsLabel;

    @FXML private PieChart deptChart;

    @FXML private BarChart<String, Number>  quizChart;
    @FXML private CategoryAxis              quizXAxis;
    @FXML private NumberAxis                quizYAxis;

    @FXML private LineChart<String, Number> evolutionChart;
    @FXML private CategoryAxis              evoXAxis;
    @FXML private NumberAxis                evoYAxis;

    private final UserService userService = new UserService();

    @FXML
    public void initialize() {
        loadKPIs();
        loadDeptChart();
        loadQuizChart();
        loadEvolutionChart();
        styleCharts();
    }

    // ══════════════════════════════════════
    //  KPI
    // ══════════════════════════════════════
    private void loadKPIs() {
        try {
            int medecins = userService.getByRole(User.Roles.DOCTOR).size();
            int patients = userService.getByRole(User.Roles.PATIENT).size();

            totalMedecinsLabel.setText(String.valueOf(medecins));
            totalPatientsLabel.setText(String.valueOf(patients));

            // ── Quiz & Événements : remplacez par vos vrais services ──
            // int quiz         = quizService.getAll().size();
            // int interactions = quizService.getTotalInteractions();
            // int evenements   = evenementService.getAll().size();
            // Pour l'instant valeurs de démonstration :
            totalQuizLabel.setText("9");
            totalInteractionsLabel.setText("312");
            totalEvenementsLabel.setText("7");

        } catch (Exception e) {
            e.printStackTrace();
            totalMedecinsLabel.setText("—");
            totalPatientsLabel.setText("—");
        }
    }

    // ══════════════════════════════════════
    //  Graphique 1 — Médecins par département
    // ══════════════════════════════════════
    private void loadDeptChart() {

        try {
            List<User> medecins = userService.getDoctors();

            Map<String, Long> byDept = medecins.stream()
                    .filter(u -> u.getDepartement() != null && !u.getDepartement().isEmpty())
                    .collect(java.util.stream.Collectors.groupingBy(
                            User::getDepartement,
                            java.util.stream.Collectors.counting()
                    ));

            int totalDoctors = medecins.size();

            ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();

            if (byDept.isEmpty()) {
                pieData.add(new PieChart.Data("No data", 1));
            } else {
                byDept.forEach((dept, count) ->
                        pieData.add(new PieChart.Data(
                                dept + " (" + String.format("%.1f", (count * 100.0 / totalDoctors)) + "%)",
                                count
                        ))
                );
            }

            deptChart.setData(pieData);

            deptChart.setClockwise(true);
            deptChart.setLabelsVisible(true);
            deptChart.setStartAngle(90);
            deptChart.setLegendVisible(true);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════
    //  Graphique 2 — Interactions quiz
    // ══════════════════════════════════════
    private void loadQuizChart() {
        // Remplacez par vos vraies données de QuizService
        XYChart.Series<String, Number> sReponses  = new XYChart.Series<>();
        XYChart.Series<String, Number> sCompletes = new XYChart.Series<>();

        sReponses.setName("Réponses");
        sCompletes.setName("Complétés");

        String[] semaines = {"Sem 1", "Sem 2", "Sem 3", "Sem 4"};
        int[]    reponses  = {40, 65, 80, 127};
        int[]    completes = {22, 38, 55, 90};

        for (int i = 0; i < semaines.length; i++) {
            sReponses.getData().add(new XYChart.Data<>(semaines[i], reponses[i]));
            sCompletes.getData().add(new XYChart.Data<>(semaines[i], completes[i]));
        }

        quizChart.getData().addAll(sReponses, sCompletes);
        applyBarColor(quizChart, "#854F0B", "#EF9F27");
    }

    // ══════════════════════════════════════
    //  Graphique 3 — Évolution mensuelle
    // ══════════════════════════════════════
    private void loadEvolutionChart() {
        try {
            // Remplacez par vos vraies données mensuelles
            List<User> patients = userService.getByRole(User.Roles.PATIENT);
            List<User> medecins = userService.getByRole(User.Roles.DOCTOR);

            XYChart.Series<String, Number> sPatients = new XYChart.Series<>();
            XYChart.Series<String, Number> sMedecins = new XYChart.Series<>();

            sPatients.setName("Patients");
            sMedecins.setName("Médecins");

            // Données de démonstration — remplacez par vos requêtes SQL mensuelles
            String[] mois     = {"Déc", "Jan", "Fév", "Mar", "Avr", "Mai"};
            int[]    patData  = {100, 108, 115, 124, 136, patients.size()};
            int[]    medData  = {20,  21,  21,  22,  23,  medecins.size()};

            for (int i = 0; i < mois.length; i++) {
                sPatients.getData().add(new XYChart.Data<>(mois[i], patData[i]));
                sMedecins.getData().add(new XYChart.Data<>(mois[i], medData[i]));
            }

            evolutionChart.getData().addAll(sPatients, sMedecins);

            // Couleurs des lignes après rendu
            evolutionChart.setOnMouseEntered(null);
            javafx.application.Platform.runLater(() -> {
                if (!evolutionChart.getData().isEmpty()) {
                    String css0 = "-fx-stroke: #185FA5;";
                    String css1 = "-fx-stroke: #8a0037;";
                    evolutionChart.getData().get(0).getNode().setStyle(css0);
                    evolutionChart.getData().get(1).getNode().setStyle(css1);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════
    //  Helpers styles
    // ══════════════════════════════════════
    private void styleCharts() {

        if (deptChart != null) {
            deptChart.setStyle("-fx-background-color: transparent;");

            if (deptChart.lookup(".chart-plot-background") != null) {
                deptChart.lookup(".chart-plot-background")
                        .setStyle("-fx-background-color: transparent;");
            }
        }

        if (quizChart != null) {
            quizChart.setStyle("-fx-background-color: transparent;");

            if (quizChart.lookup(".chart-plot-background") != null) {
                quizChart.lookup(".chart-plot-background")
                        .setStyle("-fx-background-color: transparent;");
            }
        }

        if (evolutionChart != null) {
            evolutionChart.setStyle("-fx-background-color: transparent;");

            if (evolutionChart.lookup(".chart-plot-background") != null) {
                evolutionChart.lookup(".chart-plot-background")
                        .setStyle("-fx-background-color: transparent;");
            }
        }
    }

    /**
     * Applique une couleur à chaque série d'un BarChart après le rendu.
     * @param chart   le BarChart cible
     * @param colors  une couleur hex par série (dans l'ordre)
     */
    private void applyBarColor(BarChart<?, ?> chart, String... colors) {
        javafx.application.Platform.runLater(() -> {
            for (int s = 0; s < chart.getData().size() && s < colors.length; s++) {
                final String color = colors[s];
                for (XYChart.Data<?, ?> d : chart.getData().get(s).getData()) {
                    if (d.getNode() != null) {
                        d.getNode().setStyle(
                                "-fx-bar-fill: " + color + ";" +
                                        "-fx-background-radius: 4 4 0 0;"
                        );
                    }
                }
            }
        });
    }

    @FXML
    public void navEspaceDoctor(ActionEvent actionEvent) {
        // Ensure the path matches your project structure exactly
        switchPage(actionEvent, "/com/vita/devora/AdminDocteur.fxml");
    }

    @FXML
    public void NavEspacePatient(ActionEvent actionEvent) {
        switchPage(actionEvent, "/com/vita/devora/AdminPatient.fxml");
    }

//    @FXML
//    public void TabBOARD(ActionEvent actionEvent) {
//        // Fixed potential typo: changed "Dashbord" to "Dashboard"
//        // Double-check your actual filename!
//        switchPage(actionEvent, "/com/vita/devora/AdminDashboard.fxml");
//    }

    /**
     * Refined switchPage method
     */
    @FXML
    private void haddledeconnexion(ActionEvent actionEvent) {
        try {
            SessionManager.clearSession();

            java.net.URL resource = getClass().getResource("/com/vita/devora/LoginTest.fxml");
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(resource);
            javafx.scene.Parent root = loader.load();

            javafx.scene.Node sourceNode = (javafx.scene.Node) actionEvent.getSource();
            javafx.stage.Stage stage = (javafx.stage.Stage) sourceNode.getScene().getWindow();
            stage.setResizable(false);
            stage.setWidth(1000);
            stage.setHeight(720);
            stage.centerOnScreen();

            stage.getScene().setRoot(root);

        } catch (Exception e) {
            e.printStackTrace();
            new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.ERROR,
                    "Impossible de se déconnecter."
            ).showAndWait();
        }
    }
    private void switchPage(ActionEvent event, String fxmlPath) {
        try {
            // 1. Check if resource exists before loading to avoid generic IOExceptions
            java.net.URL resource = getClass().getResource(fxmlPath);
            if (resource == null) {
                System.err.println("❌ FXML File not found at: " + fxmlPath);
                return;
            }

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(resource);
            javafx.scene.Parent root = loader.load();

            // 2. Get the Stage safely
            javafx.scene.Node sourceNode = (javafx.scene.Node) event.getSource();
            javafx.stage.Stage stage = (javafx.stage.Stage) sourceNode.getScene().getWindow();

            // 3. Set the new root
            stage.getScene().setRoot(root);

            // Optional: If you want to ensure the window adjusts to the new size
            // stage.sizeToScene();

        } catch (java.io.IOException e) {
            System.err.println("❌ Critical error loading: " + fxmlPath);
            e.printStackTrace();
        }


    }


}