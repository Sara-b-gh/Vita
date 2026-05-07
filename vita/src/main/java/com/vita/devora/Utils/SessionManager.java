package com.vita.devora.Utils;

import com.vita.devora.Entities.User;


/**
 * Garde en mémoire l'utilisateur connecté pendant toute la session.
 * Utilisation :
 *   SessionManager.setCurrentUser(u)   →  à la connexion
 *   SessionManager.getCurrentUser()    →  partout dans l'application
 *   SessionManager.clearSession()      →  à la déconnexion
 */
public class SessionManager {

    private static User currentUser;

    private SessionManager() {}

    public static void setCurrentUser(User user) { currentUser = user; }
    public static User getCurrentUser()           { return currentUser; }
    public static void clearSession()                    { currentUser = null; }

    public static boolean isConnecte() { return currentUser != null; }
    public static boolean isAdmin()    { return isConnecte() && currentUser.getRole() == User.Roles.ADMIN; }
    public static boolean isMedecin()  { return isConnecte() && currentUser.getRole() == User.Roles.DOCTOR; }
    public static boolean isPatient()  { return isConnecte() && currentUser.getRole() == User.Roles.PATIENT; }
}