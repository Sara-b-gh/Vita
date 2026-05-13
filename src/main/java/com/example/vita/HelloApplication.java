package com.example.vita;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(
                HelloApplication.class.getResource("/QuizView.fxml")
        );
        Scene scene = new Scene(fxmlLoader.load(), 1100, 650);
        stage.setTitle("Gestion des Quiz - VitaSanté");
        stage.setScene(scene);
        stage.show();
    }
}
