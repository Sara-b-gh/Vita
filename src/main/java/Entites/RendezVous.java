package Entites;

import java.time.LocalDateTime;

public class RendezVous {
    private int id_rdv;
    private int patient_id;
    private int medecin_id;
    private LocalDateTime date_rdv;
    private String motif;
    private String statut; //"planifie", "confirme"
    private String lieu;
    private String notes;
    private LocalDateTime date_creation;
    private LocalDateTime date_modification;

    public RendezVous() {}

    public RendezVous(int patient_id, int medecin_id, LocalDateTime date_rdv,
                      String motif, String statut, String lieu, String notes) {
        this.patient_id = patient_id;
        this.medecin_id = medecin_id;
        this.date_rdv = date_rdv;
        this.motif = motif;
        this.statut = statut;
        this.lieu = lieu;
        this.notes = notes;
    }

    public int getId_rdv() { return id_rdv; }
    public void setId_rdv(int id_rdv) { this.id_rdv = id_rdv; }

    public int getPatient_id() { return patient_id; }
    public void setPatient_id(int patient_id) { this.patient_id = patient_id; }

    public int getMedecin_id() { return medecin_id; }
    public void setMedecin_id(int medecin_id) { this.medecin_id = medecin_id; }

    public LocalDateTime getDate_rdv() { return date_rdv; }
    public void setDate_rdv(LocalDateTime date_rdv) { this.date_rdv = date_rdv; }

    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public String getLieu() { return lieu; }
    public void setLieu(String lieu) { this.lieu = lieu; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getDate_creation() { return date_creation; }
    public void setDate_creation(LocalDateTime date_creation) { this.date_creation = date_creation; }

    public LocalDateTime getDate_modification() { return date_modification; }
    public void setDate_modification(LocalDateTime date_modification) { this.date_modification = date_modification; }

    @Override
    public String toString() {
        return "RendezVous{" +
                "id_rdv=" + id_rdv +
                ", patient_id=" + patient_id +
                ", medecin_id=" + medecin_id +
                ", date_rdv=" + date_rdv +
                ", motif='" + motif + '\'' +
                ", statut='" + statut + '\'' +
                ", lieu='" + lieu + '\'' +
                ", notes='" + notes + '\'' +
                '}';
    }

    public static class Evenn {
        private int id_Evenn;
        private int idRessource;
        private String titre;
        private LocalDateTime dateEvenement;
        private String description;
        private String lieu;
        public Evenn() {
        }
        public Evenn(int idRessource, String titre, LocalDateTime dateEvenement, String description, String lieu) {
            this.idRessource = idRessource;
            this.titre = titre;
            this.dateEvenement = dateEvenement;
            this.description = description;
            this.lieu = lieu;
        }
        public Evenn(int id_Evenn, int idRessource, String titre, LocalDateTime dateEvenement, String description, String lieu) {
            this.id_Evenn = id_Evenn;
            this.idRessource = idRessource;
            this.titre = titre;
            this.dateEvenement = dateEvenement;
            this.description = description;
            this.lieu = lieu;
        }
        public int getId_Evenn() {
            return id_Evenn;
        }
        public void setId_Evenn(int id_Evenn) {
            this.id_Evenn = id_Evenn;
        }
        public int getIdRessource() {
            return idRessource;
        }
        public void setIdRessource(int idRessource) {
            this.idRessource = idRessource;
        }
        public String getTitre() {
            return titre;
        }
        public void setTitre(String titre) {
            this.titre = titre;
        }
        public LocalDateTime getDateEvenement() {
            return dateEvenement;
        }
        public void setDateEvenement(LocalDateTime dateEvenement) {
            this.dateEvenement = dateEvenement;
        }
        public String getDescription() {
            return description;
        }
        public void setDescription(String description) {
            this.description = description;
        }
        public String getLieu() {
            return lieu;
        }
        public void setLieu(String lieu) {
            this.lieu = lieu;
        }
        @Override
        public String toString() {
            return "Evenn{" +
                    "id_Evenn=" + id_Evenn +
                    ", idRessource=" + idRessource +
                    ", titre='" + titre + '\'' +
                    ", dateEvenement=" + dateEvenement +
                    ", description='" + description + '\'' +
                    ", lieu='" + lieu + '\'' +
                    '}';
        }
    }

    public static class ReservationPersonne {
        private int id;
        private int idEvenement;
        private String nomComplet;
        private String email;
        private String telephone;
        private LocalDateTime dateReservation;
        private String statut; // "EN_ATTENTE", "ACCEPTE", "REFUSE"
        private String commentaire;
        private boolean present;


        public ReservationPersonne() {}

        public ReservationPersonne(int idEvenement, String nomComplet, String email, String telephone, String commentaire) {
            this.idEvenement = idEvenement;
            this.nomComplet = nomComplet;
            this.email = email;
            this.telephone = telephone;
            this.commentaire = commentaire;
            this.dateReservation = LocalDateTime.now();
            this.statut = "EN_ATTENTE";
        }

        // Getters et Setters
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public int getIdEvenement() { return idEvenement; }
        public void setIdEvenement(int idEvenement) { this.idEvenement = idEvenement; }

        public String getNomComplet() { return nomComplet; }
        public void setNomComplet(String nomComplet) { this.nomComplet = nomComplet; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getTelephone() { return telephone; }
        public void setTelephone(String telephone) { this.telephone = telephone; }

        public LocalDateTime getDateReservation() { return dateReservation; }
        public void setDateReservation(LocalDateTime dateReservation) { this.dateReservation = dateReservation; }

        public String getStatut() { return statut; }
        public void setStatut(String statut) { this.statut = statut; }

        public String getCommentaire() { return commentaire; }
        public void setCommentaire(String commentaire) { this.commentaire = commentaire; }
        // Ajoutez dans ReservationPersonne.java

        public boolean isPresent() { return present; }
        public void setPresent(boolean present) { this.present = present; }

        @Override
        public String toString() {
            return nomComplet + " - " + email + " (" + statut + ")";
        }
    }

    public static class Ressource {

        private int idRessource;
        private int idEvenement;
        private String nomRessource;
        private String type;           // NOUVEAU : Humain, Logistique, Matériel
        private int quantiteRequise;
        private int quantiteDisponible;
        private double coutUnitaire;
        private String responsable;
        private String statut;
        private String evenementTitre;

        // Constructeur par défaut
        public Ressource() {
            this.type = "Matériel";  // Valeur par défaut
            this.quantiteRequise = 0;
            this.quantiteDisponible = 0;
            this.coutUnitaire = 0;
            this.statut = "En attente";
            this.responsable = "Non assigné";
        }

        // Constructeur complet
        public Ressource(int idRessource, int idEvenement, String nomRessource, String type,
                         int quantiteRequise, int quantiteDisponible, double coutUnitaire,
                         String responsable, String statut, String evenementTitre) {
            this.idRessource = idRessource;
            this.idEvenement = idEvenement;
            this.nomRessource = nomRessource;
            this.type = type;
            this.quantiteRequise = quantiteRequise;
            this.quantiteDisponible = quantiteDisponible;
            this.coutUnitaire = coutUnitaire;
            this.responsable = responsable;
            this.statut = statut;
            this.evenementTitre = evenementTitre;
        }

        // Constructeur sans ID (pour ajout)
        public Ressource(int idEvenement, String nomRessource, String type,
                         int quantiteRequise, int quantiteDisponible, double coutUnitaire,
                         String responsable, String statut, String evenementTitre) {
            this.idEvenement = idEvenement;
            this.nomRessource = nomRessource;
            this.type = type;
            this.quantiteRequise = quantiteRequise;
            this.quantiteDisponible = quantiteDisponible;
            this.coutUnitaire = coutUnitaire;
            this.responsable = responsable;
            this.statut = statut;
            this.evenementTitre = evenementTitre;
        }
        private Integer delaiReapprovisionnementJours; // délai estimé en jours
        public Integer getDelaiReapprovisionnementJours() { return delaiReapprovisionnementJours; }
        public void setDelaiReapprovisionnementJours(Integer delaiReapprovisionnementJours) { this.delaiReapprovisionnementJours = delaiReapprovisionnementJours; }

        // ==================== GETTERS ET SETTERS ====================

        public int getIdRessource() {
            return idRessource;
        }

        public void setIdRessource(int idRessource) {
            this.idRessource = idRessource;
        }

        public int getIdEvenement() {
            return idEvenement;
        }

        public void setIdEvenement(int idEvenement) {
            this.idEvenement = idEvenement;
        }

        public String getNomRessource() {
            return nomRessource;
        }

        public void setNomRessource(String nomRessource) {
            this.nomRessource = nomRessource;
        }

        // NOUVEAU GETTER ET SETTER POUR TYPE
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public int getQuantiteRequise() {
            return quantiteRequise;
        }

        public void setQuantiteRequise(int quantiteRequise) {
            this.quantiteRequise = quantiteRequise;
        }

        public int getQuantiteDisponible() {
            return quantiteDisponible;
        }

        public void setQuantiteDisponible(int quantiteDisponible) {
            this.quantiteDisponible = quantiteDisponible;
        }

        public double getCoutUnitaire() {
            return coutUnitaire;
        }

        public void setCoutUnitaire(double coutUnitaire) {
            this.coutUnitaire = coutUnitaire;
        }

        public String getResponsable() {
            return responsable;
        }

        public void setResponsable(String responsable) {
            this.responsable = responsable;
        }

        public String getStatut() {
            return statut;
        }

        public void setStatut(String statut) {
            this.statut = statut;
        }

        public String getEvenementTitre() {
            return evenementTitre;
        }

        public void setEvenementTitre(String evenementTitre) {
            this.evenementTitre = evenementTitre;
        }

        // Méthode utilitaire pour calculer le coût total
        public double getCoutTotal() {
            return quantiteRequise * coutUnitaire;
        }

        // Vérifier si la ressource est disponible
        public boolean isDisponible() {
            return quantiteDisponible >= quantiteRequise;
        }

        @Override
        public String toString() {
            return nomRessource + " (" + type + ") - " + (isDisponible() ? "Disponible" : "Rupture");
        }
    }
}