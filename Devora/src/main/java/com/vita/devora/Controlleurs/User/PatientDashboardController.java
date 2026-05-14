package com.vita.devora.Controlleurs;

import com.vita.devora.Entities.User;
import com.vita.devora.Entities.WhoIndicator;
import com.vita.devora.utils.SessionManager;
import com.vita.devora.utils.WhoService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.List;

public class PatientDashboardController {

    @FXML private BorderPane rootPane;
    @FXML private Label welcomeLabel;
    @FXML private Label loadingLabel;
    @FXML private VBox whoContainer;

    @FXML private Label profileNameLabel;
    @FXML private Label profileEmailLabel;
    @FXML private Label profileRoleLabel;
    @FXML private VBox profileBox;

    @FXML
    public void initialize() {
        User current = SessionManager.getCurrentUser();
        if (current != null) {
            welcomeLabel.setText("Bienvenue " + current.getPrenom() + " " + current.getNom());
        }
        loadWhoData();
    }

    // ══════════════════════════════════════
    // WHO DATA
    // ══════════════════════════════════════
    private void loadWhoData() {
        loadingLabel.setText("⏳ Chargement des statistiques WHO...");

        Thread thread = new Thread(() -> {
            try {
                List<WhoIndicator> indicators = WhoService.fetchIndicators();

                Platform.runLater(() -> {
                    loadingLabel.setText("");
                    whoContainer.getChildren().clear();

                    for (WhoIndicator ind : indicators) {
                        whoContainer.getChildren().add(createIndicatorCard(ind));
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() ->
                        loadingLabel.setText("❌ Impossible de charger les données WHO.")
                );
                e.printStackTrace();
            }
        });

        thread.setDaemon(true);
        thread.start();
    }

    private HBox createIndicatorCard(WhoIndicator indicator) {

        int index = getIndicatorIndex(indicator.getCode());

        String cardClass = "who-card";
        String valueClass = "who-value";
        String unitClass = "who-unit";

        if (index == 1) {
            cardClass = "who-card-vert";
            valueClass = "who-value-vert";
            unitClass = "who-unit-vert";
        } else if (index == 2) {
            cardClass = "who-card-bleu";
            valueClass = "who-value-bleu";
            unitClass = "who-unit-bleu";
        } else if (index == 3 || index == 4) {
            cardClass = "who-card-ambre";
            valueClass = "who-value-ambre";
            unitClass = "who-unit-ambre";
        }

        Label value = new Label(indicator.getValue());
        value.getStyleClass().addAll("who-value", valueClass);

        Label name = new Label(indicator.getName());
        name.getStyleClass().add("who-name");
        name.setMaxWidth(650);

        Label unitLabel = new Label(getUnit(indicator.getCode()));
        unitLabel.getStyleClass().addAll("who-unit", unitClass);

        Label source = new Label("📊 Source : WHO Global Health Observatory — Tunisie (2021)");
        source.getStyleClass().add("who-source");

        VBox info = new VBox(6, name, unitLabel, source);

        HBox card = new HBox(20, value, info);
        card.getStyleClass().add(cardClass);
        card.setPadding(new Insets(16));

        return card;
    }

    private int getIndicatorIndex(String code) {
        String[] codes = {
                "WHOSIS_000001",
                "WHOSIS_000007",
                "MDG_0000000001",
                "NCD_BMI_30C",
                "M_Est_cig_curr_std"
        };
        for (int i = 0; i < codes.length; i++) {
            if (codes[i].equals(code)) return i;
        }
        return -1;
    }

    private String getUnit(String code) {
        switch (code) {
            case "WHOSIS_000001": return "📅 Années — Toutes causes confondues";
            case "WHOSIS_000007": return "📅 Années en bonne santé";
            case "MDG_0000000001": return "📉 Pour 1 000 naissances vivantes";
            case "NCD_BMI_30C": return "📊 % de la population adulte";
            case "M_Est_cig_curr_std": return "🚬 % de fumeurs actuels";
            default: return "";
        }
    }

    // ══════════════════════════════════════
    // NAVIGATION
    // ══════════════════════════════════════
    @FXML
    private void handlePatDash(ActionEvent event) {
        // déjà sur cette page
    }

    @FXML
    private void handleProfile(ActionEvent event) {
        switchPage(event, "/com/vita/devora/PatientPassword.fxml");
    }

    @FXML
    private void handleLogout() {
        try {
            SessionManager.clearSession();
            Parent root = FXMLLoader.load(getClass().getResource("/com/vita/devora/LoginTest.fxml"));
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();

            // ← REBLOQUER pour le login
            stage.setResizable(false);
            stage.setWidth(1280);
            stage.setHeight(720);
            stage.centerOnScreen();

            stage.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void switchPage(ActionEvent event, String fxmlPath) {
        try {
            java.net.URL resource = getClass().getResource(fxmlPath);
            if (resource == null) {
                System.err.println("❌ FXML File not found at: " + fxmlPath);
                return;
            }
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(resource);
            javafx.scene.Parent root = loader.load();
            javafx.scene.Node sourceNode = (javafx.scene.Node) event.getSource();
            javafx.stage.Stage stage = (javafx.stage.Stage) sourceNode.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (java.io.IOException e) {
            System.err.println("❌ Critical error loading: " + fxmlPath);
            e.printStackTrace();
        }
    }
}