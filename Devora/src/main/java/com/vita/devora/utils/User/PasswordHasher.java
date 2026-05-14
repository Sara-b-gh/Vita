package com.vita.devora.utils;

import java.security.MessageDigest;

public class PasswordHasher {

    public static String hash(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = md.digest(password.getBytes("UTF-8"));

            StringBuilder sb = new StringBuilder();
            for (byte b : hashedBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();

        } catch (Exception e) {
            throw new RuntimeException("Erreur de hachage", e);
        }
    }

    public static boolean verify(String password, String hashedPassword) {
        return hash(password).equals(hashedPassword);
    }
}