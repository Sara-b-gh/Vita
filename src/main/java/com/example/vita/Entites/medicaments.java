package Entites;

import java.time.LocalDateTime;

public class medicaments {

        private int id_medicament;
        private String nom;
        private String description;
        private String dosage;
        private String forme; // "comprime", "sirop", "injection", "creme", "capsule"
        private double prix;
        private int stock;
        private String statut; // "disponible", "epuise", "archive"
        private LocalDateTime date_ajout;
        private LocalDateTime date_modification;

        // Constructeur vide
        public medicaments() {}

        // Constructeur complet
        public medicaments(int id_medicament, String nom, String description, String dosage,
                          String forme, double prix, int stock, String statut,
                          LocalDateTime date_ajout, LocalDateTime date_modification) {
            this.id_medicament = id_medicament;
            this.nom = nom;
            this.description = description;
            this.dosage = dosage;
            this.forme = forme;
            this.prix = prix;
            this.stock = stock;
            this.statut = statut;
            this.date_ajout = date_ajout;
            this.date_modification = date_modification;
        }

        // Constructeur sans id (ajout nouveau)
        public medicaments(String nom, String description, String dosage,
                          String forme, double prix, int stock, String statut) {
            this.nom = nom;
            this.description = description;
            this.dosage = dosage;
            this.forme = forme;
            this.prix = prix;
            this.stock = stock;
            this.statut = statut;
        }

        // Getters & Setters
        public int getId_medicament() { return id_medicament; }
        public void setId_medicament(int id_medicament) { this.id_medicament = id_medicament; }

        public String getNom() { return nom; }
        public void setNom(String nom) { this.nom = nom; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getDosage() { return dosage; }
        public void setDosage(String dosage) { this.dosage = dosage; }

        public String getForme() { return forme; }
        public void setForme(String forme) { this.forme = forme; }

        public double getPrix() { return prix; }
        public void setPrix(double prix) { this.prix = prix; }

        public int getStock() { return stock; }
        public void setStock(int stock) { this.stock = stock; }

        public String getStatut() { return statut; }
        public void setStatut(String statut) { this.statut = statut; }

        public LocalDateTime getDate_ajout() { return date_ajout; }
        public void setDate_ajout(LocalDateTime date_ajout) { this.date_ajout = date_ajout; }

        public LocalDateTime getDate_modification() { return date_modification; }
        public void setDate_modification(LocalDateTime date_modification) { this.date_modification = date_modification; }

        @Override
        public String toString() {
            return "medicaments{" +
                    "id=" + id_medicament +
                    ", nom='" + nom + '\'' +
                    ", forme='" + forme + '\'' +
                    ", prix=" + prix +
                    ", stock=" + stock +
                    ", statut='" + statut + '\'' +
                    '}';
        }

}
