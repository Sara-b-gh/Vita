package com.vita.devora;

import com.vita.devora.MyDB.MyBD;
import com.vita.devora.Services.UserService;
import com.vita.devora.utils.LocalServer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.sql.SQLException;

public class Launcher extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            LocalServer.start();
            System.out.println("✅ Serveur OK");
        } catch (Exception e) {
            System.out.println("❌ Erreur serveur : " + e.getMessage());
            e.printStackTrace();
        }

        Parent root = FXMLLoader.load(getClass().getResource("LoginTest.fxml"));
        primaryStage.setScene(new Scene(root, 1280, 720));
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        LocalServer.stop(); // ← AJOUTEZ CETTE LIGNE
    }

    public static void main(String[] args) throws SQLException {
        MyBD.getInstance();
        launch(args); // ← REMPLACEZ tout le contenu par juste ça
    }

}