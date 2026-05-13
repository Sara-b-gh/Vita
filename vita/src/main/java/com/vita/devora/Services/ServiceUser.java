package com.vita.devora.Services;

import com.vita.devora.Entities.user;
import com.vita.devora.Interfaces.InterfaceUser;
import com.vita.devora.MyDB.MyDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ServiceUser implements InterfaceUser <user>{

    private Connection conn;

    public ServiceUser() {
        conn = MyDB.getInstance().getConnection();
    }

    @Override
    public user login(String email, String password) {
        String sql = "SELECT * FROM users WHERE email = ? AND password = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                user user = new user();
                user.setId(rs.getInt("id"));
                user.setNom(rs.getString("nom"));
                user.setPrenom(rs.getString("prenom"));
                user.setEmail(rs.getString("email"));
                user.setRole(com.vita.devora.Entities.user.Roles.valueOf(rs.getString("role").toUpperCase()));
                return user;
            }
        } catch (SQLException e) {
            System.err.println("Login Error: " + e.getMessage());
        }
        return null;
    }
}
