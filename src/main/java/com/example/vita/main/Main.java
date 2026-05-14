package main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.util.Objects;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        try {
            Parent root = FXMLLoader.load(
                    Objects.requireNonNull(
                            getClass().getResource("/Main-view.fxml")
                    )
            );
            Scene scene = new Scene(root);
            stage.setTitle("VITA - Gestion Medicale");
            stage.setScene(scene);
            stage.setWidth(1100);
            stage.setHeight(737);
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            System.err.println("Erreur : " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}