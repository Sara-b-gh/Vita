package com.vita.devora.Controlleurs;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;

public class MainDashboard {

    @FXML
    private StackPane contentArea;

    private Button activeNavButton = null;

    // ✅ SINGLE METHOD (STYLE + NAVIGATION)
    @FXML
    private void onNavClick(ActionEvent event) {
        Button clicked = (Button) event.getSource();

        // =========================
        // 🎨 HANDLE ACTIVE STYLE
        // =========================
        if (activeNavButton != null) {
            activeNavButton.getStyleClass().remove("nav-item-active");

            if (!activeNavButton.getStyleClass().contains("nav-item")) {
                activeNavButton.getStyleClass().add("nav-item");
            }
        }

        clicked.getStyleClass().remove("nav-item");
        if (!clicked.getStyleClass().contains("nav-item-active")) {
            clicked.getStyleClass().add("nav-item-active");
        }

        activeNavButton = clicked;

        // =========================
        // 🔁 HANDLE NAVIGATION
        // =========================
        String text = clicked.getText();

        switch (text) {
            case "Tableau de bord":
                loadPage("/com/vita/devora/views/AdminDashboard.fxml");
                break;

            case "Espace Docteurs":
                loadPage("/com/vita/devora/views/Doctors.fxml");
                break;

            case "Espace Patients":
                loadPage("/com/vita/devora/views/Patients.fxml");
                break;

            case "Espace Rendez-vous":
                loadPage("/com/vita/devora/views/Appointments.fxml");
                break;

            case "Les Événements":
                loadPage("/com/vita/devora/views/Events.fxml");
                break;

            case "Quiz":
                loadPage("/com/vita/devora/views/Quizzes.fxml");
                break;

            default:
                System.out.println("No page linked to: " + text);
        }
    }

    // =========================
    // 🎯 HOVER EFFECT
    // =========================
    @FXML
    private void onNavHover(MouseEvent event) {
        Button btn = (Button) event.getSource();

        if (!btn.getStyleClass().contains("nav-item-active")) {
            btn.getStyleClass().add("nav-item-hover");
        }
    }

    @FXML
    private void onNavExit(MouseEvent event) {
        Button btn = (Button) event.getSource();
        btn.getStyleClass().remove("nav-item-hover");
    }

    // =========================
    // 📄 PAGE LOADER
    // =========================
    private void loadPage(String path) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource(path));
            contentArea.getChildren().setAll(view);
        } catch (Exception e) {
            System.out.println("❌ Error loading page: " + path);
            e.printStackTrace();
        }
    }

    // =========================
    // 🚀 LOAD DEFAULT PAGE
    // =========================
    @FXML
    public void initialize() {
        loadPage("/com/vita/devora/views/AdminDashboard.fxml");
    }
}