package com.example.vita.test;

import com.example.vita.entities.Medicament;
import com.example.vita.entities.Equipement;
import com.example.vita.services.MedicamentCRUD;
import com.example.vita.services.EquipementCRUD;
import com.example.vita.utils.MyBD;

import java.sql.Date;
import java.sql.SQLException;

public class TestConn {

    public static void main(String[] args) {

        // Connexion
        MyBD bd = MyBD.getInstance();

        // ===================== TEST MEDICAMENT =====================

        Medicament m1 = new Medicament(
                "Paracetamol", "Antidouleur classique",
                "500mg", "comprime", 2.50, 100, "disponible"
        );
        Medicament m2 = new Medicament(
                "Amoxicilline", "Antibiotique",
                "1g", "capsule", 5.75, 50, "disponible"
        );

        MedicamentCRUD mc = new MedicamentCRUD();

        try {
            // Ajouter
            mc.ajouter(m1);
            mc.ajouter(m2);

            // Afficher
            System.out.println("\n--- Liste des médicaments ---");
            System.out.println(mc.afficher());

            // Modifier
            m1.setId_medicament(1);
            m1.setStock(80);
            m1.setStatut("epuise");
            mc.modifier(m1);
            System.out.println("\n--- Après modification ---");
            System.out.println(mc.afficher());

            // Supprimer
            mc.supprimer(2);
            System.out.println("\n--- Après suppression ---");
            System.out.println(mc.afficher());

        } catch (SQLException s) {
            System.out.println(s.getMessage());
        }

        // ===================== TEST EQUIPEMENT =====================

        Equipement e1 = new Equipement(
                "Scanner IRM", "Imagerie médicale", "Imagerie",
                "Siemens", "disponible", "Salle B12",
                Date.valueOf("2023-05-15")
        );
        Equipement e2 = new Equipement(
                "Tensiomètre", "Mesure pression artérielle", "Diagnostic",
                "Omron", "disponible", "Salle A03",
                Date.valueOf("2022-11-20")
        );

        EquipementCRUD ec = new EquipementCRUD();

        try {
            // Ajouter
            ec.ajouter(e1);
            ec.ajouter(e2);

            // Afficher
            System.out.println("\n--- Liste des équipements ---");
            System.out.println(ec.afficher());

            // Modifier
            e1.setId_equipement(1);
            e1.setEtat("en_maintenance");
            e1.setLocalisation("Salle A01");
            ec.modifier(e1);
            System.out.println("\n--- Après modification ---");
            System.out.println(ec.afficher());

            // Supprimer
            ec.supprimer(2);
            System.out.println("\n--- Après suppression ---");
            System.out.println(ec.afficher());

        } catch (SQLException s) {
            System.out.println(s.getMessage());
        }
    }
}