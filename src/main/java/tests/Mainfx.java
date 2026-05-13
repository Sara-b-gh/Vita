package tests;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Mainfx extends Application {

    private static Stage primaryStage; // Stocker la fenêtre principale

    public static void naviguerVers(String s, String s1) {


    }

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;

        // Charger l'interface principale
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/UserEvenementView.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root);
        primaryStage.setTitle("VITA Hospital");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // Méthode pour changer d'interface (accessible depuis tous les controllers)
    public static void changerInterface(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(Mainfx.class.getResource(fxmlPath));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Erreur: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}