package tests;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Mainfx extends Application {

    public static Stage primaryStage;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        naviguerVers("/UserEvenementView" +
                ".fxml", "Événements - Hospital Management");
    }

    public static void naviguerVers(String fxmlPath, String titre) {
        try {
            var url = Mainfx.class.getResource(fxmlPath);
            System.out.println("URL FXML = " + url);

            if (url == null) {
                System.out.println("FICHIER INTROUVABLE : " + fxmlPath);
                return;
            }
            // Dans Mainfx.java ligne 34

            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();
            Scene scene = new Scene(root);

            primaryStage.setTitle(titre);
            primaryStage.setScene(scene);
            primaryStage.setWidth(1200);
            primaryStage.setHeight(750);
            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}