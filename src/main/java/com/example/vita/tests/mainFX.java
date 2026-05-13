package com.example.vita.tests;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.net.URL;

public class mainFX extends Application {

    @Override
    public void start(Stage stage) {
        try {
            System.out.println("=== DEMARRAGE APPLICATION VITA - MEDICAMENTS ===");

            // Vérifier plusieurs emplacements possibles du fichier FXML
            String[] fxmlPaths = {
                    "/UserMedicamentView.fxml",                    // Racine des ressources
                    "/com/example/vita/UserMedicamentView.fxml",   // Package complet
                    "/view/UserMedicamentView.fxml",               // Dossier view
                    "/fxml/UserMedicamentView.fxml"                // Dossier fxml
            };

            URL fxmlUrl = null;
            for (String path : fxmlPaths) {
                fxmlUrl = getClass().getResource(path);
                if (fxmlUrl != null) {
                    System.out.println("Fichier FXML trouvé: " + path);
                    break;
                }
            }

            if (fxmlUrl == null) {
                System.err.println("ERREUR: Fichier UserMedicamentView.fxml non trouvé!");
                System.err.println("Vérifiez que le fichier est dans le dossier resources/");
                return;
            }

            // Charger le FXML
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            System.out.println("FXML chargé avec succès!");

            // Créer la scène
            Scene scene = new Scene(root);

            // Charger le CSS s'il existe
            String[] cssPaths = {
                    "/STYLE1.css",
                    "/com/example/vita/style.css",
                    "/css/style.css"
            };

            for (String cssPath : cssPaths) {
                URL cssUrl = getClass().getResource(cssPath);
                if (cssUrl != null) {
                    scene.getStylesheets().add(cssUrl.toExternalForm());
                    System.out.println("CSS chargé: " + cssPath);
                    break;
                }
            }

            // Configurer la fenêtre
            stage.setTitle("💊 Gestion des Médicaments - VITA HOPITAL");
            stage.setScene(scene);
            stage.setWidth(1300);
            stage.setHeight(800);
            stage.setMinWidth(1000);
            stage.setMinHeight(600);
            stage.centerOnScreen();
            stage.show();

            System.out.println("=== APPLICATION MEDICAMENTS DEMARRÉE AVEC SUCCÈS ===");

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