package com.vita.devora.utils;

import java.security.SecureRandom;

/**
 * Génère un mot de passe aléatoire sécurisé.
 * Garantit : 1 majuscule + 1 chiffre + 1 caractère spécial minimum.
 */
public class PasswordGenerator {

    private static final String MAJUSCULES = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String MINUSCULES = "abcdefghijklmnopqrstuvwxyz";
    private static final String CHIFFRES   = "0123456789";
    private static final String SPECIAUX   = "@#!$%";
    private static final String TOUS       = MAJUSCULES + MINUSCULES + CHIFFRES + SPECIAUX;

    private static final SecureRandom random = new SecureRandom();

    public static String generer(int longueur) {
        if (longueur < 8) longueur = 8;
        StringBuilder sb = new StringBuilder();

        // Garantir au moins 1 de chaque type
        sb.append(MAJUSCULES.charAt(random.nextInt(MAJUSCULES.length())));
        sb.append(CHIFFRES  .charAt(random.nextInt(CHIFFRES.length())));
        sb.append(SPECIAUX  .charAt(random.nextInt(SPECIAUX.length())));

        // Remplir le reste
        for (int i = 3; i < longueur; i++) {
            sb.append(TOUS.charAt(random.nextInt(TOUS.length())));
        }
        return melanger(sb.toString());
    }

    private static String melanger(String input) {
        char[] chars = input.toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char tmp = chars[i];
            chars[i] = chars[j];
            chars[j] = tmp;
        }
        return new String(chars);
    }
}