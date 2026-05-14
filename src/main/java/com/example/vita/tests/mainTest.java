package tests;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.net.URL;

public class mainTest extends Application {

    @Override
    public void start(Stage stage) {
        try {
            System.out.println("=== DEMARRAGE APPLICATION VITA - MEDICAMENTS ===");

            // Option 1: Chemin simple (si FXML à la racine des ressources)
            String fxmlPath = "/UserMedicamentView.fxml";

            // Option 2: Chemin complet (si dans package com/example/vita)
            // String fxmlPath = "/com/example/vita/UserMedicamentView.fxml";

            URL fxmlUrl = getClass().getResource(fxmlPath);

            if (fxmlUrl == null) {
                System.err.println("Fichier FXML non trouvé: " + fxmlPath);
                System.err.println("Chemins recherchés :");
                System.err.println("- " + getClass().getResource("/").getPath());
                return;
            }

            System.out.println("Chargement du FXML depuis: " + fxmlUrl.getPath());

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            System.out.println("FXML chargé avec succès");

            Scene scene = new Scene(root);

            // Appliquer le CSS s'il existe
            String cssPath = "/STYLE1.css";
            URL cssUrl = getClass().getResource(cssPath);
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
                System.out.println("CSS chargé: " + cssPath);
            } else {
                System.out.println("CSS non trouvé: " + cssPath);
            }

            stage.setTitle("Espace Médicaments - VITA HOPITAL");
            stage.setScene(scene);
            stage.setWidth(1300);
            stage.setHeight(800);
            stage.setMinWidth(1000);
            stage.setMinHeight(600);
            stage.centerOnScreen();
            stage.show();

            System.out.println("=== APPLICATION DEMARREE AVEC SUCCES ===");

        } catch (Exception e) {
            System.err.println("=== ERREUR DE CHARGEMENT ===");
            System.err.println("Message: " + e.getMessage());
            System.err.println("Cause: " + (e.getCause() != null ? e.getCause().getMessage() : "N/A"));
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}