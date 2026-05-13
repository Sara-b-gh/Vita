package entities;

import java.time.LocalDateTime;

public class Ressource {

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