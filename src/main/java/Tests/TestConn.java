package tests;

import Entites.RendezVous;
import services.RendezVousCRUD;
import java.sql.SQLException;


public class TestConn {

    public static void main(String[] args) {

        //MyBD.getInstance();

        RendezVousCRUD rdvCRUD = new RendezVousCRUD();

        RendezVous rdv = new RendezVous();

        //rdv.setDate(LocalDateTime.now());
        rdv.setMotif("Consultation");
        rdv.setStatut("planifie");
        rdv.setMedecin_id(2);

        rdvCRUD.ajouter(rdv);

        System.out.println("=== Liste des RendezVous ===");
        System.out.println(rdvCRUD.afficher());

        //rdv.setId(1);
        rdv.setStatut("confirme");

        rdvCRUD.modifier(rdv);

    }
}