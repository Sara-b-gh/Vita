package com.vita.devora;

import com.vita.devora.Entities.User;
import com.vita.devora.MyDB.MyBD;
import com.vita.devora.Services.UserService;
import javafx.application.Application;

import java.sql.SQLException;

public class Launcher {
    public static void main(String[] args) throws SQLException {
        MyBD bd = new MyBD() ;
        UserService userService = new UserService();
        User user = userService.getUserById(1);
        userService.afficher();
        //if( user != null) {
        //    user.setNom("SAL");
         //   user.setPrenom("Sucre");
           // userService.modifier(user);
        }



}
