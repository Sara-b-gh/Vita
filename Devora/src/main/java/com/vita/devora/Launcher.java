package com.vita.devora;

import com.vita.devora.Entities.User;
import com.vita.devora.MyDB.MyBD;
import com.vita.devora.Services.UserService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

public class Launcher extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("LoginTest.fxml"));
        primaryStage.setScene(new Scene(root, 700, 500));
        primaryStage.show();
    }

    public static void main(String[] args) throws SQLException {

        // 🔥 IMPORTANT : initialise UNE SEULE connexion
        MyBD.getInstance();

        UserService userService = new UserService();



        // Ajouter u2
        User u2 = new User();
        u2.setId(12345678);
        u2.setNom("BLOO");
        u2.setPrenom("ranim");
        u2.setEmail("mor9@gmail.com");
        u2.setPassword("1111");
        u2.setNumtel(98765432);
        u2.setRole(User.Roles.PATIENT);
        u2.setDateNaissance((LocalDate.of(1995, 6, 15)));

        userService.ajouter(u2);

        // Affichage
        List<User> userList = userService.getAllUsers();

        for (User user : userList) {
            System.out.println(user);
        }
    }

}