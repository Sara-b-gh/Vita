package com.vita.devora;

import com.vita.devora.Entities.User;
import com.vita.devora.MyDB.MyBD;
import com.vita.devora.Services.UserService;
import javafx.application.Application;

import java.sql.SQLException;
import java.util.List;

public class Launcher {
    public static void main(String[] args) throws SQLException {
        MyBD bd = new MyBD () ;

        UserService userService = new UserService();
        User u1 = new User();
        u1.setNom("Ali");
        u1.setPrenom("Ahmed");
        u1.setEmail("ali@gmail.com");
        u1.setPassword("1234");
        u1.setNumtel(12345678);
        u1.setRole(User.Roles.USER);
        userService.ajouter(u1);

        User u2 = new User();
        u2.setNom("chaffar");
        u2.setPrenom("ranim");
        u2.setEmail("ranim@gmail.com");
        u2.setPassword("1111");
        u2.setNumtel(12345678);
        u2.setRole(User.Roles.USER);
        userService.ajouter(u2);

        List<User> userList = userService.getAllUsers();
        for(User user :userList ){
            System.out.println(user);
        }



        //User user = userService.getUserById(1);
        //userService.getAllUsers();

        //if( user != null) {
        //    user.setNom("SAL");
        //   user.setPrenom("Sucre");
        // userService.modifier(user);
    }



}