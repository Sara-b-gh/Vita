package com.vita.devora;

import com.vita.devora.Entities.User;
import com.vita.devora.MyDB.MyBD;
import com.vita.devora.Services.UserService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.List;

public class Launcher extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("LoginTest.fxml"));
        primaryStage.setScene(new Scene(root, 700, 500));
        primaryStage.show();
    }

    public static void main(String[] args) {

        // 🔥 IMPORTANT : initialise UNE SEULE connexion
        MyBD.getInstance();

        UserService userService = new UserService();

        // Ajouter u1
        User u1 = new User();
        u1.setNom("Ali");
        u1.setPrenom("Ahmed");
        u1.setEmail("ali@gmail.com");
        u1.setPassword("1234");
        u1.setNumtel(12345678);
        u1.setRole(User.Roles.PATIENT);

        userService.ajouter(u1);

        // Ajouter u2
        User u2 = new User();
        u2.setNom("chaffar");
        u2.setPrenom("ranim");
        u2.setEmail("ranim@gmail.com");
        u2.setPassword("1111");
        u2.setNumtel(12345678);
        u2.setRole(User.Roles.PATIENT);

        userService.ajouter(u2);

        // Affichage
        List<User> userList = userService.getAllUsers();

        for (User user : userList) {
            System.out.println(user);
        }
    }
}