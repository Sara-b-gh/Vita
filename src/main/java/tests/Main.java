package tests;

import entities.Evenn;
import services.ServiceEvenn;
import utils.MyBD;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class Main {

    public static void main(String[] args) {

        ServiceEvenn se = new ServiceEvenn();

        try {
            Connection cnx = MyBD.getInstance().getCnx();

            // Nettoyer la table evenn
            cnx.createStatement().executeUpdate("DELETE FROM evenn");
            cnx.createStatement().executeUpdate("ALTER TABLE evenn AUTO_INCREMENT = 1");

            System.out.println("=== BD NETTOYEE ===");

            // Créer un événement de test
            Evenn evenement = new Evenn(
                    1,  // idRessource (exemple: 1)
                    "Conférence sur la santé",  // titre
                    LocalDateTime.now().plusDays(7),  // dateEvenement (dans 7 jours)
                    "Une conférence importante sur les innovations médicales",  // description
                    "Salle A - Centre de Conférences"  // lieu
            );

            // Ajouter l'événement
            se.add(evenement);

            System.out.println("OK INSERT ÉVÉNEMENT");

            // Afficher tous les événements
            System.out.println("\n=== LISTE DES ÉVÉNEMENTS ===");
            se.getAll().forEach(ev ->
                    System.out.println(ev.getId_Evenn() + " | " + ev.getTitre() + " | " + ev.getLieu())
            );

        } catch (SQLException e) {
            System.out.println("Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }
}