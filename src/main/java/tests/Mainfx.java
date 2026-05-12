package tests;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Mainfx extends Application {

    private static Stage stg;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {

        try {

            stg = primaryStage;

            Parent root = FXMLLoader.load(
                    getClass().getResource("/AfficherRendezVous.fxml")
            );

            Scene scene = new Scene(root);

            primaryStage.setScene(scene);
            primaryStage.setTitle("Rendez-vous");
            primaryStage.show();

        } catch (IOException e) {

            System.out.println(e.getMessage());
        }
    }

    public static void naviguerVers(String fxml, String titre) {

        try {

            Parent root = FXMLLoader.load(
                    Mainfx.class.getResource(fxml)
            );

            Stage stage = new Stage();

            stage.setScene(new Scene(root));

            stage.setTitle(titre);

            stage.show();

        } catch (IOException e) {

            e.printStackTrace();
        }
    }
}