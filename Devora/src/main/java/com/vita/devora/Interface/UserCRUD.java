package com.vita.devora.Interface;

import com.vita.devora.Entities.User;

import java.sql.SQLException;
import java.util.List;

public interface UserCRUD <T>{

    public void ajouter( T t) throws SQLException;
    public void modifier(T t)throws SQLException;
    public void supprimer(int id)throws SQLException;
    public List<User> getAllUsers();
}
