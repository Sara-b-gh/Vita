package com.vita.devora;

//import utils.SessionManager;

import com.vita.devora.Entities.User;
import com.vita.devora.Services.UserService;
import com.vita.devora.utils.EmailSender;
import com.vita.devora.utils.PasswordGenerator;

import javax.management.relation.Role;
import java.sql.SQLException;
import java.util.List;

/**
 * Test complet du module Utilisateur.
 * Même style que Main.java de la prof.
 *
 * ⚠️  Avant d'exécuter :
 *   1. Créer la table dans MySQL (script ci-dessous)
 *   2. Configurer EMAIL_EXPEDITEUR et APP_PASSWORD dans EmailSender.java
 *
 * -- Script SQL à exécuter dans MySQL Workbench :
 * USE esprit3a27;
 * CREATE TABLE IF NOT EXISTS utilisateur (
 *     id           INT AUTO_INCREMENT PRIMARY KEY,
 *     nom          VARCHAR(50)  NOT NULL,
 *     prenom       VARCHAR(50)  NOT NULL,
 *     email        VARCHAR(100) NOT NULL UNIQUE,
 *     mot_de_passe VARCHAR(255) NOT NULL,
 *     telephone    VARCHAR(20),
 *     role         ENUM('ADMIN','MEDECIN','PATIENT') NOT NULL,
 *     actif        TINYINT(1) DEFAULT 1
 * );
 * INSERT INTO utilisateur (nom,prenom,email,mot_de_passe,telephone,role,actif)
 * VALUES ('Admin','Système','admin@hopital.tn','Admin@123','+216 00 000 000','ADMIN',1);
 */
public class TestMail {

    public static void main(String[] args) throws SQLException {

        UserService service = new UserService();

        // ══════════════════════════════════════════════
        //  TEST 1 — Créer un médecin + envoyer email
        // ══════════════════════════════════════════════
        System.out.println("\n===== TEST 1 : CRÉER MÉDECIN + EMAIL =====");

        String nom       = "Ammar";
        String prenom    = "Chawki";
        String email     = "morched99@yahoo.com"; // ← mettre un vrai email
        int telephone = 55111222;
        User.Roles role      = User.Roles.DOCTOR;

        String motDePasse = PasswordGenerator.generer(10);
        System.out.println("Mot de passe généré : " + motDePasse);

        User medecin = new User(nom, prenom, email,
                motDePasse, telephone, role);
        service.ajouter(medecin);
        EmailSender.envoyerCredentials(
                email,
                prenom + " " + nom,
                "Médecin",
                motDePasse
        );
        try {
            // Vérifier doublon
            if (service.getByEmail(email) != null) {
                System.out.println("⚠️  Email déjà utilisé !");
            } else {

                System.out.println("✅ email non envoyer !");
            }
            //} catch (SQLException e) {
            //  System.out.println("Erreur SQL : " + e.getMessage());
        } catch (RuntimeException e) {
            //System.out.println("⚠️  Compte créé mais email non envoyé.");
            //System.out.println("   → Email    : " + email);
            System.out.println(e);
        }

        // ══════════════════════════════════════════════
        //  TEST 2 — Créer un patient + envoyer email
        // ══════════════════════════════════════════════
        System.out.println("\n===== TEST 2 : CRÉER PATIENT + EMAIL =====");

        String mdpPatient = PasswordGenerator.generer(10);
        User patient = new User(
                "Chaari", "Khalil",
                "morcheda62@gmail.com",  // ← mettre un vrai email
                mdpPatient,
                99333444,
                User.Roles.PATIENT
        );
        //try {
        service.ajouter(patient);
        EmailSender.envoyerCredentials(
                patient.getEmail(),
                patient.getPrenom() + " " + patient.getNom(),
                "Patient",
                mdpPatient
        );
        System.out.println("✅ Patient créé et email envoyé !");
        //} catch (SQLException e) {
        //  System.out.println("Erreur SQL : " + e.getMessage());
        //} catch (RuntimeException e) {
        //  System.out.println("⚠️  Email non envoyé — credentials : "
        //           + patient.getEmail() + " / " + mdpPatient);
        // }

        // ══════════════════════════════════════════════
        //  TEST 3 — Afficher tous les utilisateurs
        // ══════════════════════════════════════════════
        System.out.println("\n===== TEST 3 : AFFICHER TOUS =====");
        List<User> tous = service.getAllUsers();
        tous.forEach(System.out::println);



        // ══════════════════════════════════════════════
        //  TEST 5 — Filtrer par rôle
        // ══════════════════════════════════════════════
        System.out.println("\n===== TEST 5 : MÉDECINS SEULEMENT =====");
        try {
            service.getByRole(User.Roles.DOCTOR).forEach(System.out::println);
        } catch (SQLException e) {
            System.out.println("Erreur : " + e.getMessage());
        }
    }
}