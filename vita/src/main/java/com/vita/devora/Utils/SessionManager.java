package com.vita.devora.Utils;

import com.vita.devora.Entities.user;


/**
 * Garde en mémoire l'utilisateur connecté pendant toute la session.
 * Utilisation :
 *   sessionManager.setCurrentUser(u)   →  à la connexion
 *   sessionManager.getCurrentUser()    →  partout dans l'application
 *   sessionManager.clearSession()      →  à la déconnexion
 */
public class sessionManager {

    private static user currentUser;

    private sessionManager() {}

    public static void setCurrentUser(user user) { currentUser = user; }
    public static user getCurrentUser()           { return currentUser; }
    public static void clearSession()                    { currentUser = null; }

    public static boolean isConnecte() { return currentUser != null; }
    public static boolean isAdmin()    { return isConnecte() && currentUser.getRole() == user.Roles.ADMIN; }
    public static boolean isMedecin()  { return isConnecte() && currentUser.getRole() == user.Roles.DOCTOR; }
    public static boolean isPatient()  { return isConnecte() && currentUser.getRole() == user.Roles.PATIENT; }
}