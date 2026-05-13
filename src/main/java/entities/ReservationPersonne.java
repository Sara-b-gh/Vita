package entities;

import java.time.LocalDateTime;

public class ReservationPersonne {
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