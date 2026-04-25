package Tests;

import Entites.RendezVous;
import Services.RendezVousCRUD;
import Utils.MyBD;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class TestConn {

    public static void main(String[] args) {

        MyBD.getInstance();

        RendezVousCRUD rdvCRUD = new RendezVousCRUD();

        try {
            RendezVous rdv = new RendezVous();

            rdv.setDate(LocalDateTime.now());
            rdv.setMotif("Consultation");
            rdv.setStatut("planifie");
            rdv.setMedecin_id(2);

            rdvCRUD.ajouter(rdv);

            System.out.println("=== Liste des RendezVous ===");
            System.out.println(rdvCRUD.afficher());

            rdv.setId(1);
            rdv.setStatut("confirme");

            rdvCRUD.modifier(rdv);

        } catch (SQLException e) {
            System.out.println("Erreur SQL : " + e.getMessage());
        }
    }
}