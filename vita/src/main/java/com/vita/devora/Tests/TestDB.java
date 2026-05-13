package com.vita.devora.Tests;

import com.vita.devora.MyDB.MyDB;

import java.sql.Connection;

public class TestDB {
    public static void main(String[] args) throws Exception {
        Connection conn = MyDB.getInstance().getConnection();

        if (conn != null) {
            System.out.println("Connection successful!");
        } else {
            System.out.println("Connection failed.");
        }
    }
}